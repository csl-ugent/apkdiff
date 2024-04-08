package Util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import PackageTree.PackageTree;

public class Stats {

	public static long generateCallGraph1Duration = -1;
	public static long generateCallGraph2Duration = -1;

	public static long loadAPKTree1Duration = -1;
	public static long loadAPKTree2Duration = -1;

	public static long matchingDuration = -1;
	public static long totalDuration = -1;


	public static List<String> getData(String apkNames, PackageTree tree1, PackageTree tree2) {
		List<Integer> tree1Classes = tree1.getNClasses();
		List<Integer> tree2Classes = tree2.getNClasses();
		List<Integer> matchData = tree1.getMatchStats();

		List<String> data = new ArrayList<>();
		data.add(apkNames);
		data.add(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()));
		data.add(""+totalDuration);

		data.add(""+generateCallGraph1Duration);
		data.add(""+generateCallGraph2Duration);

		data.add(""+loadAPKTree1Duration);
		data.add(""+loadAPKTree2Duration);

		data.add(""+matchingDuration);

		data.add(""+tree1Classes.get(0));
		data.add(""+tree2Classes.get(0));

		int sumMatched = 0;
		for(int j = 0; j < 8; j++){
			sumMatched += matchData.get(j) + matchData.get(j + matchData.size() / 2);
		}
		data.add("" + sumMatched);
		data.add(""+IO.getPercentage(sumMatched, tree1Classes.get(0)));
		int sumUnmatched = 0;
		for(int j = 8; j < 10; j++){
			sumUnmatched += matchData.get(j) + matchData.get(j + matchData.size() / 2);
		}
		data.add("" + sumUnmatched);
		data.add(""+IO.getPercentage(sumUnmatched, tree1Classes.get(0)));

		int sumByteCodeMatches = tree1.getByteCodeStatNonObf() + tree1.getByteCodeStatObf();
		data.add("" + sumByteCodeMatches);
		data.add(""+IO.getPercentage(sumByteCodeMatches, sumMatched));

		for(int j = 0; j < matchData.size() / 2; j++){
			Integer a = matchData.get(j) + matchData.get(j+matchData.size() / 2);
			data.add(""+a);
			data.add(""+IO.getPercentage(a, tree1Classes.get(0)));
		}

		data.add("");

		data.add(""+tree1Classes.get(1));
		data.add(""+tree2Classes.get(1));

		int sumMatchedNonObf = 0;
		for(int j = 0; j < 8; j++){
			sumMatchedNonObf += matchData.get(j);
		}
		data.add("" + sumMatchedNonObf);
		data.add(""+IO.getPercentage(sumMatchedNonObf, tree1Classes.get(1)));
		int sumUnmatchedNonObf = 0;
		for(int j = 8; j < 10; j++){
			sumUnmatchedNonObf += matchData.get(j);
		}
		data.add("" + sumUnmatchedNonObf);
		data.add(""+IO.getPercentage(sumUnmatchedNonObf, tree1Classes.get(1)));

		data.add("" + tree1.getByteCodeStatNonObf());
		data.add(""+IO.getPercentage(tree1.getByteCodeStatNonObf(), sumMatchedNonObf));

		for(int j = 0; j < matchData.size() / 2; j++){
			data.add(""+matchData.get(j));
			data.add(""+IO.getPercentage(matchData.get(j), tree1Classes.get(1)));
		}

		data.add("");

		data.add(""+tree1Classes.get(2));
		data.add(""+tree2Classes.get(2));

		int sumMatchedObf = 0;
		for(int j = 0; j < 8; j++){
			sumMatchedObf += matchData.get(j + matchData.size() / 2);
		}
		data.add("" + sumMatchedObf);
		data.add(""+IO.getPercentage(sumMatchedObf, tree1Classes.get(2)));
		int sumUnmatchedObf = 0;
		for(int j = 8; j < 10; j++){
			sumUnmatchedObf += matchData.get(j + matchData.size() / 2);
		}
		data.add("" + sumUnmatchedObf);
		data.add(""+IO.getPercentage(sumUnmatchedObf, tree1Classes.get(2)));

		data.add("" + tree1.getByteCodeStatObf());
		data.add(""+IO.getPercentage(tree1.getByteCodeStatObf(), sumMatchedObf));

		for(int j = matchData.size() / 2; j < matchData.size(); j++){
			data.add(""+matchData.get(j));
			data.add(""+IO.getPercentage(matchData.get(j), tree1Classes.get(2)));
		}

