package FastCallGraph;

import java.util.stream.Collectors;

import PackageTree.PackageTree;
import Util.MySootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.toolkits.callgraph.Edge;

public class FastEdgeFactory {


	public static FastEdge createFastEdge(Edge e, PackageTree tree){
		String srcClass = e.src().getDeclaringClass().getName();
		MySootClass a = tree.findClass(srcClass);
		if(a == null || e.src().getDeclaringClass().isPhantom()) return null;
		if(!a.sootClass.getMethods().stream().map(SootMethod::getSubSignature)
				.collect(Collectors.toList()).contains(e.src().getSubSignature())) return null;
		String srcMethod = e.src().getSubSignature();
		String tgtClass = e.tgt().getDeclaringClass().getName();
		MySootClass b = tree.findClass(tgtClass);
		if(b == null || e.tgt().getDeclaringClass().isPhantom()) return null;
		if(!b.sootClass.getMethods().stream().map(SootMethod::getSubSignature)
				.collect(Collectors.toList()).contains(e.tgt().getSubSignature())) return null;
		String tgtMethod = e.tgt().getSubSignature();
		return new FastEdge(srcMethod, srcClass, tgtMethod, tgtClass);
	}
}
