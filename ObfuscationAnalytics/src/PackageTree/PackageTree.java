package PackageTree;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import Config.Config;
import FastCallGraph.FastCallGraph;
import FastCallGraph.FastEdge;
import SootCompare.ClassVerify;
import SootCompare.SootCompare;
import SootCompare.SootCompare.MatchType;
import Util.AndroidUtil;
import Util.Stats;
import Util.IO;
import Util.MySootClass;
import Util.MySootInnerClass;
import Util.SootUtil;
import obfuscation.Obfuscation;
import soot.ArrayType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;

public class PackageTree extends Package {

    public PackageTree() {
        super(null, null);
    }

    //iternal vars
    private PackageTree srcTree = null;
    private FastCallGraph callGraph = null;
    private Map<String, List<MySootClass>> classHashMap = null;


    public void setCallGraph(FastCallGraph callGraph){
        this.callGraph = callGraph;
    }

    public void setSrcTree(PackageTree srcTree){
        this.srcTree = srcTree;
        if(this.classHashMap == null){
            this.classHashMap = SootUtil.getClassHashMap(this.srcTree.getAllClassesAndInnerClasses());
        }
    }

    public MySootClass addClass(SootClass sootClass) {
        String fullClassPath = sootClass.getName();
        List<String> classPathParts  = AndroidUtil.getclassPathParts(fullClassPath);
        //remove actual class name
        String className = classPathParts.remove(classPathParts.size() - 1);

        if(Config.USE_INNER_CLASS_LINK_INFO){
            if(sootClass.hasOuterClass()){
                MySootClass outerClass = this.findClass(sootClass.getOuterClass().getName());
                if(outerClass == null){
                    outerClass = this.addClass(sootClass.getOuterClass());
                }
                String[] nameParts = className.split("\\$");
                MySootInnerClass innerClass = new MySootInnerClass(outerClass.pack, sootClass, nameParts[nameParts.length - 1]);
                outerClass.innerClasses.add(innerClass);
                return innerClass;
            }
        }


        //find package
        Package currPack = this;
        for(String part: classPathParts){
            Package correctPack = null;
            ArrayList<Package> packs = currPack.packages;
            for(Package pack: packs){
                if(pack.path.equals(part)){
                    correctPack = pack;
                    break;
                }
            }
            if(correctPack == null) {
                correctPack = new Package(part, currPack);
                packs.add(correctPack);
            }
            currPack = correctPack;
        }

        ArrayList<MySootClass> outerClasses = currPack.classes;
        for(MySootClass outerClass: outerClasses) {
            if(outerClass.sootClass.getName().equals(fullClassPath)){
                return outerClass;
            }
        }
        MySootClass mySootClass = new MySootClass(currPack, sootClass, className);
        outerClasses.add(mySootClass);
        return mySootClass;

    }


    @Override
    public String toString() {
        String output = "";
        for(Package pack: packages){
            output += pack.toString(0) + newLine;
        }
        for(MySootClass mySootClass: classes) {
            output += mySootClass.toString() + newLine;
        }
        return output;
    }

    public void writeTo(String filePath){
        IO.writeToFile(filePath, toString());
    }


    public enum MatchMethod {
        CLASS_SIGNATURE_AND_NAMES,
        CLASS_SIGNATURE,
        CALL_GRAPH,
        CALL_GRAPH_AFTER_SIGNATURE,
        SUPER,
        FIELD,
        METHOD,
        OPT1
    }

    private MySootClass matchOn(MatchMethod method, MySootClass classToMatch, List<MySootClass> classesRemainingOptions) {
        List<MySootClass> potentialMatches = new ArrayList<>();

        for(MySootClass srcClazz: classesRemainingOptions){
            switch(method) {
                case CLASS_SIGNATURE_AND_NAMES:
                    if(SootCompare.isMySootClassPotentialMatch(classToMatch, this, srcClazz, this.srcTree, true)){
                        potentialMatches.add(srcClazz);
                    }
                    break;
                case CLASS_SIGNATURE:
                    if(SootCompare.isMySootClassPotentialMatch(classToMatch, this, srcClazz, this.srcTree, false)){
                        potentialMatches.add(srcClazz);
                    }
                    break;
                default:
                    throw new RuntimeException("This method for matching does not fit here!");
            }
        }

        if(potentialMatches.size() == 1){
        	switch(method){
        		case CLASS_SIGNATURE_AND_NAMES:
        			classToMatch.matchMethod = MatchMethod.CLASS_SIGNATURE_AND_NAMES;
        			break;
        		case CLASS_SIGNATURE:
        			classToMatch.matchMethod = MatchMethod.CLASS_SIGNATURE;
        			break;
				default:
					throw new RuntimeException("This method for matching does not fit here!");
        	}
            return potentialMatches.get(0);
        }
        else if (potentialMatches.size() > 1){
            //is to know later if no match because of no options or too many
            classToMatch.hasOptions = true;

            //attempt to narrow down options with callgraph
            if(Config.USE_CALLGRAPH){
                MySootClass cgMatch = matchOnCallGraph(classToMatch, classesRemainingOptions);
                if(cgMatch != null){
                	classToMatch.matchMethod = MatchMethod.CALL_GRAPH_AFTER_SIGNATURE;
                }
                return cgMatch;
            }
        }
        return null;
    }

