package SootCompare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Config.Config;
import PackageTree.PackageTree;
import Util.AndroidUtil;
import Util.MySootClass;
import Util.SootUtil;
import obfuscation.Obfuscation;
import soot.ArrayType;
import soot.PrimType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;

public class SootCompare {

	public enum MatchType {
		EXACT,
		POTENTIAL
	}

	public static int areTypesExactMatch(Type a, PackageTree srcTreeA, Type b, PackageTree srcTreeB){
		//returns
		//1 exact match
		//0 no match
		//-1 dont know

		//assure a and b are same "array depth", e.g. int[] == int[] but not int[]!=int or int[]!= int[][]
		while(a instanceof ArrayType){
			if(!(b instanceof ArrayType)) return 0;
			a = ((ArrayType) a).getElementType();
			b = ((ArrayType) b).getElementType();
		}
		if(b instanceof ArrayType) return 0;

    	String as = a.toString();
    	String bs = b.toString();

    	if(SootUtil.isBasicType(a) || SootUtil.isBasicType(b)){
    		return Config.bool2int(as.equals(bs));
    	}

    	List<String> ap = AndroidUtil.getclassPathParts(as);
    	String aFullClassName = ap.remove(ap.size() - 1);
    	List<String> classParts = new ArrayList<String>(Arrays.asList(aFullClassName.split("\\$")));
    	ap = Stream.concat(ap.stream(), classParts.stream())
                .collect(Collectors.toList());
    	boolean isAObufscated = false;
    	for(String part: ap){
    		if(Obfuscation.isNameMangled(part)){
    			isAObufscated = true;
    			break;
    		}
    	}
    	if(!isAObufscated){
    		return Config.bool2int(as.equals(bs));
    	}
    	else{
    		MySootClass aClass = srcTreeA.findClass(as);
    		if(aClass != null){
    			if(aClass.matchClass !=  null){
    				return  Config.bool2int(aClass.matchClass.sootClass.getName().equals(bs));
    			}
    		}
    	}
    	return -1;
	}

    public static boolean areTypesPotentialMatch(Type a, PackageTree srcTreeA, Type b, PackageTree srcTreeB){
		//assure a and b are same "array depth", e.g. int[] == int[] but not int[]!=int or int[]!= int[][]
		while(a instanceof ArrayType){
			if(!(b instanceof ArrayType)) return false;
			a = ((ArrayType) a).getElementType();
			b = ((ArrayType) b).getElementType();
		}
		if(b instanceof ArrayType) return false;


    	int exactMatch = areTypesExactMatch(a, srcTreeA, b, srcTreeB);
    	if(exactMatch != -1){
    		//convert to bool
    		return exactMatch == 1;
    	}

    	String as = a.toString();
    	String bs = b.toString();
    	List<String> ap = AndroidUtil.getclassPathParts(as);
    	String aFullClassName = ap.remove(ap.size() - 1);
    	List<String> classParts = new ArrayList<String>(Arrays.asList(aFullClassName.split("\\$")));
    	ap = Stream.concat(ap.stream(), classParts.stream())
                .collect(Collectors.toList());


		//check for unobfuscated parts, e.g. androidx.activity.a is definitely not equal to androidx.biometric.fragment
    	List<String> bp = AndroidUtil.getclassPathParts(bs);
    	String bFullClassName = bp.remove(bp.size() - 1);
    	List<String> bClassParts = new ArrayList<String>(Arrays.asList(bFullClassName.split("\\$")));
    	bp = Stream.concat(bp.stream(), bClassParts.stream())
                .collect(Collectors.toList());
    	Iterator<String> it1 = ap.iterator();
    	Iterator<String> it2 = bp.iterator();
    	while (it1.hasNext() && it2.hasNext()) {
    	    String partA = it1.next();
    	    String partB = it2.next();
    	    if(!Obfuscation.isNameMangled(partA) && !Obfuscation.isNameMangled(partB)){
    	    	if(!partA.equals(partB)) return false;
    	    }
    	    else {
    	    	break;
    	    }
    	}

    	return true;
    }


