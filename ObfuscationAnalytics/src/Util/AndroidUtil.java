package Util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import FastCallGraph.FastCallGraph;
import FastCallGraph.FastEdge;
import PackageTree.PackageTree;
import obfuscation.Obfuscation;
import soot.G;
import soot.PackManager;
import soot.PrimType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.options.Options;
import soot.tagkit.AnnotationElem;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Host;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.tagkit.VisibilityParameterAnnotationTag;
import soot.util.Chain;

import Config.Config;

public class AndroidUtil {
    public static String defaultAndroidPlatformsPath = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local" + File.separator + "Android" + File.separator + "Sdk" + File.separator + "platforms";
    public static String androidPlatformsPath = "";


    public static void setupSootForAPK(String apkPath, String outputPath) {
        G.reset();


        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_prepend_classpath(true);
        //Options.v().set_validate(true);

        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_output_format(Options.output_format_jimple);

        String androidPath = androidPlatformsPath.equals("") ? defaultAndroidPlatformsPath : androidPlatformsPath;
        Options.v().set_android_jars(androidPath);

        Options.v().set_process_dir(Collections.singletonList(apkPath));

        Options.v().set_include_all(true);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_output_dir(outputPath);




        Scene.v().loadNecessaryClasses();

        PackManager.v().runPacks();




    }

    public static CallGraph constructJARCallGraph(String srcDirectory, String packageName){
        System.out.println("[SETUP] Creating " + packageName + " Call Graph ...");
        G.reset();

        Options.v().set_soot_classpath(srcDirectory);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);

        List<SootMethod> entryPoints = new ArrayList<SootMethod>();
        List<SootClass> classes = new ArrayList<>();

        /*
         * Since class files have no main function and callgraphs need an entrypoint
         * we simply iterate all classes, force resolve them to bodies and
         * then mark every method as an entrypoint to force Soot to create
         * a complete callgraph.
         */
        File directoryPath = new File(srcDirectory + File.separator + packageName);
        ArrayList<File> directoriesTODO = new ArrayList<File>();
        directoriesTODO.add(directoryPath);


        while(!directoriesTODO.isEmpty()){
            File currentDir = directoriesTODO.remove(0);
            File filesList[] = currentDir.listFiles();
            for(File f: filesList){
                if(f.isDirectory()){
                    directoriesTODO.add(f);
                }
                else{
                    List<String> parts = Arrays.asList(f.getPath().replace("\\", "/").split("/"));
                    parts = parts.subList(parts.indexOf(packageName), parts.size());
                    //remove .class
                    if(f.getName().lastIndexOf('.') == -1){
                        //file has no extension, skip
                        continue;
                    }
                    if(!f.getName().substring(f.getName().lastIndexOf('.') + 1, f.getName().length()).equals("class")){
                        //extension is not .class
                        continue;
                    }
                    String className = f.getName().substring(0, f.getName().lastIndexOf('.'));
                    parts.set(parts.size() - 1, className);
                    Scene.v().addBasicClass(String.join(".", parts), SootClass.SIGNATURES);
                    SootClass c = Scene.v().forceResolve(String.join(".", parts), SootClass.BODIES);
                    classes.add(c);
                }
            }
        }
        Scene.v().loadNecessaryClasses();
        for(SootClass c: classes){
            c.setApplicationClass();
            for(SootMethod m: c.getMethods()){
                entryPoints.add(m);
            }

        }

        Scene.v().setEntryPoints(entryPoints);
        PackManager.v().runPacks();

