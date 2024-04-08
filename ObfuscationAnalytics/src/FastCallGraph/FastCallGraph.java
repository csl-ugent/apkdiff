package FastCallGraph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import PackageTree.PackageTree;
import Util.MySootClass;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import Config.Config;

public class FastCallGraph implements Iterable<FastEdge>, Serializable {

    List<FastEdge> edges = new ArrayList<>();

    private transient Map<SootClass, Map<SootMethod, List<FastEdge>>> outMap;
    private transient Map<SootClass, Map<SootMethod, List<FastEdge>>> inMap;

    public FastCallGraph(CallGraph cg, PackageTree tree) {
        for (Iterator<Edge> it = cg.iterator(); it.hasNext();) {
            Edge edge = it.next();
            FastEdge e = FastEdgeFactory.createFastEdge(edge, tree);
            if(e != null) edges.add(e);
        }
    }

    public void setPackageTree(PackageTree tree){
        for(FastEdge e: edges) e.setPackageTree(tree);
    }

     public boolean addEdge(FastEdge e) {
         return edges.add(e);
     }

     public List<FastEdge> getEdges(){
         return edges;
     }

     @Override
     public Iterator<FastEdge> iterator() {
       return edges.iterator();
     }

     public void createOutMap(){
    	 outMap = new HashMap<>();
    	 //first sort edges per src method
    	 Map<SootMethod, List<FastEdge>> mEdges = new HashMap<>();
    	 for(FastEdge e: edges){
    		 SootMethod srcMethod = e.src();
    		 if(Config.ONLY_PUBLIC_METHODS_AND_FIELDS && !srcMethod.isPublic()){
    			 continue;
    		 }
    		 if(!mEdges.containsKey(srcMethod)){
    			 List<FastEdge> l = new ArrayList<>();
    			 mEdges.put(srcMethod, l);
    		 }
    		 mEdges.get(srcMethod).add(e);
    	 }
    	 //second sort src methods per class
    	 for (Iterator<Entry<SootMethod, List<FastEdge>>> mIt1 = mEdges.entrySet().iterator();mIt1.hasNext();) {
 			Entry<SootMethod, List<FastEdge>> kv1 = mIt1.next();
 			SootMethod m = kv1.getKey();
 			List<FastEdge> edges1 = kv1.getValue();
 			SootClass clazz = m.getDeclaringClass();
 			if(!outMap.containsKey(clazz)){
 				Map<SootMethod, List<FastEdge>> methodEdges = new HashMap<>();
 				outMap.put(clazz, methodEdges);
 			}
 			outMap.get(clazz).put(m, edges1);
    	 }
     }

     public void createInMap(){
    	 inMap = new HashMap<>();
    	 //first sort edges per tgt method
    	 Map<SootMethod, List<FastEdge>> mEdges = new HashMap<>();
    	 for(FastEdge e: edges){
    		 SootMethod tgtMethod = e.tgt();
    		 if(Config.ONLY_PUBLIC_METHODS_AND_FIELDS && !tgtMethod.isPublic()){
    			 continue;
    		 }
    		 if(!mEdges.containsKey(tgtMethod)){
    			 List<FastEdge> l = new ArrayList<>();
    			 mEdges.put(tgtMethod, l);
    		 }
    		 mEdges.get(tgtMethod).add(e);
    	 }
    	 //second sort tgt methods per class
    	 for (Iterator<Entry<SootMethod, List<FastEdge>>> mIt1 = mEdges.entrySet().iterator();mIt1.hasNext();) {
 			Entry<SootMethod, List<FastEdge>> kv1 = mIt1.next();
 			SootMethod m = kv1.getKey();
 			List<FastEdge> edges1 = kv1.getValue();
 			SootClass clazz = m.getDeclaringClass();
 			if(!inMap.containsKey(clazz)){
 				Map<SootMethod, List<FastEdge>> methodEdges = new HashMap<>();
 				inMap.put(clazz, methodEdges);
 			}
 			inMap.get(clazz).put(m, edges1);
    	 }
     }

     public Map<SootClass, Map<SootMethod, List<FastEdge>>> getEdgesOutOfClasses() {
    	 if(outMap == null){
    		 createOutMap();
    	 }
    	 return outMap;
     }

     public Map<SootClass, Map<SootMethod, List<FastEdge>>> getEdgesInClasses() {
    	 if(inMap == null){
    		 createInMap();
    	 }
    	 return inMap;
     }

}
