import java.util.Map;

import soot.*;
import soot.jimple.toolkits.callgraph.UnreachableMethodTransformer;
import soot.jimple.toolkits.scalar.DeadAssignmentEliminator;
import soot.options.Options;
import soot.toolkits.scalar.UnusedLocalEliminator;
import soot.jimple.toolkits.scalar.UnreachableCodeEliminator;
import java.time.Instant;

public class PA4 {
    public static void main(String[] args) {
        soot.G.reset();
        String classPath = "../tests/" + args[0]; 
        Boolean transform = Boolean.parseBoolean(args[1]);
        String output_Format = "class";
        Options.v().set_keep_line_number(true);

        if(transform) {
            SceneTransformer sceneTransformer = new AnalysisTransformer();

            PackManager.v().getPack("wjtp").add(new Transform("wjtp.analysis", sceneTransformer));
            PackManager.v().getPack("jtp").add(new Transform("jtp.DCE", new BodyTransformer() {
                @Override
                protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
                    DeadAssignmentEliminator.v().transform(b);
                    UnusedLocalEliminator.v().transform(b);
                    UnreachableCodeEliminator.v().transform(b);
                }
            }));


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
                "-output-dir", "sootOutput_" + args[0] + "_" + args[2],
            };
            // Call Soot's main method with arguments
            soot.Main.main(sootArgs);
        }

    else{
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
            "-output-dir", "OriginalOutput_" + args[0] + "_" + args[2],
        };
        // Call Soot's main method with arguments
        soot.Main.main(OriginalArgs);
    }
}
}
