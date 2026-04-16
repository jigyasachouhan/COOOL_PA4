import java.util.*;

import polyglot.ast.Call;
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
    CallGraph cg;

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
        cg = Scene.v().getCallGraph();
        myPrint("Starting Analysis...");
        for(SootClass sc : Scene.v().getApplicationClasses()) {
            for(SootMethod sm : sc.getMethods()){
                myPrint("Function to be analysed for the first time "+sm);
                Analysis a = new Analysis(new BriefUnitGraph(sm.getActiveBody()),inlinableMap);
            }
        }
        Boolean isInlinable = true;
        while(isInlinable)
        {
            Boolean smthInlinedThistime = false;
            for(SootClass sc : Scene.v().getApplicationClasses()) {
                for(SootMethod sm : sc.getMethods()){
                    myPrint("Function to be analysed"+sm);
                    Analysis a = new Analysis(new BriefUnitGraph(sm.getActiveBody()),inlinableMap);
                    smthInlinedThistime |= inlinestuff(a);
                }
            }
            isInlinable = smthInlinedThistime;
        }
    }

    boolean inlinestuff(Analysis analysis)
    {
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
        myPrint("Components found: " + components);
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

        for(SootMethod m: recursiveMethods){
            myPrint("Recursive method being checked for loop: " + m.getSignature());
            Boolean has_self_loop = false;
            Iterator<Edge> it = cg.edgesOutOf(m);
            while (it.hasNext()) {
                if (it.next().tgt() == m) {
                    has_self_loop = true;
                }
            }
            if(has_self_loop){
                myPrint("Self looped method : " + m.getSignature());
            }
            else{
                myPrint("Removed method : " + m.getSignature());
                recursiveMethods.remove(m);
                break;
            }
        }

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
                                        
                                        // Remove edge from sm to target in call graph
                                        List<Edge> edgesToRemove = new ArrayList<>();
                                        Iterator<Edge> edgeIt = cg.edgesOutOf(sm);
                                        while(edgeIt.hasNext()) {
                                            Edge e = edgeIt.next();
                                            if(e.tgt() == target) {
                                                edgesToRemove.add(e);
                                            }
                                        }
                                        for(Edge e : edgesToRemove) {
                                            cg.removeEdge(e);
                                        }

                                        // Add edges from sm to all methods called by target
                                        Iterator<Edge> newEdgesIt = cg.edgesOutOf(target);
                                        while(newEdgesIt.hasNext()) {
                                            Edge e = newEdgesIt.next();
                                            Edge newEdge = new Edge(sm, stmt, e.tgt(), e.kind());
                                            cg.addEdge(newEdge);
                                        }

                                        myPrint("Inlined: " + target.getSignature() + " at call site: " + stmt + " in method: " + sm.getSignature());

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
                                        myPrint("Did not Inline : " + target.getSignature() + " because it is not inlinable at this call siteee " + stmt + " in method: " + sm.getSignature() + " with reason : " + (analysis.inlinableMap.containsKey(u) ? "non inlinable" : "not analyzed yet") );
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