    private MySootClass matchOnCallGraph(MySootClass classToMatch, List<MySootClass> classesRemainingOptions){
        if(this.callGraph == null || this.srcTree == null || this.srcTree.callGraph == null){
            throw new RuntimeException("PackageTree vars are not set for this function!");
        }
        //first calculated edges out so dont repeat it unnecessarily
        Map<SootClass, Map<SootMethod, List<FastEdge>>> classesToMatchEdgesOut = this.callGraph.getEdgesOutOfClasses();
        Map<SootClass, Map<SootMethod, List<FastEdge>>> classesRemainingOptionsEdgesOut = srcTree.callGraph.getEdgesOutOfClasses();



        if(classToMatch.sootClass.isPhantom()) return null;

        List<MySootClass> potentialMatches = new ArrayList<>();
        for (Iterator<MySootClass> cIt2 = classesRemainingOptions.iterator(); cIt2.hasNext();) {
            MySootClass srcClazz = cIt2.next();

            if(srcClazz.sootClass.isPhantom()) continue;
            if(classToMatch.sootClass.getMethodCount() == 0) continue;
            if(srcClazz.sootClass.getMethodCount() == 0) continue;


            //early stop if methodcount is not equal
            List<SootMethod> l1 = new ArrayList<>(classToMatch.sootClass.getMethods());
            List<SootMethod> l2 = new ArrayList<>(srcClazz.sootClass.getMethods());
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
            if(l1.size() != l2.size()) continue;



            //OUTOGING EDGES
            int nMethodsWithEdgesOut = 0;
            int nMethodMatchesOut = 0;
            Map<SootMethod, List<FastEdge>> methodEdges1 = new HashMap<>();
            Map<SootMethod, List<FastEdge>> methodEdgeOriginal1 = classesToMatchEdgesOut.get(classToMatch.sootClass);
            if(methodEdgeOriginal1 != null ) methodEdges1.putAll(methodEdgeOriginal1);
            Map<SootMethod, List<FastEdge>> methodEdges2 = new HashMap<>();
            Map<SootMethod, List<FastEdge>> methodEdgeOriginal2 = classesRemainingOptionsEdgesOut.get(srcClazz.sootClass);
            if(methodEdgeOriginal2 != null ) methodEdges2.putAll(methodEdgeOriginal2);
            for (Iterator<Entry<SootMethod, List<FastEdge>>> mIt1 = methodEdges1.entrySet().iterator();mIt1.hasNext();) {
                Entry<SootMethod, List<FastEdge>> kv1 = mIt1.next();
                List<FastEdge> edges1 = new ArrayList<>(kv1.getValue());

                nMethodsWithEdgesOut++;

                for (Iterator<Entry<SootMethod, List<FastEdge>>> mIt2 = methodEdges2.entrySet().iterator();mIt2.hasNext();) {
                    Entry<SootMethod, List<FastEdge>> kv2 = mIt2.next();
                    List<FastEdge> edges2 = new ArrayList<>(kv2.getValue());


                    if(edges1.size() != edges2.size()) continue;

                    for (Iterator<FastEdge> eIt1 = edges1.iterator(); eIt1.hasNext();) {
                        FastEdge e1 = eIt1.next();
                        for (Iterator<FastEdge> eIt2 = edges2.iterator(); eIt2.hasNext();) {
                            FastEdge e2 = eIt2.next();
                            if(SootCompare.isMethodMatch(e1.tgt(), this, e2.tgt(), srcTree, false, MatchType.POTENTIAL)
                                    && SootCompare.isMethodMatch(e1.src(), this, e2.src(), srcTree, false, MatchType.POTENTIAL)){
                                eIt1.remove();
                                eIt2.remove();
                                break;
                            }
                        }
                    }
                    if(edges1.size() == 0 && edges2.size() == 0){
                        nMethodMatchesOut++;
                        mIt1.remove();
                        mIt2.remove();
                        break;
                    }
                }
            }



            //INCOMING EDGES
            int nMethodsWithEdgesIn = 0;
            int nMethodMatchesIn = 0;
            if(Config.USE_INCOMING_CALLGRAPH_EDGES){
                Map<SootClass, Map<SootMethod, List<FastEdge>>> classesToMatchEdgesIn = this.callGraph.getEdgesInClasses();
                Map<SootClass, Map<SootMethod, List<FastEdge>>> classesRemainingOptionsEdgesIn = srcTree.callGraph.getEdgesInClasses();
                Map<SootMethod, List<FastEdge>> methodEdgesIn1 = new HashMap<>();
                Map<SootMethod, List<FastEdge>> methodEdgeOriginalIn1 = classesToMatchEdgesIn.get(classToMatch.sootClass);
                if(methodEdgeOriginalIn1 != null ) methodEdgesIn1.putAll(methodEdgeOriginalIn1);
                Map<SootMethod, List<FastEdge>> methodEdgesIn2 = new HashMap<>();
                Map<SootMethod, List<FastEdge>> methodEdgeOriginalIn2 = classesRemainingOptionsEdgesIn.get(srcClazz.sootClass);
                if(methodEdgeOriginalIn2 != null ) methodEdgesIn2.putAll(methodEdgeOriginalIn2);
                for (Iterator<Entry<SootMethod, List<FastEdge>>> mIt1 = methodEdgesIn1.entrySet().iterator();mIt1.hasNext();) {
                    Entry<SootMethod, List<FastEdge>> kv1 = mIt1.next();
                    List<FastEdge> edges1 = new ArrayList<>(kv1.getValue());

                    nMethodsWithEdgesIn++;

                    for (Iterator<Entry<SootMethod, List<FastEdge>>> mIt2 = methodEdgesIn2.entrySet().iterator();mIt2.hasNext();) {
                        Entry<SootMethod, List<FastEdge>> kv2 = mIt2.next();
                        List<FastEdge> edges2 = new ArrayList<>(kv2.getValue());


                        if(edges1.size() != edges2.size()) continue;

                        for (Iterator<FastEdge> eIt1 = edges1.iterator(); eIt1.hasNext();) {
                            FastEdge e1 = eIt1.next();
                            for (Iterator<FastEdge> eIt2 = edges2.iterator(); eIt2.hasNext();) {
                                FastEdge e2 = eIt2.next();
                                if(SootCompare.isMethodMatch(e1.tgt(), this, e2.tgt(), srcTree, false, MatchType.POTENTIAL)
                                        && SootCompare.isMethodMatch(e1.src(), this, e2.src(), srcTree, false, MatchType.POTENTIAL)){
                                    eIt1.remove();
                                    eIt2.remove();
                                    break;
                                }
                            }
                        }
                        if(edges1.size() == 0 && edges2.size() == 0){
                        	nMethodMatchesIn++;
                            mIt1.remove();
                            mIt2.remove();
                            break;
                        }
                    }
                }

            }





            if((nMethodMatchesOut == nMethodsWithEdgesOut && nMethodMatchesOut > 0) && ((nMethodMatchesIn == nMethodsWithEdgesIn && nMethodMatchesIn > 0) || !Config.USE_INCOMING_CALLGRAPH_EDGES)){
                //the methods with edges out of them matched, now check the remaining methods
                List<SootMethod> ms1 = new ArrayList<>(classToMatch.sootClass.getMethods());
                List<SootMethod> ms2 = new ArrayList<>(srcClazz.sootClass.getMethods());

                ms1.removeAll(this.callGraph.getEdgesOutOfClasses().get(classToMatch.sootClass).keySet());
                ms2.removeAll(srcTree.callGraph.getEdgesOutOfClasses().get(srcClazz.sootClass).keySet());

                if(Config.USE_INCOMING_CALLGRAPH_EDGES){
                	//also remove edges with incoming edges, so only methods without incoming edges remain
                    ms1.removeAll(this.callGraph.getEdgesInClasses().get(classToMatch.sootClass).keySet());
                    ms2.removeAll(srcTree.callGraph.getEdgesInClasses().get(srcClazz.sootClass).keySet());
                }

                for (Iterator<SootMethod> it1 = ms1.iterator(); it1.hasNext();) {
                    SootMethod m1 = it1.next();
                    for (Iterator<SootMethod> it2 = ms2.iterator(); it2.hasNext();) {
                        SootMethod m2 = it2.next();
                        if(SootCompare.isMethodMatch(m1, this, m2, srcTree, false, MatchType.POTENTIAL)){
                            it1.remove();
                            it2.remove();
                            break;
                        }
                    }
                }
                //methods without edges out also match
                if(ms1.size() == 0 && ms2.size() == 0) {
                    potentialMatches.add(srcClazz);
                }
            }
        }

        if(potentialMatches.size() == 1){
        	classToMatch.matchMethod =  MatchMethod.CALL_GRAPH;
            return potentialMatches.get(0);
        }
        if(potentialMatches.size() > 1){
            classToMatch.hasOptions = true;
        }
        return null;
    }

