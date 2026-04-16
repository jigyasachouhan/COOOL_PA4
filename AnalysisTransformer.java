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
        myPrint("Starting transformation...");
        Boolean isInlinable = true;
        while(isInlinable)
        {
            for(SootClass sc : Scene.v().getApplicationClasses()) {
                for(SootMethod sm : sc.getMethods()){
                    myPrint("Function to be analysed"+sm);
                    Analysis a = new Analysis(new BriefUnitGraph(sm.getActiveBody()),inlinableMap);
                    isInlinable = inlinestuff(a);
                }
            }
        }
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

        // Boolean keep_searching = true;
        // while(keep_searching){
        //     keep_searching = false;
            for(SootMethod m: recursiveMethods){
                Boolean has_self_loop = false;
                Iterator<Edge> it = cg.edgesOutOf(m);
                while (it.hasNext()) {
                    if (it.next().tgt() == m) {
                        has_self_loop = true;
                    }
                }
                if(has_self_loop) continue;
                else{
                    Integer size_m = m.getActiveBody().getUnits().size();
                    if(size_m < Config.INLINE_THRESHOLD){
                        recursiveMethods.remove(m);
                        // keep_searching = true;
                        break;
                    }
                }
            }
        // }

        myPrint(components);
        myPrint("Starting transformation...");
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
                                if(analysis.inlinableMap.containsKey(u) && !recursiveMethods.contains(target) && target.getActiveBody().getUnits().size() < Config.INLINE_THRESHOLD)
                                {
                                    try {
                                        // Inline the call site
                                        SiteInliner.inlineSite(target, stmt, sm);
                                        wasInlinable = true;

                                        myPrint("Inlined: " + target.getSignature() + " at call site: " + stmt);

                                    } catch (Exception e) {
                                        myPrint("Failed to inline: " + target.getSignature());
                                    }
                                }
                                else
                                {
                                    // print reason for not inling: non inlinable or recursive or too big
                                    if(analysis.inlinableMap.containsKey(u))
                                    {
                                        myPrint("Did not Inline : " + target.getSignature() + " because of " + (recursiveMethods.contains(target) ? "recursion" : "size = " + target.getActiveBody().getUnits().size())    );
                                    }
                                    else
                                    {
                                        myPrint("Did not Inline : " + target.getSignature() + " because it is not inlinable at this call siteee " + stmt);
                                    }
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