    public static boolean isMethodMatch(SootMethod m1, PackageTree srcTree1, SootMethod m2, PackageTree srcTree2, boolean checkName, MatchType matchtype){
        if(m1.getDeclaringClass().resolvingLevel() == SootClass.HIERARCHY || m2.getDeclaringClass().resolvingLevel() == SootClass.HIERARCHY) return false;

        if(m1.isStatic() != m2.isStatic()) return false;
        if(m1.getParameterCount() != m2.getParameterCount()) return false;

        if(Config.USE_FINAL_MODIFIER){
        	if(m1.isFinal() != m2.isFinal()) return false;
        }

        if(Config.USE_ACCESS_MODIFIERS){
        	if(m1.isPublic() != m2.isPublic()) return false;
        	if(m1.isProtected() != m2.isProtected()) return false;
        	if(m1.isPrivate() != m2.isPrivate()) return false;
        }

        if(checkName){
        	String m1Name = m1.getName();
        	String m2Name = m2.getName();
        	if(Obfuscation.isNameMangled(m1Name) || Obfuscation.isNameMangled(m2Name)){
        		return false;
        	}
        	if(!m1Name.equals(m2Name)) return false;
        }

        //annotations
//        List<String> m1Annotations = getAnnotations(m1);
//        List<String> m2Annotations = getAnnotations(m2);
//        if(m1Annotations.size() != m2Annotations.size()) return false;
//        Collections.sort(m1Annotations);
//        Collections.sort(m2Annotations);
//        if(!m1Annotations.equals(m2Annotations)) return false;
//        List<String> m1ParameterAnnotations = getParameterAnnotations(m1);
//        List<String> m2ParameterAnnotations = getParameterAnnotations(m2);


        //compare parameter types of both functions, check if all Type.toString() of one contains all whole other list
        //List<String> params1 = m1.getParameterTypes().stream().map(Type::toString).collect(Collectors.toList());
        //augment params with their annotations
//        for(int i = 0; i < m1ParameterAnnotations.size(); i++){
//        	params1.set(i, m1ParameterAnnotations.get(i) + params1.get(i));
//        }
        //Collections.sort(params1);
        //List<String> params2 = m2.getParameterTypes().stream().map(Type::toString).collect(Collectors.toList());
//        for(int i = 0; i < m2ParameterAnnotations.size(); i++){
//        	params2.set(i, m2ParameterAnnotations.get(i) + params2.get(i));
//        }
//        Collections.sort(params2);
//        if(!params1.equals(params2)) return false;


        List<Type> m1Types = m1.getParameterTypes();
        List<Type> m2Types = m2.getParameterTypes();
        for(int i = 0; i < m1Types.size(); i++){
        	Type a = m1Types.get(i);
        	Type b = m2Types.get(i);
            if(matchtype == MatchType.POTENTIAL){
            	if(!areTypesPotentialMatch(a, srcTree1, b, srcTree2)) return false;
            }
            else if (matchtype == MatchType.EXACT){
            	if(areTypesExactMatch(a, srcTree1, b, srcTree2) != 1) return false;
            }
            else{
            	throw new RuntimeException("MatchType does not exist!");
            }
        }

        //return type
        if(matchtype == MatchType.POTENTIAL){
        	if(!areTypesPotentialMatch(m1.getReturnType(), srcTree1, m2.getReturnType(), srcTree2)) return false;
        }
        else if (matchtype == MatchType.EXACT){
        	if(areTypesExactMatch(m1.getReturnType(), srcTree1, m2.getReturnType(), srcTree2) != 1) return false;
        }
        else{
        	throw new RuntimeException("MatchType does not exist!");
        }

        return true;
    }