    private boolean matchClasses(List<MySootClass> classesToMatch, List<MySootClass> classesRemainingOptions){
        //first check if no classes are matched from other classes (e.g. was superclass)
        for (Iterator<MySootClass> it = classesToMatch.iterator(); it.hasNext();) {
            MySootClass classToMatch = it.next();
            if(classToMatch.matchClass != null){
                it.remove();
                if(classesRemainingOptions.contains(classToMatch.matchClass)){
                    classesRemainingOptions.remove(classToMatch.matchClass);
                }
            }
        }
        //begin matching
        boolean foundMatchesFunction = false;
        //foundMatchesFunction |= matchOnClassName(classesToMatch, classesRemainingOptions);

        boolean foundMatchesWhile = true;
        while(foundMatchesWhile){
            foundMatchesWhile = false;

            MySootClass[] matchClasses = classesToMatch.toArray(new MySootClass[0]);
            MySootClass[] matchClassesSrc = new MySootClass[classesToMatch.size()];
            //MatchMethod[] matchMethods = new MatchMethod[classesToMatch.size()];

            List<Thread> threads = new ArrayList<>();

            for(int i = 0; i < classesToMatch.size(); i++) {
                MySootClass classToMatch = matchClasses[i];
                if(classToMatch.matchClass != null) {
                    //when it has matched in matchOnMatch for example
                    if(classesRemainingOptions.contains(classToMatch.matchClass)){
                        classesRemainingOptions.remove(classToMatch.matchClass);
                    }
                    continue;
                }



                final int iConstant = i;//copy i in final var to use inside of thread
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        MySootClass match = null;
                        //reduce options size based on hash, !!only for this class
                        List<MySootClass> options = classHashMap.get(SootUtil.sootClass2hash(classToMatch.sootClass));
                        if(options == null){
                            return;
                        }
                        List<MySootClass> classesRemainingOptionsReduced = new ArrayList<>(options);
                        classesRemainingOptionsReduced.retainAll(classesRemainingOptions);


                        //check class sig & names
                        if(Config.USE_NAMES){
                            match = matchOn(MatchMethod.CLASS_SIGNATURE_AND_NAMES, classToMatch, classesRemainingOptionsReduced);
                            if(match != null){
                                matchClassesSrc[iConstant] = match;
                                return;
                            }
                        }

                          //check class sig
                        match = matchOn(MatchMethod.CLASS_SIGNATURE, classToMatch, classesRemainingOptionsReduced);
                           if(match != null){
                            matchClassesSrc[iConstant] = match;
                            return;
                        }

                        //check call graph for matches
                        if(Config.USE_CALLGRAPH){
                            match = matchOnCallGraph(classToMatch, classesRemainingOptionsReduced);
                               if(match != null){
                                   matchClassesSrc[iConstant] = match;
                                   return;
                            }
                        }
                    }
                };
                Thread t = new Thread(runnable);
                threads.add(t);
                t.start();
            }

