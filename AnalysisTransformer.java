import java.util.*;

import soot.*;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import soot.jimple.toolkits.annotation.purity.DirectedCallGraph;
import soot.jimple.toolkits.annotation.purity.SootMethodFilter;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.invoke.SiteInliner;
import soot.util.*;
import soot.jimple.*;

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
                    isInlinable = inlinestuff(a);
                }
            }
        }while(isInlinable);
    }

    boolean inlinestuff(Analysis analysis)
    {
        CallGraph cg = Scene.v().getCallGraph();
        Boolean wasInlinable = false;
        Set<SootMethod> recursiveMethods = new HashSet<>();
        SootMethodFilter filter = new SootMethodFilter() {
            @Override
            public boolean want(SootMethod m)
            {
                if(m.isJavaLibraryMethod())
                    return false;
                return true;
            }
        };
        DirectedGraph<SootMethod> graph = new DirectedCallGraph(cg,filter,Scene.v().getEntryPoints().iterator(),false);
        StronglyConnectedComponentsFast<SootMethod> scc = new StronglyConnectedComponentsFast<>(graph);

        List<List<SootMethod>> components = scc.getComponents();
        for (List<SootMethod> comp : components) {
            if (comp.size() > 1) {
                // all methods here are recursive
                for(SootMethod m : comp)
                {
                    recursiveMethods.add(m);
                }
            } else {
                SootMethod m = comp.get(0);

                // check self-loop
                Iterator<Edge> it = cg.edgesOutOf(m);
                while (it.hasNext()) {
                    if (it.next().tgt() == m) {
                        recursiveMethods.add(m);
                    }
                }
            }
        }
        System.out.println(components);
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
                                SootMethod target = vie.getMethod();

                                // Skip unsafe cases
                                if (!target.isConcrete()) continue;
                                if (target.getDeclaringClass().isInterface()) continue;
                                
                                if(analysis.inlinableMap.containsKey(u) && !recursiveMethods.contains(target) && target.getActiveBody().getUnits().size() < 30)
                                {
                                    try {
                                        // Inline the call site
                                        SiteInliner.inlineSite(target, stmt, sm);
                                        wasInlinable = true;

                                        System.out.println("Inlined: " + target.getSignature());

                                    } catch (Exception e) {
                                        System.out.println("Failed to inline: " + target.getSignature());
                                    }
                                }
                                else
                                {
                                    System.out.println("Did not Inline : " + target.getSignature());
                                }
                            }
                        }
                    }
            }
            }
        }
        return wasInlinable;
    }
}