    public static boolean isFieldMatch(SootField f1, PackageTree srcTree1, SootField f2, PackageTree srcTree2, boolean checkName, MatchType matchtype){
        if(f1.getDeclaringClass().resolvingLevel() == SootClass.HIERARCHY || f2.getDeclaringClass().resolvingLevel() == SootClass.HIERARCHY) return false;

        if(f1.isStatic() != f2.isStatic()) return false;

        if(Config.USE_FINAL_MODIFIER){
        	if(f1.isFinal() != f2.isFinal()) return false;
        }

        if(Config.USE_ACCESS_MODIFIERS){
        	if(f1.isPublic() != f2.isPublic()) return false;
        	if(f1.isProtected() != f2.isProtected()) return false;
        	if(f1.isPrivate() != f2.isPrivate()) return false;
        }

        if(checkName){
        	String f1Name = f1.getName();
        	String f2Name = f2.getName();
           	if(Obfuscation.isNameMangled(f1Name) || Obfuscation.isNameMangled(f2Name)){
        		return false;
        	}
        	if(!f1Name.equals(f2Name)) return false;
        }

        if(matchtype == MatchType.POTENTIAL){
        	if(!areTypesPotentialMatch(f1.getType(), srcTree1, f2.getType(), srcTree2)) return false;
        }
        else if (matchtype == MatchType.EXACT){
        	if(areTypesExactMatch(f1.getType(), srcTree1, f2.getType(), srcTree2) != 1) return false;
        }
        else{
        	throw new RuntimeException("MatchType does not exist!");
        }

//        List<String> f1Annotations = getAnnotations(f1);
//        List<String> f2Annotations = getAnnotations(f2);
//        Collections.sort(f1Annotations);
//        Collections.sort(f2Annotations);
//        if(!f1Annotations.equals(f2Annotations)) return false;

        return true;
    }


