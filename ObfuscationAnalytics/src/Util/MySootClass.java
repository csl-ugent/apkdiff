package Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import PackageTree.Package;
import PackageTree.PackageTree.MatchMethod;
import soot.SootClass;

public class MySootClass {
	public String name;
	public boolean isMangled;
	public SootClass sootClass;
	public List<MySootInnerClass> innerClasses = new ArrayList<MySootInnerClass>();
	public int nLinksToObfuscatedClasses = 0;
	public int nLinks = 0;
	public Package pack;

	public MySootClass matchClass = null;
	public MatchMethod matchMethod = null;

	//used for stats to know wheter options were found or not
	public boolean hasOptions = false;


    public static String newLine = System.getProperty("line.separator");
    public static String tab = "\t";

	public MySootClass(Package pack, SootClass sootClass, String name) {
		isMangled = false;
		this.sootClass = sootClass;
		this.name = name;
		this.pack = pack;
	}

	public SootClass getMostOuterClass(){
		SootClass curr = sootClass;
		while(curr.hasOuterClass()) curr = curr.getOuterClass();
		return curr;
	}

	public void addInnerClass(SootClass sootClass){
		ArrayList<String> parts = new ArrayList<String>(Arrays.asList(sootClass.getShortName().split("\\$")));
		parts.remove(0);//most outerclass name
		addInnerClass(sootClass, parts);
	}

	public void addInnerClass(SootClass sootClass, List<String> remainingParts){
		if(remainingParts.size() == 0) return;
		String partName = remainingParts.remove(0);
		boolean alreadyAdded = false;
		for(MySootInnerClass innerClass: innerClasses){
			if(innerClass.name.equals(partName)){
				alreadyAdded = true;
				innerClass.addInnerClass(sootClass, remainingParts);
			}
		}
		if(!alreadyAdded){
			innerClasses.add(new MySootInnerClass(pack, sootClass, partName));
		}

	}

    public ArrayList<MySootClass> getAllClasses(){
    	ArrayList<MySootClass> output = new ArrayList<MySootClass>();
    	output.add(this);
    	for(MySootInnerClass clazz: innerClasses){
    		output.addAll(clazz.getAllClasses());
    	}
    	return output;
    }



	public String toString(int level) {
		String output = "";
		//output += String.format("%s: %d/%d (%d%%)", name, nLinksToObfuscatedClasses, nLinks, (int) Math.round((double)nLinksToObfuscatedClasses/(double)nLinks*100));
		output += String.format("%s", name);
		output += newLine;
		for(MySootInnerClass innerClass: innerClasses){
			for(int i = 0; i < level + 1; i++) output += tab;
			output += "$ " + innerClass.toString(level + 1);
		}
		return output;
	}

	@Override
	public String toString() {
		return sootClass.toString();
	}
}
