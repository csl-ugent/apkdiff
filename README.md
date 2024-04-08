# ApkDiff

This repository contains the code for ApkDiff, which is explained in more detail in the associated paper [ApkDiff: Matching Android App Versions Based on Class Structure](https://bartcoppens.be/research/deghein2022_apkdiff_checkmate.pdf) by Robbe De Ghein, Bert Abrath, Bjorn De Sutter, Bart Coppens, published at CheckMATE 2022. The code was written by Robbe De Ghein.

## Usage

Basic usage is as follows:

```bash
java -Xmx20g -Xss512m -cp ./ObfuscationAnalytics-0.0.1-SNAPSHOT-jar-with-dependencies.jar Main -o ./outputdirectory -p /androidPlatformDirectory -s /classFilesDirectory -a ./apkFile_v1.apk -m ./apkFile_v2.apk
```

* `/androidPlatformDirectory` is the directory being passed to infoflow's `setAndroidPlatformDir`; it should contain a set of subdirectories `android-<versionnumber>` with `android.jar` and others in them.
* `/classFilesDirectory` points to a directory with your Java classpath (i.e., the a directory with subdirectory structure `java/lang/Object.class` etc.

This will output to `./outputdirectory/apkFile_v1_vs_apkFile_v2`:

* A `log_apkFile_v1_vs_apkFile_v2.txt` log file with the progress
* A `apkFile_v1_vs_apkFile_v2_treeStats.csv` file with statistics about the matches
* A `apkFile_v1_vs_apkFile_v2_matches.csv` file with per line a class of the v1 APK and either a class of the v2 APK or "no match"
* A `tree_apkFile_v1_vs_apkFile_v2.txt` file with the matched class structure, and for each class the match (or lack of match) the matching strategy or reasons of no matching. The "(bytecode match)" indicates if the bytecode was equivalent (this is not used for the matching itself).

Running the Main class without any arguments prints the available options which can also be used to enable/disable using certain types of information for matching; for the sake of completeness, here they are as well:

```
  --androidPlatformsPath [-p] (a string; default: "")
    Full path to the android sdk platform dir, if absent take windows default: 
    /$user/AppData/Local/android/Sdk/platforms.
  --apkMatchPath [-m] (a string; default: "")
    Path to the .apk file to match with.
  --apkPath [-a] (a string; default: "")
    Path to the .apk file.
  --Call Graph Timeout [-t] (an integer; default: "60")
    Maximum time spent on calculating call graph in minutes.
  --Debug [-d] (a string; default: "false")
    Print out debugging information
  --incomingCallGraphEdges [-i] (a string; default: "false")
    Not only use outgoing call graph edges, but also the incoming ones
  --onlyPublic [-l] (a string; default: "false")
    Only use public fields and methods
  --outputDir [-o] (a string; default: "output")
    Output directory. Creates it if non-existant. Default is /output.
  --srcCLassPath [-s] (a string; default: "")
    Path to the directory where source (.class) files can be found. E.g. the 
    dir where the androidx dir is.
  --useAccessModifiers [-y] (a string; default: "true")
    Use access modifiers: public/private/protected on classes/methods/fields
  --Use Call Graph [-g] (a string; default: "true")
    Use call graph edges as information
  --useFinalModifier [-f] (a string; default: "false")
    Use the final modifier
  --Use Names [-n] (a string; default: "true")
    Use Identifier information if not obviously renamed
```
