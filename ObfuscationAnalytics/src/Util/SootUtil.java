package Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Config.Config;
import obfuscation.Obfuscation;
import soot.ArrayType;
import soot.PrimType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.util.Chain;

public class SootUtil {
    public static boolean isNumeric(final String str) {

        // null or empty
        if (str == null || str.length() == 0) {
            return false;
        }

        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;

    }

	public static boolean isSootClassMangled(SootClass s){
		String[] parts = s.getJavaStyleName().split("\\$");
		String last = parts[parts.length-1];
		if(!isNumeric(last)){
			//anonymous inner classes
			if(Obfuscation.isNameMangled(last)) return true;
		}

		for(SootField f: s.getFields()){
			if(Obfuscation.isNameMangled(f.getName())) return true;
		}

		for(SootMethod m: s.getMethods()){
			if(Obfuscation.isNameMangled(m.getName())) return true;
		}

		return false;
	}

	public static boolean isBasicType(Type t){
		while(t instanceof ArrayType){
			t = ((ArrayType) t).getElementType();
		}

    	if(t instanceof VoidType || t instanceof PrimType ){
    		return true;
    	}
    	String ts = t.toString();
    	if(ts.startsWith("java.") || ts.startsWith("android.") || ts.startsWith("javax.")){
    		return true;
    	}
    	return false;
	}

	public static boolean isFullClassPathUnobfuscated(String fullClassPath){
		List<String> ap = AndroidUtil.getclassPathParts(fullClassPath);
    	String aFullClassName = ap.remove(ap.size() - 1);
    	List<String> classParts = new ArrayList<String>(Arrays.asList(aFullClassName.split("\\$")));
    	ap = Stream.concat(ap.stream(), classParts.stream())
                .collect(Collectors.toList());
    	for(String part: ap){
    		if(Obfuscation.isNameMangled(part)){
    			return false;
    		}
    	}
    	return true;
	}

	public static List chain2list(Chain chain){
		List output = new ArrayList();
		chain.forEach((obj) -> output.add(obj));
		return output;
	}

	public static String type2hash(Type t){
    	String ts = t.toString();
    	if(isBasicType(t)){
    		return ts;
    	}
    	else if(isFullClassPathUnobfuscated(ts)){
    		return ts;
    	}
    	else {
    		return "x";
    	}
	}

	public static String sootClass2hash(SootClass clazz){
		String split = "_";
		String subsplit = ",";
		String subsubsplit = "-";
		String hash = "";

		//soot util, must match before continuing
		if(clazz.isPhantom()) return "";
		if(clazz.resolvingLevel() == SootClass.HIERARCHY) return "";

		//general class/interface/enum properties
		hash += clazz.isAbstract();
		hash += clazz.isPublic();
		hash += clazz.isPrivate();
		hash += clazz.isProtected();
		hash += clazz.isStatic();
		hash += clazz.isInterface();
		hash += clazz.isEnum();
		hash += clazz.isFinal();
		hash += clazz.isInnerClass();
		hash += clazz.getModifiers();
		hash += clazz.isSynchronized();

		hash += split;

        //same amount of parent classes (same depth in inner class tree)
        if(Config.USE_INNER_CLASS_LINK_INFO){
        	int n = 0;
            SootClass Sclazz = clazz;
            while(Sclazz.hasOuterClass()){
            	Sclazz = Sclazz.getOuterClass();
            	n++;
            }
            hash += n;
        }

        hash += split;

        //superclass

        //interfaces
        hash += clazz.getInterfaceCount();

        hash += split;

        //fields
        hash += clazz.getFieldCount();
        hash += subsplit;
        List<String> fieldshashes = new ArrayList<>();
        for(SootField f: clazz.getFields()){
        	if(f.isPhantom()) continue;
        	String fieldhash = "";
        	fieldhash += f.isPublic();
        	fieldhash += f.isProtected();
        	fieldhash += f.isPrivate();
        	fieldhash += f.isStatic();
        	fieldhash += f.isFinal();
        	fieldhash += type2hash(f.getType());
        	fieldshashes.add(fieldhash);

        }
        List<String> fieldshashesSorted = fieldshashes.stream().sorted().collect(Collectors.toList());
        hash += String.join(subsplit, fieldshashesSorted);

        hash += split;

        //methods
        hash += clazz.getMethodCount();
        hash += subsplit;
        List<String> methodhashes = new ArrayList<>();
        for(SootMethod m: clazz.getMethods()){
        	if(m.isPhantom()) continue;
        	String methodhash = "";
        	methodhash += m.isPublic();
        	methodhash += m.isProtected();
        	methodhash += m.isPrivate();
        	methodhash += m.isStatic();
        	methodhash += m.isFinal();
        	methodhash += m.isAbstract();
        	methodhash += m.isConstructor();
        	methodhash += m.isNative();
        	methodhash += m.isSynchronized();
        	methodhash += m.isStaticInitializer();
        	methodhash += type2hash(m.getReturnType());
        	methodhash += m.getParameterCount();
        	List<String> params = new ArrayList<>();
        	for(Type p: m.getParameterTypes()){
        		params.add(type2hash(p));
        	}
        	methodhash += params.stream().sorted().collect(Collectors.toList());
        	methodhashes.add(methodhash);
        }
        List<String> methodhashesSorted = methodhashes.stream().sorted().collect(Collectors.toList());
        hash += String.join(subsplit, methodhashesSorted);
		return hash;
	}

	public static Map<String, List<MySootClass>> getClassHashMap(List<MySootClass> classes){
		Map<String, List<MySootClass>> output = new HashMap<>();

		for(MySootClass clazz: classes){
			String h = sootClass2hash(clazz.sootClass);
			if(output.containsKey(h)){
				output.get(h).add(clazz);
			}
			else{
				List<MySootClass> a = new ArrayList<MySootClass>();
				a.add(clazz);
				output.put(h, a);
			}
		}

		return output;
	}
}
