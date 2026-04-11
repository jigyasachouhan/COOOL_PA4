import java.util.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.invoke.SiteInliner;

import soot.jimple.internal.JNewExpr;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.InstanceInvokeExpr;
import soot.util.*;
import soot.jimple.Jimple;

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
                Analysis a = new Analysis(new BriefUnitGraph(sm.getActiveBody()),inlinableMap);
                
            }
    }
    }
}
