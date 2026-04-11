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
        cg = Scene.v().getCallGraph();
        System.out.println("Starting transformation...");
        for(SootClass sc : Scene.v().getApplicationClasses()) {
            for(SootMethod sm : sc.getMethods()) {
                Chain<Unit> units = sm.retrieveActiveBody().getUnits();
                Iterator<Unit> unitIt = units.snapshotIterator();
                while(unitIt.hasNext()) {
                    Unit u = unitIt.next();
                    if (u instanceof Stmt) {
                        Stmt stmt = (Stmt) u;

                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr ie = stmt.getInvokeExpr();

                            if (ie instanceof VirtualInvokeExpr) {
                                VirtualInvokeExpr vie = (VirtualInvokeExpr) ie;

                                // Base object (the receiver)
                                Local base = (Local) vie.getBase();

                                // Method being called
                                SootMethod method = vie.getMethod();

                                // Arguments
                                List<Value> args = vie.getArgs();
                                SootMethod target = vie.getMethod();


                                // Skip unsafe cases
                                if (!target.isConcrete()) continue;
                                if (target.getDeclaringClass().isInterface()) continue;
                                
                                if(InlinerTransform.analysis.inlinableMap.containsKey(u));
                                {
                                    try {
                                        // Inline the call site
                                        SiteInliner.inlineSite(target, stmt, sm);

                                        System.out.println("Inlined: " + target.getSignature());

                                    } catch (Exception e) {
                                        System.out.println("Failed to inline: " + target.getSignature());
                                    }
                                }
                            }
                        }
                    }
            }
            }
    }
    }
}
