import java.util.*;
import soot.*;
import soot.toolkits.graph.*;

public class AnalysisTransformer extends SceneTransformer {

    Map<Unit, Boolean> inlinableMap;

    public AnalysisTransformer()
    {
        inlinableMap = new HashMap<>();
    }

    public void myPrint(Object toPrint)
    {
        System.out.println(toPrint.toString());
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        System.out.println("Starting transformation...");
        for(SootClass sc : Scene.v().getApplicationClasses()) {
            for(SootMethod sm : sc.getMethods()){
                System.out.println("Function to be analysed"+sm);
                new Analysis(new BriefUnitGraph(sm.getActiveBody()),inlinableMap);
                
            }
    }
    }
}
