package PackageTree;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;

import Config.Config;
import Util.AndroidUtil;
import Util.IO;
import Util.MySootClass;
import Util.SootUtil;
import obfuscation.Obfuscation;
import soot.SootClass;

public class Package {
    public static String newLine = System.getProperty("line.separator");
    public static String tab = "\t";

    public Package parentPackage;
	public ArrayList<Package> packages = new ArrayList<Package>();
	public ArrayList<MySootClass> classes = new ArrayList<MySootClass>();
	public String path;



	//Stats
    public int nClassesNoOptionsNonObf = 0;
    public int nClassesTooManyOptionsNonObf = 0;
    public int nMatchedClasses1optNonObf = 0;
    public int nMatchedOnCGNonObf = 0;
    public int nMatchedOnCGAfterSigNonObf = 0;
    public int nMatchedOnClassSigAndNameNonObf = 0;
    public int nMatchedOnClassSigNonObf = 0;
    public int nMatchedOnSuperNonObf = 0;
    public int nMatchedOnFieldNonObf = 0;
    public int nMatchedOnMethodsNonObf = 0;
    public int nByteCodeMatchNonObf = 0;

    public int nClassesNoOptionsObf = 0;
    public int nClassesTooManyOptionsObf = 0;
    public int nMatchedClasses1optObf = 0;
    public int nMatchedOnCGObf = 0;
    public int nMatchedOnCGAfterSigObf = 0;
    public int nMatchedOnClassSigAndNameObf = 0;
    public int nMatchedOnClassSigObf = 0;
    public int nMatchedOnSuperObf = 0;
    public int nMatchedOnFieldObf = 0;
    public int nMatchedOnMethodsObf = 0;
    public int nByteCodeMatchObf = 0;

	public Package(String path, Package parentPackage) {
		this.path = path;
		this.parentPackage = parentPackage;
	}

	public String getFullPath(){
		List<String> parts = new ArrayList<>();
		Package topPack = this;
		while(topPack != null){
			if(topPack.path != null) parts.add(topPack.path);
			topPack = topPack.parentPackage;
		}
		Collections.reverse(parts);
		return String.join(".", parts);
	}

    public String toString(int level) {
    	String output = path;
    	output += newLine;
    	for(Package pack: packages) {
    		for(int i = 0; i < level + 1; i++) output += tab;
    		output += "" + pack.toString(level + 1);
    	}
    	for(MySootClass mySootClass: classes) {
    		for(int i = 0; i < level + 1; i++) output += tab;
    		output += "- " + mySootClass.toString(level + 1);
    	}
    	return output;
    }

    public List<Integer> getNClasses(){
    	//returns the amount of classes
    	// total, non obufscated, obfuscated

    	List<Integer> output = new ArrayList<>();
    	for(int i=0;i<3;i++)output.add(0);
    	for(Package p: packages){
    		output = Streams.zip(output.stream(), p.getNClasses().stream(), (a, b) -> a+b).collect(Collectors.toList());
    	}
    	List<MySootClass> cs = new ArrayList<>(classes);
    	while(cs.size() > 0){
    		MySootClass c = cs.remove(0);
    		if(SootUtil.isSootClassMangled(c.sootClass)){
    			output.set(2, output.get(2) + 1);
    		}
    		else{
    			output.set(1, output.get(1) + 1);
    		}
    		output.set(0, output.get(0) + 1);
    		cs.addAll(c.innerClasses);

    	}

    	return output;
    }

    public int getByteCodeStatObf() {
    	int sum = nByteCodeMatchObf;
    	for(Package p: packages){
    		sum += p.getByteCodeStatObf();
    	}
    	return sum;
    }

    public int getByteCodeStatNonObf() {
    	int sum = nByteCodeMatchNonObf;
    	for(Package p: packages){
    		sum += p.getByteCodeStatNonObf();
    	}
    	return sum;
    }



    public List<Integer> getMatchStats(){
    	List<Integer> myStats = new ArrayList<>();
    	myStats.add(nMatchedOnClassSigAndNameNonObf);
    	myStats.add(nMatchedOnClassSigNonObf);
    	myStats.add(nMatchedOnCGNonObf);
    	myStats.add(nMatchedOnCGAfterSigNonObf);
    	myStats.add(nMatchedOnSuperNonObf);
    	myStats.add(nMatchedOnFieldNonObf);
    	myStats.add(nMatchedOnMethodsNonObf);
    	myStats.add(nMatchedClasses1optNonObf);
    	myStats.add(nClassesTooManyOptionsNonObf);
    	myStats.add(nClassesNoOptionsNonObf);

    	myStats.add(nMatchedOnClassSigAndNameObf);
    	myStats.add(nMatchedOnClassSigObf);
    	myStats.add(nMatchedOnCGObf);
    	myStats.add(nMatchedOnCGAfterSigObf);
    	myStats.add(nMatchedOnSuperObf);
    	myStats.add(nMatchedOnFieldObf);
    	myStats.add(nMatchedOnMethodsObf);
    	myStats.add(nMatchedClasses1optObf);
    	myStats.add(nClassesTooManyOptionsObf);
    	myStats.add(nClassesNoOptionsObf);

    	for(Package p: packages){
    		myStats = Streams.zip(myStats.stream(), p.getMatchStats().stream(), (a, b) -> a+b).collect(Collectors.toList());
    	}
    	return myStats;
    }

    public ArrayList<MySootClass> getClasses(){
    	ArrayList<MySootClass> output = new ArrayList<MySootClass>();
    	for(Package pack: packages){
    		output.addAll(pack.getClasses());
    	}
    	for(MySootClass clazz: classes){
    		output.add(clazz);
    	}
    	return output;
    }

    public ArrayList<MySootClass> getAllClassesAndInnerClasses(){
    	ArrayList<MySootClass> output = new ArrayList<>();
    	for(Package pack: packages){
    		output.addAll(pack.getAllClassesAndInnerClasses());
    	}
    	for(MySootClass clazz: classes){
    		output.addAll(clazz.getAllClasses());
    	}
    	return output;
    }

    public Package findPackage(String fullPackPath){
		List<String> pathParts = AndroidUtil.getclassPathParts(fullPackPath);
		return findPackage(pathParts);
    }

    public Package findPackage(List<String> parts) {
    	if(parts.size() == 0){
    		throw new RuntimeException("Cannot find package for empty parts!");
    	}
    	String relevantPart = parts.remove(0);
    	for(Package pack: this.packages){
    		if(pack.path.equals(relevantPart)){
    			if(parts.size() == 0){
    				return pack;
    			}
    			else{
    				return pack.findPackage(parts);
    			}
    		}
    	}
    	return null;
    }

    public MySootClass findClass(String fullClassPath){
    	List<String> parts = AndroidUtil.getclassPathParts(fullClassPath);
    	parts.remove(parts.size() - 1); //remove classname
    	Package pack = this;
    	if(parts.size() > 0){
    		pack = findPackage(parts);
    		if(pack == null) return null;
    	}

    	for(MySootClass clazz: pack.getAllClassesAndInnerClasses()){
    		if(clazz.sootClass.getName().equals(fullClassPath)){
    			return clazz;
    		}
    	}
    	return null;
    }

    @Override
    public String toString() {
    	return getFullPath();
    }
}