        CallGraph cg = Scene.v().getCallGraph();
        System.out.println("[SETUP] "+packageName+" CallGraph size: " + cg.size());
        return cg;
    }


    public static CallGraph constructAPKCallGraph(String apkPath){
        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        String androidPath = androidPlatformsPath.equals("") ? defaultAndroidPlatformsPath : androidPlatformsPath;
        config.getAnalysisFileConfig().setAndroidPlatformDir(androidPath);
        config.getAnalysisFileConfig().setTargetAPKFile(apkPath);
        config.setMergeDexFiles(true);
        config.getCallbackConfig().setCallbackAnalysisTimeout(Config.CALLGRAPH_TIMEOUT_IN_MINUTES * 60);

        SetupApplication analyzer = new SetupApplication(config);
        analyzer.constructCallgraph();

        return Scene.v().getCallGraph();
    }

    public static List<String> getclassPathParts(String fullClassPath) {
        //split on dot (eg com.android.className)
        ArrayList<String> classPathParts  = new ArrayList<String>(Arrays.asList(fullClassPath.split("\\.")));
        return classPathParts;
    }

    public static String getClassName(String fullClassPath){
    	List<String> parts = getclassPathParts(fullClassPath);
    	return parts.get(parts.size() - 1);
    }

    public static void cleanDirectory(String outputPath) {
        final File[] files = (new File(outputPath)).listFiles();
        if (files != null && files.length > 0) {
            for(File file: files){
                file.delete();
            }
        }
    }

    public static String getDefaultAndroidPlatformsPath(){
        return defaultAndroidPlatformsPath;
    }

    public static PackageTree loadSrcClasses(String srcDirectory, String packageName){
        G.reset();

        Options.v().set_soot_classpath(srcDirectory);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_src_prec(Options.src_prec_class);

        ArrayList<File> directoriesTODO = new ArrayList<File>();

        //also add standard packages: java, javax, android
        String[] packs = {"java", "javax", "android", packageName};
        for(String pack: packs){
        	File f = new File(srcDirectory + File.separator + pack);
        	if(!f.exists()){
        		throw new RuntimeException("Cannot find source files for " + pack + "!");
        	}
        	directoriesTODO.add(f);
        }

        while(!directoriesTODO.isEmpty()){
            File currentDir = directoriesTODO.remove(0);
            File filesList[] = currentDir.listFiles();
            for(File f: filesList){
                if(f.isDirectory()){
                    directoriesTODO.add(f);
                }
                else{
                    List<String> parts = Arrays.asList(f.getPath().replace("\\", "/").split("/"));
                    if(parts.indexOf(packageName) != -1){
                    	parts = parts.subList(parts.indexOf(packageName), parts.size());
                    }
                    else if (parts.indexOf("java") != -1){
                    	parts = parts.subList(parts.indexOf("java"), parts.size());
                    }
                    else if (parts.indexOf("javax") != -1){
                    	parts = parts.subList(parts.indexOf("javax"), parts.size());
                    }
                    else if (parts.indexOf("android") != -1){
                    	parts = parts.subList(parts.indexOf("android"), parts.size());
                    }


                    //remove .class
                    if(f.getName().lastIndexOf('.') == -1){
                        //file has no extension, skip
                        continue;
                    }
                    if(!f.getName().substring(f.getName().lastIndexOf('.') + 1, f.getName().length()).equals("class")){
                        //extension is not .class
                        continue;
                    }
                    String className = f.getName().substring(0, f.getName().lastIndexOf('.'));
                    parts.set(parts.size() - 1, className);
                    Scene.v().addBasicClass(String.join(".", parts), SootClass.SIGNATURES);
                }
            }
        }

        Scene.v().loadNecessaryClasses();
        Chain<SootClass> allClasses =  Scene.v().getClasses();
        //convert to list
        List<SootClass> strictlyPackageClasses = new ArrayList<SootClass>();
        for(SootClass s: allClasses) strictlyPackageClasses.add(s);

        //sort alphabetically to ensure class "a" is inserted before "a$b"
        Collections.sort(strictlyPackageClasses, new Comparator<SootClass>() {
              @Override
              public int compare(SootClass u1, SootClass u2) {
                return u1.getName().compareTo(u2.getName());
              }
            });

        PackageTree srcTree = new PackageTree();
        strictlyPackageClasses.forEach((clazz) -> srcTree.addClass(clazz));
        System.out.println("[SETUP] Loaded " + strictlyPackageClasses.size() + " " + packageName + " classes into " + packageName + " tree");
        return srcTree;
    }

    public static FastCallGraph getPackageCallGraph(String tmpStoragePath, String srcDbDirectory, String packageName, PackageTree srcTree){
        String callGraphPath = tmpStoragePath + File.separator + "callGraph_" + packageName + "_" + Config.getIdentifier() +  ".ser";
        FastCallGraph cg = (FastCallGraph) IO.readObject(callGraphPath);
        if(cg == null){
            CallGraph cgSlow = AndroidUtil.constructJARCallGraph(srcDbDirectory, packageName);


            cg = new FastCallGraph(cgSlow, srcTree);
            IO.writeObject(callGraphPath, cg);
        }
        cg.setPackageTree(srcTree);
        return cg;
    }

    public static FastCallGraph getAPKCallGraph(String tmpStoragePath, String apkPath, String apkName, PackageTree apkTree){
    	long startTime2 = System.currentTimeMillis();
        String callGraphPath = tmpStoragePath + File.separator + "callGraph_" + apkName + "_" +  Config.getIdentifier() +  ".ser";
        FastCallGraph cg = (FastCallGraph) IO.readObject(callGraphPath);
        if(cg == null){
            CallGraph cgSlow = AndroidUtil.constructAPKCallGraph(apkPath);
            cg = new FastCallGraph(cgSlow, apkTree);
            IO.writeObject(callGraphPath, cg);
        }
        cg.setPackageTree(apkTree);
		if(Stats.generateCallGraph1Duration == -1){
			Stats.generateCallGraph1Duration = Stats.getMinutes(startTime2);
		}
		else{
			Stats.generateCallGraph2Duration = Stats.getMinutes(startTime2);
		}
        return cg;
    }

    public static PackageTree getApkTree(String apkPath, String outputPath, String apkNoExt, String tmpStoragePath){
    	long startTime = System.currentTimeMillis();
		// Initialize Soot, must be last for correct options settings!
		System.out.print("[SETUP] Setting up soot and loading in classes from apk " + apkNoExt + " ... ");
		AndroidUtil.setupSootForAPK(apkPath, outputPath);
		System.out.println("done");

		// create apkTree
		System.out.print("[SETUP] Loading classes from apk in tree sturcture ... ");
		PackageTree apkTree = new PackageTree();
		Chain<SootClass> chain = Scene.v().getClasses();
		ArrayList<SootClass> sceneClasses = new ArrayList<SootClass>();
		chain.forEach((clazz) -> sceneClasses.add(clazz));
		chain = null;// delete chain
		Collections.sort(sceneClasses, new Comparator<SootClass>() {
			@Override
			public int compare(SootClass u1, SootClass u2) {
				return u1.getName().compareTo(u2.getName());
			}
		});
		for (SootClass clazz : sceneClasses)
			apkTree.addClass(clazz);
		System.out.println("done");

		if(Stats.loadAPKTree1Duration == -1){
			Stats.loadAPKTree1Duration = Stats.getMinutes(startTime);
		}
		else{
			Stats.loadAPKTree2Duration = Stats.getMinutes(startTime);
		}

		// apk callgraph
		if(Config.USE_CALLGRAPH){
			System.out.println("[RUN] Constructing "+ apkNoExt +" Call Graph ... ");
			FastCallGraph APKCallGraph = AndroidUtil.getAPKCallGraph(tmpStoragePath, apkPath, apkNoExt, apkTree);
			apkTree.setCallGraph(APKCallGraph);
		}


		return apkTree;
    }

    public static List<String> getAnnotations(Host host){
    	//host = sootmethod, sootclass, sootfield etc.
    	List<String> output = new ArrayList<>();
		for(Tag tag: host.getTags()){
			if(tag instanceof VisibilityAnnotationTag){
				for (AnnotationTag annotation : ((VisibilityAnnotationTag)tag).getAnnotations()) {
					String annotationString = "";
					annotationString += annotation.getType();
					for(AnnotationElem elem: annotation.getElems()){
						annotationString += elem.toString();
					}
					output.add(annotationString);
				}
			}
		}
		return output;
    }

    public static List<String> getParameterAnnotations(SootMethod m){
    	List<String> output = new ArrayList<>();
    	for(Tag tag: m.getTags()){
			if (tag instanceof VisibilityParameterAnnotationTag){
				for (VisibilityAnnotationTag annotation : ((VisibilityParameterAnnotationTag)tag).getVisibilityAnnotations()) {
					if(annotation == null){
						output.add("");
						continue;
					}
					if(annotation.hasAnnotations()){
						String annotationString = "";
						for (AnnotationTag annotation2 : annotation.getAnnotations()) {
							annotationString += annotation2.getType();
							for(AnnotationElem elem: annotation2.getElems()){
								annotationString += elem.toString();
							}
						}
						output.add(annotationString);
					}
					else {
						output.add("");
					}
				}
			}
    	}
    	return output;
    }


}