            //synchronize: barrier
            for(Thread t: threads){
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //check for unique matches, set duplicates to null
            for(int i = 0; i < classesToMatch.size(); i++){
                if(matchClassesSrc[i] != null){
                    boolean foundDuplicate = false;
                    for(int j = i+1; j < classesToMatch.size(); j++){
                        if(matchClassesSrc[i] == matchClassesSrc[j]){
                            matchClassesSrc[j] = null;
                            foundDuplicate = true;
                        }
                    }
                    if(foundDuplicate){
                        matchClassesSrc[i] = null;
                    }
                }
            }

            //all unique matches, link them together
            for(int i = 0; i < matchClasses.length; i++){
                if(matchClasses[i].matchClass != null){
                    classesToMatch.remove(matchClasses[i]);
                    continue;
                }
                if(matchClassesSrc[i] != null){
                    matchClasses[i].matchClass = matchClassesSrc[i];
                    classesToMatch.remove(matchClasses[i]);
                    classesRemainingOptions.remove(matchClassesSrc[i]);
                    foundMatchesWhile = true;
                    switch(matchClasses[i].matchMethod){
                        case CLASS_SIGNATURE_AND_NAMES:
                            if(SootUtil.isSootClassMangled(matchClasses[i].sootClass)){
                                matchClasses[i].pack.nMatchedOnClassSigAndNameObf++;
                            }
                            else{
                                matchClasses[i].pack.nMatchedOnClassSigAndNameNonObf++;
                            }
                            matchClasses[i].name += " (class sig & names: " + matchClassesSrc[i].sootClass.getName() + ")";
                            break;
                        case CLASS_SIGNATURE:
                        	if(SootUtil.isSootClassMangled(matchClasses[i].sootClass)){
                                matchClasses[i].pack.nMatchedOnClassSigObf++;
                            }
                            else{
                                matchClasses[i].pack.nMatchedOnClassSigNonObf++;
                            }
                            matchClasses[i].name += " (class sig: " + matchClassesSrc[i].sootClass.getName() + ")";
                            break;
                        case CALL_GRAPH_AFTER_SIGNATURE:
                        	if(SootUtil.isSootClassMangled(matchClasses[i].sootClass)){
                                matchClasses[i].pack.nMatchedOnCGAfterSigObf++;
                            }
                            else{
                                matchClasses[i].pack.nMatchedOnCGAfterSigNonObf++;
                            }
                            matchClasses[i].name += " (CG after sig: " + matchClassesSrc[i].sootClass.getName() + ")";
                            break;
                        case CALL_GRAPH:
                        	if(SootUtil.isSootClassMangled(matchClasses[i].sootClass)){
                                matchClasses[i].pack.nMatchedOnCGObf++;
                            }
                            else{
                                matchClasses[i].pack.nMatchedOnCGNonObf++;
                            }
                            matchClasses[i].name += " (CG: " + matchClassesSrc[i].sootClass.getName() + ")";
                            break;
                    }


                    if(Config.USE_INNER_CLASS_LINK_INFO){
                        matchChildren(matchClasses[i], matchClassesSrc[i]);
                    }
                }
            }
            for(int i = 0; i < matchClasses.length; i++){
                if(matchClasses[i].matchClass != null){
                	matchOnMatch(matchClasses[i]);
                }
            }

            foundMatchesFunction |= foundMatchesWhile;
        }


