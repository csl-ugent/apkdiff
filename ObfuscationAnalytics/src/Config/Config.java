package Config;

public class Config {

	public static boolean DEBUG = false;

	public static long startTime = System.currentTimeMillis();

	public static boolean USE_INNER_CLASS_LINK_INFO = false;

	public static boolean USE_CALLGRAPH = true;

	public static boolean USE_NAMES = true;

	public static boolean USE_SUPERCLASSES_ON_NEW_MATCH = true;

	public static boolean USE_INTERFACES_ON_NEW_MATCH = true;

	public static boolean USE_FIELDS_ON_NEW_MATCH = true;

	public static boolean USE_METHODS_ON_NEW_MATCH = true;

	public static boolean USE_ACCESS_MODIFIERS = true;

	public static boolean USE_FINAL_MODIFIER = false;

	public static boolean ONLY_PUBLIC_METHODS_AND_FIELDS = false;

	public static boolean USE_INCOMING_CALLGRAPH_EDGES = false;

	public static int CALLGRAPH_TIMEOUT_IN_MINUTES = 60;



	public static int bool2int(boolean b){
		return Boolean.compare(b, false);
	}

	public static String getIdentifier(){
		//this identifier is used to differentiate between different callgraphs of the same apk
		//because when for example innner classes are used, the call graph will be different after generation
		//because FastCallGraph & FastEdge uses apk.find(class) and this behaves differently when inner classes are used
		return "" + bool2int(USE_INNER_CLASS_LINK_INFO);
	}

}