    public static boolean areAllMethodsPotentialMatch(SootClass s1, PackageTree srcTree1, SootClass s2, PackageTree srcTree2, boolean checkNames){
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
                if(isMethodMatch(m1, srcTree1, m2, srcTree2, checkNames, MatchType.POTENTIAL)){
                    it1.remove();
                    it2.remove();
                    break;
                }
            }
        }
        return l1.size() == 0 && l2.size() == 0;
    }


    public static boolean areAllFieldsPotentialMatch(SootClass s1, PackageTree srcTree1, SootClass s2, PackageTree srcTree2, boolean checkNames){
        if(s1.getFieldCount() != s2.getFieldCount()) return false;

        List<SootField> l1 = new ArrayList<>(s1.getFields());
        List<SootField> l2 = new ArrayList<>(s2.getFields());

        if(Config.ONLY_PUBLIC_METHODS_AND_FIELDS){
        	for (Iterator<SootField> it1 = l1.iterator(); it1.hasNext();) {
        		SootField f1 = it1.next();
        		if(!f1.isPublic()){
        			it1.remove();
        		}
        	}
        	for (Iterator<SootField> it2 = l2.iterator(); it2.hasNext();) {
        		SootField f2 = it2.next();
        		if(!f2.isPublic()){
        			it2.remove();
        		}
        	}
        }

        if(l1.size() != l2.size()) return false;

        for (Iterator<SootField> it1 = l1.iterator(); it1.hasNext();) {
            SootField f1 = it1.next();
            for (Iterator<SootField> it2 = l2.iterator(); it2.hasNext();) {
                SootField f2 = it2.next();
                if(isFieldMatch(f1, srcTree1, f2, srcTree2, checkNames, MatchType.POTENTIAL)){
                    it1.remove();
                    it2.remove();
                    break;
                }
            }
        }

        return l1.size() == 0 && l2.size() == 0;
    }


    public static boolean isSootClassSignaturePotentialMatch(SootClass s1, PackageTree srcTree1, SootClass s2, PackageTree srcTree2, boolean checkNames){
        //needed to avoid errors when certain classes are not loaded
    	if(s1.resolvingLevel() != s2.resolvingLevel()) return false;
    	if(s1.resolvingLevel() == SootClass.HIERARCHY && s2.resolvingLevel() == SootClass.HIERARCHY) return true;


    	if(s1.isPhantom() != s2.isPhantom()) return false;


        if(s1.isInnerClass() != s2.isInnerClass()) return false;
        if(s1.isInterface() != s2.isInterface()) return false;
        if(s1.isStatic() != s2.isStatic()) return false;
        if(s1.isAbstract() != s2.isAbstract()) return false;
        if(s1.getInterfaceCount() != s2.getInterfaceCount()) return false;
        if(s1.isEnum() != s2.isEnum()) return false;
        if(s1.isSynchronized() != s2.isSynchronized()) return false;

        if(Config.USE_FINAL_MODIFIER){
        	if(s1.isFinal() != s2.isFinal()) return false;
        }

        if(Config.USE_ACCESS_MODIFIERS){
        	if(s1.isPublic() != s2.isPublic()) return false;
        	if(s1.isPrivate() != s2.isPrivate()) return false;
        	if(s1.isProtected() != s2.isProtected()) return false;
        }

        //same amount of parent classes (same depth in inner class tree)
        if(Config.USE_INNER_CLASS_LINK_INFO){
            SootClass s1h = s1;
            SootClass s2h = s2;
            while(s1h.hasOuterClass()){
            	if(!s2h.hasOuterClass()) return false;
            	s1h = s1h.getOuterClass();
            	s2h = s2h.getOuterClass();
            }
        }

        if(checkNames) {
        	String s1ClassName = AndroidUtil.getClassName(s1.getName());
        	String s2ClassName = AndroidUtil.getClassName(s2.getName());
        	if(Obfuscation.isNameMangled(s1ClassName) || Obfuscation.isNameMangled(s2ClassName)){
        		return false;
        	}
        	if(!s1ClassName.equals(s2ClassName)) return false;
        }


        //annotations
//        List<String> s1Annotations = getAnnotations(s1);
//        List<String> s2Annotations = getAnnotations(s2);
//        Collections.sort(s1Annotations);
//        Collections.sort(s2Annotations);
//        if(!s1Annotations.equals(s2Annotations)) return false;


        //superclass
        if(s1.hasSuperclass() != s2.hasSuperclass()) return false;
        if(s1.hasSuperclass()){
        	if(!isSootClassSignaturePotentialMatch(s1.getSuperclass(), srcTree1, s2.getSuperclass(), srcTree2, checkNames)) return false;
        }

        //interfaces
        Iterator<SootClass> interfaces1 = s1.getInterfaces().iterator();
        Iterator<SootClass> interfaces2 = s2.getInterfaces().iterator();
        while(interfaces1.hasNext() && interfaces2.hasNext()) {
        	SootClass interface1 = interfaces1.next();
        	SootClass interface2 = interfaces2.next();
        	if(!isSootClassSignaturePotentialMatch(interface1, srcTree1, interface2, srcTree2, checkNames)) return false;
        }


        return true;

    }


    public static boolean isMySootClassPotentialMatch(MySootClass msc1, PackageTree srcTree1, MySootClass msc2, PackageTree srcTree2, boolean checkNames){
        //apk class cannot have more inner classes than src class since it can only have classes removed during shrinking
        //not aplicable when comparing apks
        //if(msc1.innerClasses.size() > msc2.innerClasses.size()) return false;

        //same amount of inner classes
    	if(Config.USE_INNER_CLASS_LINK_INFO){
    		if(msc1.innerClasses.size() != msc2.innerClasses.size()) return false;
    	}

        SootClass s1 = msc1.sootClass;
        SootClass s2 = msc2.sootClass;

        if(!isSootClassSignaturePotentialMatch(s1, srcTree1, s2, srcTree2, checkNames)) return false;

        if(!areAllMethodsPotentialMatch(s1, srcTree1, s2, srcTree2, checkNames)) return false;

        if(!areAllFieldsPotentialMatch(s1, srcTree1, s2, srcTree2, checkNames)) return false;

        return true;
    }


    public static void removeExactUniqueFieldMatches(List<SootField> l1, PackageTree srcTree1, List<SootField> l2, PackageTree srcTree2){
    	//this function iterates over both lists
    	//if an exact & UNIQUE match is found, the fields are removed

    	//keep track of excess matches in case there are groups that can be eliminated together
    	// e.g. you have 2x private float in both lists, these 4 must be eliminated together as they are not unique
    	Map<SootField, List<SootField>> matches = new HashMap<SootField, List<SootField>>();

    	for (Iterator<SootField> it = l1.iterator(); it.hasNext();) {
    		SootField f1 = it.next();
    		List<SootField> f1ExactMatches = new ArrayList<>();
    		for(SootField f2: l2) {
    			if(isFieldMatch(f1, srcTree1, f2, srcTree2, false, MatchType.EXACT)){
    				f1ExactMatches.add(f2);
    			}
    		}
    		if(f1ExactMatches.size() == 0){
    			continue;
    		}
    		if(f1ExactMatches.size() == 1){
    			it.remove();
    			l2.remove(f1ExactMatches.get(0));
    		}
    		else{
    			matches.put(f1, f1ExactMatches);
    		}
    	}

    	//eliminate groups
    	 while(matches.size() > 0) {
              Map.Entry<SootField, List<SootField>> entry1 = matches.entrySet().iterator().next();
              SootField f1 = entry1.getKey();
              List<SootField> m1 = entry1.getValue();
              List<Map.Entry<SootField, List<SootField>>> matchingEntries = new ArrayList<>();
              matchingEntries.add(entry1);
              Iterator<Map.Entry<SootField, List<SootField>>> it2 = matches.entrySet().iterator();
              while(it2.hasNext()){
            	  Map.Entry<SootField, List<SootField>> entry2 = it2.next();
                  SootField f2 = entry2.getKey();
                  List<SootField> m2 = entry2.getValue();
                  if(f1 != f2 && m1.containsAll(m2) && m2.containsAll(m1)){
                	  matchingEntries.add(entry2);
                  }

              }
              if(m1.size() == matchingEntries.size()){
            	  for(SootField f: m1){
            		  l2.remove(f);
            	  }
            	  for(Map.Entry<SootField, List<SootField>> e: matchingEntries){
            		  l1.remove(e.getKey());
            	  }
              }
        	  for(Map.Entry<SootField, List<SootField>> e: matchingEntries){
        		  matches.remove(e.getKey());
        	  }
         }


    }

    public static void removeExactUniqueMethodMatches(List<SootMethod> l1, PackageTree srcTree1, List<SootMethod> l2, PackageTree srcTree2){
    	//this function iterates over both lists
    	//if an exact & UNIQUE match is found, the methods are removed


    	//keep track of excess matches in case there are groups that can be eliminated together
		Map<SootMethod, List<SootMethod>> matches = new HashMap<SootMethod, List<SootMethod>>();

		for (Iterator<SootMethod> it = l1.iterator(); it.hasNext();) {
    		SootMethod m1 = it.next();
    		List<SootMethod> m1ExactMatches = new ArrayList<>();
    		for(SootMethod m2: l2) {
    			if(isMethodMatch(m1, srcTree1, m2, srcTree2, false, MatchType.EXACT)){
    				m1ExactMatches.add(m2);
    			}
    		}
    		if(m1ExactMatches.size() == 0){
    			continue;
    		}
    		if(m1ExactMatches.size() == 1){
    			it.remove();
    			l2.remove(m1ExactMatches.get(0));
    		}
    		else{
    			matches.put(m1, m1ExactMatches);
    		}
    	}



    	//eliminate groups
    	while(matches.size() > 0) {
	        Map.Entry<SootMethod, List<SootMethod>> entry1 = matches.entrySet().iterator().next();
	        SootMethod f1 = entry1.getKey();
	        List<SootMethod> m1 = entry1.getValue();
	        List<Map.Entry<SootMethod, List<SootMethod>>> matchingEntries = new ArrayList<>();
	        matchingEntries.add(entry1);
	        Iterator<Map.Entry<SootMethod, List<SootMethod>>> it2 = matches.entrySet().iterator();
	        while(it2.hasNext()){
	        	Map.Entry<SootMethod, List<SootMethod>> entry2 = it2.next();
	           	SootMethod f2 = entry2.getKey();
	           	List<SootMethod> m2 = entry2.getValue();
	           	if(f1 != f2 && m1.containsAll(m2) && m2.containsAll(m1)){
	           		matchingEntries.add(entry2);
	           	}

	        }
	        if(m1.size() == matchingEntries.size()){
	        	for(SootMethod f: m1){
	        		l2.remove(f);
	        	}
	        	for(Map.Entry<SootMethod, List<SootMethod>> e: matchingEntries){
	        		l1.remove(e.getKey());
	        	}
	        }
	       	for(Map.Entry<SootMethod, List<SootMethod>> e: matchingEntries){
        		matches.remove(e.getKey());
        	}
    	}




    }
}
