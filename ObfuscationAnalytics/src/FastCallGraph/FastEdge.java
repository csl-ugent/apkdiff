package FastCallGraph;

import java.io.Serializable;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.Edge;
import PackageTree.PackageTree;
import Util.MySootClass;

public class FastEdge implements Serializable {

	public String srcMethod;
	public String srcClass;
	public String tgtMethod;
	public String tgtClass;

	public transient PackageTree srcTree = null;

	public FastEdge(String srcMethod, String srcClass, String tgtMethod, String tgtClass){
		this.srcMethod = srcMethod;
		this.srcClass = srcClass;
		this.tgtMethod = tgtMethod;
		this.tgtClass = tgtClass;
	}

//	public Edge getEdge(Package srcForClasses){
//		Kind k = null;
//		try {
//			k = (Kind) Kind.class.getField(kind).get(null);
//		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
//			e1.printStackTrace();
//		}
//		SootClass srcClass = srcForClasses.findClass(this.srcClass).sootClass;
//		SootMethod srcMethod = srcClass.getMethodByName(this.srcMethod);
//		SootClass tgtClass = srcForClasses.findClass(this.tgtClass).sootClass;
//		SootMethod tgtMethod = srcClass.getMethodByName(this.tgtMethod);
//
//		Unit srcUnit = new JInvokeStmt(box.getValue());
//
//		return new Edge(srcMethod, srcUnit, tgtMethod, k);
//	}

	public void setPackageTree(PackageTree tree){
		srcTree = tree;
	}

	public SootMethod src() {
		if(srcTree == null) {
			throw new RuntimeException("SrcTree is not set for fast edges!");
		}
		MySootClass srcClass = srcTree.findClass(this.srcClass);
		if(srcClass == null){
			throw new RuntimeException("In FastEdge.src: not found: " + this.srcClass);
		}
		for(SootMethod m: srcClass.sootClass.getMethods()){
			if(m.getSubSignature().equals(this.srcMethod)) return m;
		}
		return null;
	}

	public SootMethod tgt() {
		if(srcTree == null) {
			throw new RuntimeException("SrcTree is not set for fast edges!");
		}
		MySootClass tgtClass = srcTree.findClass(this.tgtClass);
		if(srcClass == null){
			System.out.println("In FastEdge.src: not found: " + this.tgtClass);
		}
		for(SootMethod m: tgtClass.sootClass.getMethods()){
			if(m.getSubSignature().equals(this.tgtMethod)) return m;
		}
		return null;
	}

	@Override
	public String toString(){
		return "" + srcClass + ":" + srcMethod + " "+ tgtClass + ":" + tgtMethod;
	}
}
