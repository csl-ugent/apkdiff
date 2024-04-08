package SootCompare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import Config.Config;
import PackageTree.PackageTree;
import SootCompare.SootCompare.MatchType;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.IdentityStmt;

public class ClassVerify {


	public static boolean isMethodByteCodeMatch(SootMethod m1, SootMethod m2) {
		if(!m1.hasActiveBody() && !m2.hasActiveBody()) return true;
		if(!m1.hasActiveBody() ^ !m2.hasActiveBody()) return false;
		UnitPatchingChain c1 = m1.retrieveActiveBody().getUnits();
		UnitPatchingChain c2 = m2.retrieveActiveBody().getUnits();
		if(c1.size() != c2.size()) return false;

		Iterator<Unit> i1 = c1.iterator();
		Iterator<Unit> i2 = c2.iterator();
		while(i1.hasNext() && i2.hasNext()) {
			Unit u1 = i1.next();
			Unit u2 = i2.next();
			//compare statement type (eg ReturnVoid or Invoke or Identity)
			Class<? extends Unit> cl1 = u1.getClass();
			Class<? extends Unit> cl2 = u2.getClass();
			if(!cl1.getName().equals(cl2.getName())) {
				return false;
			}
//			if (u1 instanceof InvokeStmt) {
//				Value a = ((InvokeStmt) u1).getInvokeExprBox().getValue();
//			}
//			else if (u1 instanceof ReturnVoidStmt) {
//				//nothing
//			}
//			else if (u1 instanceof IdentityStmt) {
//
//			}
		}


		return true;
	}


    public static boolean isClassByteCodeMatch(SootClass s1, PackageTree srcTree1, SootClass s2, PackageTree srcTree2, boolean checkNames){
        //needed to avoid errors when certain classes are not loaded
    	if(s1.resolvingLevel() != s2.resolvingLevel()) return false;
    	if(s1.resolvingLevel() == SootClass.HIERARCHY && s2.resolvingLevel() == SootClass.HIERARCHY) return true;


    	if(s1.isPhantom() != s2.isPhantom()) return false;


        List<SootMethod> l1 = new ArrayList<>(s1.getMethods());
        List<SootMethod> l2 = new ArrayList<>(s2.getMethods());

        if(Config.ONLY_PUBLIC_METHODS_AND_FIELDS) {
            for (Iterator<SootMethod> it1 = l1.iterator(); it1.hasNext();) {
                SootMethod m1 = it1.next();
                if(!m1.isPublic()){
                	it1.remove();
                }
            }
            for (Iterator<SootMethod> it2 = l2.iterator(); it2.hasNext();) {
                SootMethod m2 = it2.next();
                if(!m2.isPublic()){
                    it2.remove();
                }
            }
        }

        if(l1.size() != l2.size()) return false;

        for (Iterator<SootMethod> it1 = l1.iterator(); it1.hasNext();) {
            SootMethod m1 = it1.next();
            for (Iterator<SootMethod> it2 = l2.iterator(); it2.hasNext();) {
                SootMethod m2 = it2.next();
                if(SootCompare.isMethodMatch(m1, srcTree1, m2, srcTree2, checkNames, MatchType.POTENTIAL)){
                    it1.remove();
                    it2.remove();
                    boolean byteCodeMatch = isMethodByteCodeMatch(m1, m2);
                    if(!byteCodeMatch){
                    	return false;
                    }
                    break;
                }
            }
        }
        return l1.size() == 0 && l2.size() == 0;
    }


}
