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
import soot.util.*;

public class InlinerTransform extends SceneTransformer {

    static CallGraph cg;
    static AnalysisTransformer analysis;


    public void myPrint(Object toPrint)
    {
        System.out.println(toPrint.toString());
    }

    public InlinerTransform(AnalysisTransformer analysis)
    {
        InlinerTransform.analysis = analysis;
        
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
    
    }
}