        // fifth check if only one remaining option
        if(classesToMatch.size() == 1 && classesRemainingOptions.size() == 1){
            MySootClass clazz = classesToMatch.get(0);
            clazz.matchClass = classesRemainingOptions.get(0);
            clazz.name += " (1 opt: " + classesRemainingOptions.get(0).sootClass.getName() + ")";
            if(Config.USE_INNER_CLASS_LINK_INFO){
                matchChildren(clazz, classesRemainingOptions.get(0));
            }
        	if(SootUtil.isSootClassMangled(clazz.sootClass)){
                clazz.pack.nMatchedClasses1optObf++;
            }
            else{
                clazz.pack.nMatchedClasses1optNonObf++;
            }
            clazz.matchMethod = MatchMethod.OPT1;
            classesToMatch.remove(0);
            classesRemainingOptions.remove(0);
            return true;
        }

        return foundMatchesFunction;
    }

    private boolean matchOnMatch(MySootClass clazz){
        //this function is called whenever a match is found
        //when a match is found, other information could be used to narrow down options
        //for example, the superclasses of a match can often also be matched
        //or when for example only one unknown ref is present in the class, they must match as well
        // e.g. an interface with 1 function "a.b.unknownRef doSomething();"
        boolean foundMatches = false;
        MySootClass match = clazz.matchClass;

        if(Config.USE_SUPERCLASSES_ON_NEW_MATCH){
            //superclass
            if(clazz.sootClass.hasSuperclass() && match.sootClass.hasSuperclass()){
                SootClass super1 = clazz.sootClass.getSuperclass();
                MySootClass mySuper1 = this.findClass(super1.getName());
                if(mySuper1.matchClass == null){
                    SootClass super2 = match.sootClass.getSuperclass();
                    if(SootCompare.isSootClassSignaturePotentialMatch(super1, this, super2, srcTree, false)){
                        MySootClass mySuper2 = this.srcTree.findClass(super2.getName());
                        mySuper1.matchClass = mySuper2;
                        mySuper1.name += " (super match: " + mySuper2.sootClass.getName() + ")";
                    	if(SootUtil.isSootClassMangled(mySuper1.sootClass)){
                            mySuper1.pack.nMatchedOnSuperObf++;
                        }
                        else{
                            mySuper1.pack.nMatchedOnSuperNonObf++;
                        }
                        mySuper1.matchMethod = MatchMethod.SUPER;
                        matchOnMatch(mySuper1);
                        foundMatches = true;
                    }
                }
            }
        }


        if(Config.USE_INTERFACES_ON_NEW_MATCH){
            //interfaces
            List<SootClass> i1 = SootUtil.chain2list(clazz.sootClass.getInterfaces());
            List<SootClass> i2 = SootUtil.chain2list(match.sootClass.getInterfaces());
            List<MySootClass> interfaces1 = new ArrayList<>();
            i1.forEach((iface) -> interfaces1.add(this.findClass(iface.getName())));
            List<MySootClass> interfaces2 = new ArrayList<>();
            i2.forEach((iface) -> interfaces2.add(this.srcTree.findClass(iface.getName())));
            foundMatches |= matchClasses(interfaces1, interfaces2);
        }


        if(Config.USE_FIELDS_ON_NEW_MATCH){
            //fields
            //first remove exact primitive & standard package (java, android etc) matches
            //then if only one remains, this ref must match, only if modifiers match as well ofcourse
            List<SootField> fields1 = SootUtil.chain2list(clazz.sootClass.getFields());
            List<SootField> fields2 = SootUtil.chain2list(match.sootClass.getFields());
            SootCompare.removeExactUniqueFieldMatches(fields1, this, fields2, this.srcTree);
            if(fields1.size() == 1 && fields2.size() == 1) {
                Type f1Type = fields1.get(0).getType();
                Type f2Type = fields2.get(0).getType();
                boolean sameArrayDepth = true;
                while(f1Type instanceof ArrayType){
                    if(!(f2Type instanceof ArrayType)){
                        sameArrayDepth = false;
                        break;
                    }
                    f1Type = ((ArrayType) f1Type).getElementType();
                    f2Type = ((ArrayType) f2Type).getElementType();
                }
                if(f2Type instanceof ArrayType) sameArrayDepth = false;
                if(sameArrayDepth){
                    MySootClass f1 = this.findClass(f1Type.toString());
                    if(!SootUtil.isBasicType(f1Type) && !SootUtil.isBasicType(f2Type)){
                        //f1 can be null if for example only one field does not match
                        //and the fields are both float (diff modifiers)
                        if(f1.matchClass == null){
                            MySootClass f2 = this.srcTree.findClass(f2Type.toString());
                            if(SootCompare.isMySootClassPotentialMatch(f1, this, f2, srcTree, false)){
                                f1.matchClass = f2;
                                f1.name += " (1field match: " + f2.sootClass.getName() + ")";
                                if(SootUtil.isSootClassMangled(f1.sootClass)){
                                    f1.pack.nMatchedOnFieldObf++;
                                }
                                else{
                                    f1.pack.nMatchedOnFieldNonObf++;
                                }
                                f1.matchMethod = MatchMethod.FIELD;
                                matchOnMatch(f1);
                                foundMatches = true;
                            }
                        }
                    }
                }
            }
        }


        if(Config.USE_METHODS_ON_NEW_MATCH){
            //methods
            List<SootMethod> methods1 = new ArrayList<>(clazz.sootClass.getMethods());
            List<SootMethod> methods2 = new ArrayList<>(match.sootClass.getMethods());
            SootCompare.removeExactUniqueMethodMatches(methods1, this, methods2, this.srcTree);
            if(methods1.size() == 1 && methods2.size() == 1) {
                SootMethod m1 = methods1.get(0);
                SootMethod m2 = methods2.get(0);
                if(m1.getParameterCount() == m2.getParameterCount()){
                    List<Type> typesToMatchA = new ArrayList<>();
                    List<Type> typesToMatchB = new ArrayList<>();
                    typesToMatchA.add(m1.getReturnType());
                    typesToMatchB.add(m2.getReturnType());
                    for(int i = 0; i < m1.getParameterCount(); i++){
                        typesToMatchA.add(m1.getParameterType(i));
                        typesToMatchB.add(m2.getParameterType(i));
                    }

                    for(int i = 0; i < typesToMatchA.size(); i++){
                        Type a = typesToMatchA.get(i);
                        Type b = typesToMatchB.get(i);
                        if(SootCompare.areTypesExactMatch(a, this, b, this.srcTree) != -1) continue;
                        boolean sameArrayDepth = true;
                        while(a instanceof ArrayType){
                            if(!(b instanceof ArrayType)){
                                sameArrayDepth = false;
                                break;
                            }
                            a = ((ArrayType) a).getElementType();
                            b = ((ArrayType) b).getElementType();
                        }
                        if(b instanceof ArrayType) sameArrayDepth = false;
                        if(sameArrayDepth){
                            MySootClass c1 = this.findClass(a.toString());
                            if(!SootUtil.isBasicType(a) && !SootUtil.isBasicType(b)){
                                if(c1.matchClass == null){
                                    MySootClass c2 = this.srcTree.findClass(b.toString());
                                    if(SootCompare.isMySootClassPotentialMatch(c1, this, c2, srcTree, false)){
                                        c1.matchClass = c2;
                                        c1.name += " (1method match: " + c2.sootClass.getName() + ")";
                                        if(SootUtil.isSootClassMangled(c1.sootClass)){
                                            c1.pack.nMatchedOnMethodsObf++;
                                        }
                                        else{
                                            c1.pack.nMatchedOnMethodsNonObf++;
                                        }
                                        c1.matchMethod = MatchMethod.METHOD;
                                        matchOnMatch(c1);
                                        foundMatches = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return foundMatches;
    }

    private boolean matchChildren(MySootClass parent, MySootClass parentSrc){
        if(Config.USE_INNER_CLASS_LINK_INFO){
            //this should not be necessary, but just to be safe
            return false;
        }
        //parent is matched, try to match children
        List<MySootClass> innerClassesTODO = new ArrayList<MySootClass>(parent.innerClasses);
        List<MySootClass> innerClassesRemainingOptions = new ArrayList<MySootClass>(parentSrc.innerClasses);
        return matchClasses(innerClassesTODO, innerClassesRemainingOptions);
    }

    public void findMatches(PackageTree srcTree) {
        long startTime = System.currentTimeMillis();
        setSrcTree(srcTree);
        if(Config.DEBUG){
        	System.out.println("begin findMatches part 1 at " + Stats.getMinutes(Config.startTime) + " min");
        }

        //part1: get sets of classes to match
        List<List<MySootClass>> classesToMatchList = new ArrayList<>();
        List<List<MySootClass>> classesRemainingOptionsList = new ArrayList<>();

        List<MySootClass> unmatchedClasses = new ArrayList<>();
        List<MySootClass> unmatchedClassesSrc = new ArrayList<>();

        List<Package> packagesTODO = new ArrayList<>();
        packagesTODO.add(this);

        while(packagesTODO.size() > 0){
            Package currPack = packagesTODO.remove(0);

            List<MySootClass> classesToMatch = new ArrayList<>();
            List<MySootClass> classesRemainingOptions = new ArrayList<>();

            Package srcPack = currPack == this ? srcTree : srcTree.findPackage(AndroidUtil.getclassPathParts(currPack.getFullPath()));

            classesToMatch.addAll(currPack.classes);
            classesRemainingOptions.addAll(srcPack.classes);

            List<Package> childPackagesTODO = new ArrayList<>(currPack.packages);
            List<Package> childPackagesSrc = new ArrayList<>(srcPack.packages);

            while(childPackagesTODO.size() > 0){
                Package childPack = childPackagesTODO.remove(0);
                Package srcChildPack = srcTree.findPackage(childPack.getFullPath());
                if(srcChildPack == null){
                    childPackagesTODO.addAll(childPack.packages);
                    classesToMatch.addAll(childPack.classes);
                }
                else{
                    //do not search exact matches for obfuscated names, could be different packages
                    if(Obfuscation.isNameMangled(childPack.path)){
                        childPackagesTODO.addAll(childPack.packages);
                        classesToMatch.addAll(childPack.classes);
                        classesRemainingOptions.addAll(srcChildPack.classes);

                        childPackagesSrc.remove(srcChildPack);
                        childPackagesSrc.addAll(srcChildPack.packages);
                    }
                    else {
                        packagesTODO.add(childPack);
                        childPackagesSrc.remove(srcChildPack);
                    }
                }
            }
            //add packages that were not used to unmachted classses
            for(Package p: childPackagesSrc){
                unmatchedClassesSrc.addAll(p.getAllClassesAndInnerClasses());
            }

            if(classesToMatch.size() != 0 && classesRemainingOptions.size() != 0){
                classesToMatchList.add(classesToMatch);
                classesRemainingOptionsList.add(classesRemainingOptions);
            }
            else{
                if (classesToMatch.size() != 0){
                    unmatchedClasses.addAll(classesToMatch);
                }
                if (classesRemainingOptions.size() != 0){
                    unmatchedClassesSrc.addAll(classesRemainingOptions);
                }
            }
        }



        //part 2: match known packages
        if(Config.DEBUG){
        	System.out.println("begin findMatches part 2 at " + Stats.getMinutes(Config.startTime) + " min");
        }
        boolean foundMatches = true;
        int nIterations = 0;
        boolean unmatchedAdded = false;
        while(foundMatches) {
        	if(Config.DEBUG){
        		System.out.println("Matching iteration " + nIterations++);
        	}
            foundMatches = false;
            int currentSize = classesToMatchList.size();
            for(int i = 0; i < currentSize; i++){
            	if(Config.DEBUG){
            		System.out.println("begin iteration " + nIterations + " at list " + i + " at " + Stats.getMinutes(Config.startTime) + " min with " + classesToMatchList.get(i).size() + " classes");
            	}
                List<MySootClass> classesToMatch = new ArrayList<>(classesToMatchList.get(i));
                List<MySootClass> classesRemainingOptions = new ArrayList<>(classesRemainingOptionsList.get(i));
                foundMatches |= matchClasses(classesToMatch, classesRemainingOptions);

                classesToMatchList.set(i, classesToMatch);
                classesRemainingOptionsList.set(i, classesRemainingOptions);

                if(Config.USE_INNER_CLASS_LINK_INFO){
                    //check which classes are matched, and then add children who are not matched yet
                    List<MySootClass> matchedClasses = new ArrayList<>(classesToMatchList.get(i));
                    matchedClasses.removeAll(classesToMatch);

                    for(MySootClass clazz: matchedClasses){
                        List<MySootClass> unmatchedInnerClasses = new ArrayList<>();
                        List<MySootClass> innerClassesFoundInSrc = new ArrayList<>();
                        for(MySootClass innerClazz: clazz.innerClasses){
                            if(innerClazz.matchClass == null) {
                                unmatchedInnerClasses.add(innerClazz);

                            }
                            else {
                                innerClassesFoundInSrc.add(innerClazz.matchClass);
                            }
                        }
                        if(unmatchedInnerClasses.size() > 0) {
                            List<MySootClass> unmatchedInnerClassesSrc = new ArrayList<>(clazz.matchClass.innerClasses);
                            unmatchedInnerClassesSrc.removeAll(innerClassesFoundInSrc);
                            classesToMatchList.add(unmatchedInnerClasses);
                            classesRemainingOptionsList.add(unmatchedInnerClassesSrc);
                        }

                    }
                }

                if(!foundMatches && i == currentSize -1 && !unmatchedAdded){
                	if(Config.DEBUG){
                		System.out.println("No more matches found in normal packages, trying to match the other classes");
                	}
                    //no more matches are found and it is last sublist --> normally stops
                    //last effort where we put together all remaining classes and try to match
                    for(int j = 0; j < classesToMatchList.size(); j++){
                        unmatchedClasses.addAll(classesToMatchList.get(j));
                        unmatchedClassesSrc.addAll(classesRemainingOptionsList.get(j));
                    }
                    classesToMatchList.clear();
                    classesRemainingOptionsList.clear();
                    classesToMatchList.add(unmatchedClasses);
                    classesRemainingOptionsList.add(unmatchedClassesSrc);
                    foundMatches = true;
                    unmatchedAdded = true;
                }
            }
        }

        //for stats, check noOpts, phantom & 2many
        for(MySootClass clazz: this.getAllClassesAndInnerClasses()) {
            if(clazz.matchClass == null){
                if(clazz.hasOptions){
                	if(SootUtil.isSootClassMangled(clazz.sootClass)){
                        clazz.pack.nClassesTooManyOptionsObf++;
                    }
                    else{
                        clazz.pack.nClassesTooManyOptionsNonObf++;
                    }
                    clazz.name += " (2manyOptions)";
                }
                else{
                	if(SootUtil.isSootClassMangled(clazz.sootClass)){
                        clazz.pack.nClassesNoOptionsObf++;
                    }
                    else{
                        clazz.pack.nClassesNoOptionsNonObf++;
                    }
                    clazz.name += " (noOptions)";
                }
            }
            else {
            	if(ClassVerify.isClassByteCodeMatch(clazz.sootClass, this, clazz.matchClass.sootClass, this.srcTree, Config.USE_NAMES)){
            		clazz.name += " (bytecode match)";
            		if(SootUtil.isSootClassMangled(clazz.sootClass)){
                        clazz.pack.nByteCodeMatchObf++;
                    }
                    else{
                        clazz.pack.nByteCodeMatchNonObf++;
                    }
            	}
            	else {
            		if(Config.DEBUG) {
            			System.out.println("[DEBUG] No bytecode match for " + clazz.sootClass.getName() + " and " + clazz.matchClass.sootClass.getName());
            		}
            	}
            }
        }

        Stats.matchingDuration = Stats.getMinutes(startTime);
    }
}

