		return data;
	}

    public static List<String> getHeaders(){
    	List<String> headers = new ArrayList<>();

    	headers.add("apkNames");
		headers.add("time");
		headers.add("totalDuration");

		headers.add("generateCallGraph1Duration");
		headers.add("generateCallGraph2Duration");

		headers.add("loadAPKTree1Duration");
		headers.add("loadAPKTree2Duration");

		headers.add("matchingDuration");

    	headers.add("#classes APK1");
    	headers.add("#classes APK2");

    	headers.add("#matched");
    	headers.add("%matched");

    	headers.add("#unmatched");
    	headers.add("%unmatched");

    	headers.add("#bytecode match");
    	headers.add("%bytecode match");

    	headers.add("#ClassSigAndName");
    	headers.add("%ClassSigAndName");

    	headers.add("#ClassSig");
    	headers.add("%ClassSig");

    	headers.add("#CG");
    	headers.add("%CG");

    	headers.add("#CGAfterSig");
    	headers.add("%CGAfterSig");

    	headers.add("#super");
    	headers.add("%super");

    	headers.add("#field");
    	headers.add("%field");

    	headers.add("#method");
    	headers.add("%method");

    	headers.add("#1opt");
    	headers.add("%1opt");

    	headers.add("#2many");
    	headers.add("%2many");

    	headers.add("#noOpts");
    	headers.add("%noOpts");

    	headers.add("");

    	headers.add("#classes APK1 non-obfuscated");
    	headers.add("#classes APK2 non-obfuscated");

    	headers.add("#matched non-obfuscated");
    	headers.add("%matched non-obfuscated");

    	headers.add("#unmatched non-obfuscated");
    	headers.add("%unmatched non-obfuscated");

    	headers.add("#bytecode match non-obfuscated");
    	headers.add("%bytecode match non-obfuscated");

    	headers.add("#ClassSigAndName non-obfuscated");
    	headers.add("%ClassSigAndName non-obfuscated");

    	headers.add("#ClassSig non-obfuscated");
    	headers.add("%ClassSig non-obfuscated");

    	headers.add("#CG non-obfuscated");
    	headers.add("%CG non-obfuscated");

    	headers.add("#CGAfterSig non-obfuscated");
    	headers.add("%CGAfterSig non-obfuscated");

    	headers.add("#super non-obfuscated");
    	headers.add("%super non-obfuscated");

    	headers.add("#field non-obfuscated");
    	headers.add("%field non-obfuscated");

    	headers.add("#method non-obfuscated");
    	headers.add("%method non-obfuscated");

    	headers.add("#1opt non-obfuscated");
    	headers.add("%1opt non-obfuscated");

    	headers.add("#2many non-obfuscated");
    	headers.add("%2many non-obfuscated");

    	headers.add("#noOpts non-obfuscated");
    	headers.add("%noOpts non-obfuscated");

    	headers.add("");

    	headers.add("#classes APK1 obfuscated");
    	headers.add("#classes APK2 obfuscated");

    	headers.add("#matched obfuscated");
    	headers.add("%matched obfuscated");

    	headers.add("#unmatched obfuscated");
    	headers.add("%unmatched obfuscated");

    	headers.add("#bytecode match obfuscated");
    	headers.add("%bytecode match obfuscated");

    	headers.add("#ClassSigAndName obfuscated");
    	headers.add("%ClassSigAndName obfuscated");

    	headers.add("#ClassSig obfuscated");
    	headers.add("%ClassSig obfuscated");

    	headers.add("#CG obfuscated");
    	headers.add("%CG obfuscated");

    	headers.add("#CGAfterSig obfuscated");
    	headers.add("%CGAfterSig obfuscated");

    	headers.add("#super obfuscated");
    	headers.add("%super obfuscated");

    	headers.add("#field obfuscated");
    	headers.add("%field obfuscated");

    	headers.add("#method obfuscated");
    	headers.add("%method obfuscated");

    	headers.add("#1opt obfuscated");
    	headers.add("%1opt obfuscated");

    	headers.add("#2many obfuscated");
    	headers.add("%2many obfuscated");

    	headers.add("#noOpts obfuscated");
    	headers.add("%noOpts obfuscated");


    	return headers;
    }


	public static long getMinutes(long startTime){
		long endTime = System.currentTimeMillis();
		long minutes = TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
		return minutes;
	}


}
