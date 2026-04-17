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
    Map<SootMethod, SootMethod> staticisedMetods;
    Map<SootMethod, Integer> incomingMap;

    public AnalysisTransformer()
    {
        inlinableMap = new HashMap<>();
        staticisedMetods = new HashMap<>();
        incomingMap = new HashMap<>();
    }

    public void myPrint(Object toPrint)
    {
        System.out.println(toPrint.toString());
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        cg = Scene.v().getCallGraph();
        Map<SootMethod, Integer> incomingMap = new HashMap<>();

        // initialize
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            for (SootMethod sm : sc.getMethods()) {
                incomingMap.put(sm, 0);
            }
        }

        for (SootClass sc : Scene.v().getApplicationClasses()) {
            for (SootMethod sm : sc.getMethods()) {

                if (!sm.hasActiveBody()) continue;

                for (Unit u : sm.getActiveBody().getUnits()) {
                    if (!(u instanceof Stmt)) continue;

                    Stmt stmt = (Stmt) u;

                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr ie = stmt.getInvokeExpr();

                        // all possible targets of this call site
                        List<SootMethod> targets = new ArrayList<>();
                        if (ie instanceof VirtualInvokeExpr) {
                            VirtualInvokeExpr vie = (VirtualInvokeExpr) ie;
                            for (Iterator<Edge> it = cg.edgesOutOf(sm); it.hasNext();) {
                                Edge e = it.next();
                                if (e.src() == sm && e.srcUnit() == stmt) {
                                    targets.add(e.tgt());
                                }                            }
                        } else {
                            targets.add(ie.getMethod());
                        }

                        // increment count
                        for (SootMethod target : targets) {
                            incomingMap.put(target,
                                incomingMap.getOrDefault(target, 0) + 1);
                        }
                    }
                }
            }
        }

        myPrint("Starting Analysis...");
        for(SootClass sc : Scene.v().getApplicationClasses()) {
            for(SootMethod sm : sc.getMethods()){
                myPrint("Function to be analysed for the first time "+sm);
                new Analysis(new BriefUnitGraph(sm.getActiveBody()),inlinableMap);
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
        Set<SootMethod> recursiveMethods = new LinkedHashSet<>();
       
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
        
        for(List<SootMethod> comp : components) {
            comp.sort((c1, c2) -> {
                // 1. sort by size of function (smaller first)
                int sizeCmp = Integer.compare(c1.getActiveBody().getUnits().size(), c2.getActiveBody().getUnits().size());
                if (sizeCmp != 0) return sizeCmp;
                // 2. if sizes are equal, sort by total incoming edges (descending)
                int score1 = incomingMap.getOrDefault(c1, 0);
                int score2 = incomingMap.getOrDefault(c2, 0);
                if(score1 != score2) return Integer.compare(score2, score1); // reverse
                // 3. if still equal, sort by method signature (lexicographically)
                String sig1 = c1.getSignature();
                String sig2 = c2.getSignature();
                return sig1.compareTo(sig2);
            });
        }

        components.sort((comp1, comp2) -> {
            // 1. sort by size of biggest function in the component (bigger first)
            int size1 = comp1.get(0).getActiveBody().getUnits().size();
            int size2 = comp2.get(0).getActiveBody().getUnits().size();
            if (size1 != size2) return Integer.compare(size2, size1);
            // 2. if sizes are equal, sort by total incoming edges of the biggest function (descending)
            int score1 = incomingMap.getOrDefault(comp1.get(0), 0);
            int score2 = incomingMap.getOrDefault(comp2.get(0), 0);
            if(score1 != score2) return Integer.compare(score2, score1);
            // 3. if still equal, sort by method signature of the biggest function (lexicographically)
            String sig1 = comp1.get(0).getSignature();
            String sig2 = comp2.get(0).getSignature();
            return sig1.compareTo(sig2);
        });

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

        myPrint("Recursive methods: " + recursiveMethods);
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
        for(SootClass sc : new ArrayList<>(Scene.v().getApplicationClasses())) {
            List<SootMethod> methods = new ArrayList<>(sc.getMethods());
            for(SootMethod sm : methods) {
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

                                        System.out.println("Inlined: " + target.getSignature() + " in method: " + sm.getSignature());

                                    } catch (Exception e) {
                                        myPrint("Failed to inline: " + target.getSignature());
                                    }
                                }
                                else if (analysis.inlinableMap.containsKey(u) && !recursiveMethods.contains(target) && target.getActiveBody().getUnits().size() >= Config.INLINE_THRESHOLD)
                                {
                                    myPrint("Gonna try static-ising");
                                    SootMethod oldMethod = target;

                                    List<Type> newParams = new ArrayList<>();
                                    newParams.add(oldMethod.getDeclaringClass().getType()); // this param
                                    newParams.addAll(oldMethod.getParameterTypes());

                                    SootMethod newMethod;
                                    if(staticisedMetods.containsKey(oldMethod))
                                    {
                                        newMethod = staticisedMetods.get(oldMethod);
                                        myPrint("Method already staticised: " + oldMethod.getSignature());
                                    }
                                    else{
                                        newMethod = new SootMethod(
                                            oldMethod.getName() + "_static",
                                            newParams,
                                            oldMethod.getReturnType(),
                                            Modifier.STATIC | Modifier.PUBLIC
                                        );

                                        oldMethod.getDeclaringClass().addMethod(newMethod);

                                        Body oldBody = oldMethod.retrieveActiveBody();
                                        Body newBody = Jimple.v().newBody(newMethod);
                                        newMethod.setActiveBody(newBody);

                                        newBody.importBodyContentsFrom(oldBody);

                                        for (Unit u_newbody : newBody.getUnits()) {
                                            if (u_newbody instanceof IdentityStmt) {
                                                IdentityStmt id = (IdentityStmt) u_newbody;

                                                if (id.getRightOp() instanceof ThisRef) {
                                                    id.setRightOp(Jimple.v().newParameterRef(
                                                        oldMethod.getDeclaringClass().getType(), -1
                                                    ));
                                                }
                                            }
                                        }

                                        for (Unit u_newbody : newBody.getUnits()) {
                                            if (u_newbody instanceof IdentityStmt) {
                                                IdentityStmt id = (IdentityStmt) u_newbody;

                                                if (id.getRightOp() instanceof ParameterRef) {
                                                    ParameterRef pr = (ParameterRef) id.getRightOp();

                                                    id.setRightOp(Jimple.v().newParameterRef(
                                                        pr.getType(),
                                                        pr.getIndex() + 1
                                                    ));
                                                }
                                            }
                                        }
                                        staticisedMetods.put(oldMethod, newMethod);
                                    }
                                
                                    Local base = (Local) vie.getBase();
                                    List<Value> args = new ArrayList<>(vie.getArgs());
                                    args.add(0, base);
                                    StaticInvokeExpr sie = Jimple.v().newStaticInvokeExpr(
                                        newMethod.makeRef(),
                                        args
                                    );

                                    if (stmt instanceof AssignStmt) {
                                        AssignStmt assign = (AssignStmt) stmt;
                                        assign.setRightOp(sie);
                                    } else if (stmt instanceof InvokeStmt) {
                                        InvokeStmt inv = (InvokeStmt) stmt;
                                        inv.setInvokeExpr(sie);
                                    }

                                    System.out.println("Method " + target.getSignature() + " is too big, converted to static call: " + newMethod.getSignature() + " in method: " + sm.getSignature());
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
