package Util;


import java.io.File;
import java.util.Collections;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParser;


public class MainOptions extends OptionsBase {

	  @Option(
		      name = "Debug",
		      abbrev = 'd',
		      help = "Print out debugging information",
		      category = "startup",
		      defaultValue = "false"
		  )
	  public String debug;

	  @Option(
		      name = "Use Names",
		      abbrev = 'n',
		      help = "Use Identifier information if not obviously renamed",
		      category = "startup",
		      defaultValue = "true"
		  )
	  public String useNames;

	  @Option(
		      name = "Use Call Graph",
		      abbrev = 'g',
		      help = "Use call graph edges as information",
		      category = "startup",
		      defaultValue = "true"
		  )
	  public String useCallGraph;

	  @Option(
		      name = "useAccessModifiers",
		      abbrev = 'y',
		      help = "Use access modifiers: public/private/protected on classes/methods/fields",
		      category = "startup",
		      defaultValue = "true"
		  )
	  public String useAccessModifiers;

	  @Option(
		      name = "useFinalModifier",
		      abbrev = 'f',
		      help = "Use the final modifier",
		      category = "startup",
		      defaultValue = "false"
		  )
	  public String useFinalModifier;

	  @Option(
		      name = "onlyPublic",
		      abbrev = 'l',
		      help = "Only use public fields and methods",
		      category = "startup",
		      defaultValue = "false"
		  )
	  public String onlyPublic;

	  @Option(
		      name = "incomingCallGraphEdges",
		      abbrev = 'i',
		      help = "Not only use outgoing call graph edges, but also the incoming ones",
		      category = "startup",
		      defaultValue = "false"
		  )
	  public String incomingCallGraphEdges;

	  @Option(
		      name = "Call Graph Timeout",
		      abbrev = 't',
		      help = "Maximum time spent on calculating call graph in minutes.",
		      category = "startup",
		      defaultValue = "60"
		  )
	  public int callGraphTimeout = 60;

	  @Option(
		      name = "androidPlatformsPath",
		      abbrev = 'p',
		      help = "Full path to the android sdk platform dir, if absent take windows default: /$user/AppData/Local/android/Sdk/platforms.",
		      category = "startup",
		      defaultValue = ""
		  )
	public String androidPlatformsPath;

	  @Option(
		      name = "outputDir",
		      abbrev = 'o',
		      help = "Output directory. Creates it if non-existant. Default is /output.",
		      category = "startup",
		      defaultValue = "output"
		  )
	public String outputDir;

	  @Option(
		      name = "apkPath",
		      abbrev = 'a',
		      help = "Path to the .apk file.",
		      category = "startup",
		      defaultValue = ""
		  )
	  public String apkPath;

	  @Option(
		      name = "apkMatchPath",
		      abbrev = 'm',
		      help = "Path to the .apk file to match with.",
		      category = "startup",
		      defaultValue = ""
		  )
	  public String apkMatchPath;

	  @Option(
			  name = "srcCLassPath",
			  abbrev = 's',
			  help = "Path to the directory where source (.class) files can be found. E.g. the dir where the androidx dir is.",
			  category = "startup",
			  defaultValue = ""
			  )
	  public String srcCLassPath;


	  public static void printUsage(OptionsParser parser) {
		    System.out.println(parser.describeOptions(Collections.<String, String>emptyMap(),
		                                              OptionsParser.HelpVerbosity.LONG));
		  }
}
