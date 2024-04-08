

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.alg.clustering.KSpanningTreeClustering;
import org.jgrapht.alg.interfaces.ClusteringAlgorithm.Clustering;

import soot.Body;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootResolver;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.tagkit.AnnotationElem;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.tagkit.VisibilityParameterAnnotationTag;
import soot.util.Chain;

import com.google.common.collect.Streams;
import com.google.devtools.common.options.OptionsParser;

import Config.Config;
import FastCallGraph.FastCallGraph;
import FastCallGraph.FastEdge;
import PackageTree.PackageTree;
import PackageTree.Package;
import Util.AndroidUtil;
import Util.Stats;
import Util.IO;
import Util.MainOptions;
import Util.MySootClass;
import Util.SootUtil;
import obfuscation.Obfuscation;

public class Main {

	public static void main(String[] args) {
		/**************************** SETUP *******************************************/


		// cmd arguments
		// System.out.println("[SETUP] Loading in arguments.");
		OptionsParser parser = OptionsParser.newOptionsParser(MainOptions.class);
		parser.parseAndExitUponError(args);
		MainOptions options = parser.getOptions(MainOptions.class);
		if (options.apkPath.equals("")) {
			MainOptions.printUsage(parser);
			return;
		}
		if (options.androidPlatformsPath.equals("")) {
			options.androidPlatformsPath = AndroidUtil.getDefaultAndroidPlatformsPath();
		} else {
			// check if directory exists
			if (!IO.directoryExists(options.androidPlatformsPath)) {
				System.out.println("Android Platforms directory does not exist!");
				MainOptions.printUsage(parser);
				return;
			}
			// set androidPlatformsPath in android util
			AndroidUtil.androidPlatformsPath = options.androidPlatformsPath;
		}
		if (options.srcCLassPath.equals("")) {
			MainOptions.printUsage(parser);
			return;
		}
		if(options.debug.toLowerCase().equals("true")){
			Config.DEBUG = true; //default false
		}
		if(options.useNames.toLowerCase().equals("false")){
			Config.USE_NAMES = false;//default true
		}
		if(options.useCallGraph.toLowerCase().equals("false")){
			Config.USE_CALLGRAPH = false;//default true
		}
		if(options.useAccessModifiers.toLowerCase().equals("false")){
			Config.USE_ACCESS_MODIFIERS = false;//default true
		}
		if(options.useFinalModifier.toLowerCase().equals("true")){
			Config.USE_FINAL_MODIFIER = true; //default false
		}
		if(options.onlyPublic.toLowerCase().equals("true")){
			Config.ONLY_PUBLIC_METHODS_AND_FIELDS = true; //default false
		}
		if(options.incomingCallGraphEdges.toLowerCase().equals("true")){
			Config.USE_INCOMING_CALLGRAPH_EDGES = true; //default false
		}
		Config.CALLGRAPH_TIMEOUT_IN_MINUTES = options.callGraphTimeout;

		// String apk = "calc.apk";
		// String apk = "bunq_V14_12_3.apk";
		// String apk = "crelan_V2_6_1.apk";
		// String apk = "HelloMaldr0id-toast.apk";

		// String sourceDir = System.getProperty("user.dir");
		// String apkPath = sourceDir + File.separator + apk;
		// String outputPath = sourceDir + File.separator + "Output";

		String outputPath = options.outputDir;

		/****************** END OF OPTIONS ********************/

		// check if output dir exists and if not create it
		File directory = new File(outputPath);
		if (!directory.exists()) {
			directory.mkdirs();
		}


		// tmp storage vars
		String tmpStoragePath = outputPath + File.separator + "tmp";
		directory = new File(tmpStoragePath);
		if (!directory.exists()) {
			directory.mkdirs();

		}
		String srcDbDirectory = options.srcCLassPath;

		String apkPath1 = options.apkPath;
		String apk1 = Paths.get(apkPath1).getFileName().toString();
		String apkNoExt1 = apk1.split("\\.", 2)[0];

		String apkPath2 = options.apkMatchPath;
		String apk2 = Paths.get(apkPath2).getFileName().toString();
		String apkNoExt2 = apk2.split("\\.", 2)[0];
		String identifier = apkNoExt1 + "_vs_" + apkNoExt2;
		String identifierWithConfig = identifier + "_" + Config.getIdentifier();

		//check if apk subdir exists
		File apkOutputDir = new File(outputPath + File.separator + identifier);
		if (!apkOutputDir.exists()) {
			apkOutputDir.mkdirs();
		}

		// setting up logger
		String logFileName = "log_" + identifierWithConfig + ".txt";
		try {
			System.setOut(new PrintStream(new File(apkOutputDir + File.separator + logFileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		if(!(new File(apkPath1)).exists()) {
			System.out.println("[ERROR] APK 1 does not exist: " + apkPath1);
			return;
		}
		if(!(new File(apkPath2)).exists()) {
			System.out.println("[ERROR] APK 2 does not exist: " + apkPath2);
			return;
		}

		System.out.println("[SETUP] Database for .class src files = " + srcDbDirectory);
		System.out.println("[SETUP] OutputPath = " + outputPath);
		System.out.println("[SETUP] Temporary Storage = " + tmpStoragePath);

		System.out.println("[SETUP] ApkPath1 = " + apkPath1);
		System.out.println("[SETUP] apk1 = " + apk1);

		System.out.println("[SETUP] ApkPath2 = " + apkPath2);
		System.out.println("[SETUP] apk2 = " + apk2);

		System.out.println("[SETUP] Using identifiers = " + Config.USE_NAMES);
		System.out.println("[SETUP] Only using public fields and methods = " + Config.ONLY_PUBLIC_METHODS_AND_FIELDS);
		System.out.println("[SETUP] Using final modifier = " + Config.USE_FINAL_MODIFIER);
		System.out.println("[SETUP] Using access modifiers = " + Config.USE_ACCESS_MODIFIERS);
		System.out.println("[SETUP] Using incoming callgraph edges = " + Config.USE_INCOMING_CALLGRAPH_EDGES);
		System.out.println("[SETUP] Print Debug Info = " + Config.DEBUG);
		System.out.println("[SETUP] Call Graph Timeout = " + Config.CALLGRAPH_TIMEOUT_IN_MINUTES + " min");

		//DEBUG code for iterative matching based on yellow packages
//		String packageName2 = "Yellow";
//		PackageTree yellowTree = AndroidUtil.loadSrcClasses(srcDbDirectory, packageName2);
//		FastCallGraph yellowCG = AndroidUtil.getPackageCallGraph(tmpStoragePath, srcDbDirectory, packageName2, yellowTree);
//		yellowTree.setCallGraph(yellowCG);
//
//		String packageName3 = "Yellow";
//		PackageTree yellowTree3 = AndroidUtil.loadSrcClasses("C:/Users/Acer/Desktop/Thesis/Database/classFiles2", packageName3);
//		FastCallGraph yellowCG3 = AndroidUtil.getPackageCallGraph("C:/Users/Acer/Desktop/Thesis/Database/classFiles2", "C:/Users/Acer/Desktop/Thesis/Database/classFiles2", packageName3, yellowTree3);
//		yellowTree3.setCallGraph(yellowCG3);
//
//		yellowTree3.findMatches(yellowTree, "Yellow");
//
//		System.out.println("FINISHED");
//		System.exit(1);


		// load androidx
//		String packageName = "androidx";
//		PackageTree androidxTree = AndroidUtil.loadSrcClasses(srcDbDirectory, packageName);
//		FastCallGraph androidxCG = AndroidUtil.getPackageCallGraph(tmpStoragePath, srcDbDirectory, packageName, androidxTree);
//		androidxTree.setCallGraph(androidxCG);



		PackageTree apkTree1 = AndroidUtil.getApkTree(apkPath1, outputPath, apkNoExt1, tmpStoragePath);
		String filePathTree1 = apkOutputDir + File.separator + "tree_" + apkNoExt1 + ".txt";
		apkTree1.writeTo(filePathTree1);
		long tree1Duration = Stats.getMinutes(Config.startTime);
		System.out.println("[INFO] loaded in tree1 in " + tree1Duration + " min");

		PackageTree apkTree2 = AndroidUtil.getApkTree(apkPath2, outputPath, apkNoExt2, tmpStoragePath);
		String filePathTree2 = apkOutputDir + File.separator + "tree_" + apkNoExt2 + ".txt";
		apkTree2.writeTo(filePathTree2);
		long tree2Duration = Stats.getMinutes(Config.startTime);
		System.out.println("[INFO] loaded in tree2 in " + tree2Duration + " min");

		/**************************** END SETUP *******************************************/

		/**************************** MAIN *******************************************/
		// match
		System.out.println("[RUN] Matching classes ....");
		apkTree1.findMatches(apkTree2);
		System.out.println("[RUN] Matching done");
		long matchingDuration = Stats.getMinutes(Config.startTime);
		System.out.println("[INFO] Matching took " + (matchingDuration - tree2Duration) + " min");

		// write tree to txt
		String filePath = apkOutputDir + File.separator + "tree_" + identifierWithConfig + ".txt";
		apkTree1.writeTo(filePath);



		if(apkNoExt1.equals(apkNoExt2)){
			int n = 0;
			ArrayList<MySootClass> allClasses = apkTree1.getAllClassesAndInnerClasses();
			for(MySootClass clazz: allClasses) {
				if(clazz.matchClass != null){
					if(!clazz.sootClass.getName().equals(clazz.matchClass.sootClass.getName())){
						System.out.println("!!! MISMATCH: " + clazz.name);
						n++;
					}
				}
			}
			System.out.println("Comparing " + apkNoExt1 + " with itself: found " + allClasses.size() + " classes with " + n + " mismatches");
		}


		long totalDuration = Stats.getMinutes(Config.startTime);
		Stats.totalDuration = totalDuration;


		//write treeStats to csv
		String csvPath = apkOutputDir + File.separator + identifierWithConfig + "_treeStats.csv";
		List<String> headers = Stats.getHeaders();
		List<String> data = Stats.getData(identifier, apkTree1, apkTree2);
		IO.writeRowToCSV(csvPath, headers, data);

		//write matches to csv
		String matchCsvPath = apkOutputDir + File.separator + identifierWithConfig + "_matches.csv";
		List<String> apkNames = new ArrayList<>();
		apkNames.add(apkNoExt1);
		apkNames.add(apkNoExt2);
		ArrayList<MySootClass> allClasses = apkTree1.getAllClassesAndInnerClasses();
		for(MySootClass clazz: allClasses) {
			List<String> matchdata = new ArrayList<>();
			matchdata.add(clazz.sootClass.getName());
			if(clazz.matchClass != null){
				matchdata.add(clazz.matchClass.sootClass.getName());
			}
			else {
				matchdata.add("no match");
			}
			IO.writeRowToCSV(matchCsvPath, apkNames, matchdata);
		}



		//finish up
		System.out.println("[END] Finished.");
		System.out.println("[END] That took " + totalDuration + " minutes.");


		/**************************** FINISH *******************************************/

	}

}
