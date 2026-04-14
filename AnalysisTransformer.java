import java.util.*;

import soot.*;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.*;

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
        Boolean isInlinable = false;
        do
        {
            for(SootClass sc : Scene.v().getApplicationClasses()) {
                for(SootMethod sm : sc.getMethods()){
                    System.out.println("Function to be analysed"+sm);
                    Analysis a = new Analysis(new BriefUnitGraph(sm.getActiveBody()),inlinableMap);
                    isInlinable = a.isInlinable || isInlinable;
                    inlinestuff(a);
                }
            }
        }while(isInlinable);
    }

    void inlinestuff(Analysis analysis)
    {
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
                                
                                if(analysis.inlinableMap.containsKey(u));
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
