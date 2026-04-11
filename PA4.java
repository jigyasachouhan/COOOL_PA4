import soot.*;
import soot.options.Options;

public class PA4 {
    public static void main(String[] args) {
        String classPath = "./testcases/" + args[0]; //do not change this, as evaluation would have testcases under this directory
        String output_Format = "class";
        Options.v().set_keep_line_number(true);

        SceneTransformer sceneTransformer = new AnalysisTransformer();
        SceneTransformer inliner = new InlinerTransform((AnalysisTransformer)sceneTransformer);

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.dfa", sceneTransformer));
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.dfa iansd", inliner));

        //Set up arguments for Soot
        String[] sootArgs = {
            "-cp", classPath, 
            "-pp", // sets the class path for Soot
            "-w",
            "-app",
            "-allow-phantom-refs",
            "-no-bodies-for-excluded",
            "-exclude", "java.*",
            "-exclude", "sun.*",
            "-exclude", "javax.*",
            "-exclude", "com.sun.*",
            "-exclude", "jdk.*",
            // "-f", "J",
            "-t", "1",
            "-main-class", "Test",	// specify the main class
            "-process-dir", classPath,
            "-output-format", output_Format,
            "-output-dir", "sootOutput",
        };
        // Call Soot's main method with arguments
        soot.Main.main(sootArgs);

        //Running the original and transformed classes to compare results
        soot.G.reset();
        Options.v().set_keep_line_number(true);

        //Set up arguments for Soot
        String[] OriginalArgs = {
            "-cp", classPath, 
            "-pp", // sets the class path for Soot
            "-w",
            "-app",
            "-allow-phantom-refs",
            "-no-bodies-for-excluded",
            "-exclude", "java.*",
            "-exclude", "sun.*",
            "-exclude", "javax.*",
            "-exclude", "com.sun.*",
            "-exclude", "jdk.*",
            // "-f", "J",
            "-t", "1",
            "-main-class", "Test",	// specify the main class
            "-process-dir", classPath,
            "-output-format", output_Format,
            "-output-dir", "OriginalOutput",
        };
        // Call Soot's main method with arguments
        soot.Main.main(OriginalArgs);
    }
}
