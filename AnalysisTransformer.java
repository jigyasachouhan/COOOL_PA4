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
    Set<SootMethod> allMethods;

    public AnalysisTransformer()
    {
        inlinableMap = new HashMap<>();
        staticisedMetods = new HashMap<>();
        incomingMap = new HashMap<>();
        allMethods = new LinkedHashSet<>();
    }

    public void myPrint(Object toPrint)
    {
        // System.out.println(toPrint.toString());
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        cg = Scene.v().getCallGraph();
        Map<SootMethod, Integer> incomingMap = new HashMap<>();

        // initialize
        for (SootClass sc : Scene.v().getApplicationClasses()) {
            for (SootMethod sm : sc.getMethods()) {
                incomingMap.put(sm, 0);
                allMethods.add(sm);
            }
        }

        for (SootMethod sm : allMethods) {
            if (!sm.hasActiveBody()) continue;

            for (Unit u : sm.getActiveBody().getUnits()) {
                if (!(u instanceof Stmt)) continue;

                Stmt stmt = (Stmt) u;

                if (stmt.containsInvokeExpr()) {
                    InvokeExpr ie = stmt.getInvokeExpr();

                    // all possible targets of this call site
                    List<SootMethod> targets = new ArrayList<>();
                    if (ie instanceof VirtualInvokeExpr) {
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

        myPrint("Starting Analysis...");
        for(SootMethod sm : allMethods){
            myPrint("Function to be analysed for the first time "+sm);
            new Analysis(new BriefUnitGraph(sm.getActiveBody()),inlinableMap);
        }
        Boolean isInlinable = true;
        while(isInlinable)
        {
            Boolean smthInlinedThistime = false;
            for(SootMethod sm : new LinkedHashSet<>(allMethods)){
                myPrint("Function to be analysed"+sm);
                Analysis a = new Analysis(new BriefUnitGraph(sm.getActiveBody()),inlinableMap);
                smthInlinedThistime |= inlinestuff(a);
            }
            isInlinable = smthInlinedThistime;

            Set<SootMethod> reachable = new LinkedHashSet<>();
            Queue<SootMethod> worklist = new LinkedList<>();

            // Get main method
            SootMethod mainMethod = Scene.v().getMainMethod();

            worklist.add(mainMethod);
            reachable.add(mainMethod);

            while (!worklist.isEmpty()) {
                SootMethod current = worklist.poll();

                List<SootMethod> targets = new ArrayList<>();
                Iterator<Unit> unitiIterator= current.getActiveBody().getUnits().snapshotIterator();
                while(unitiIterator.hasNext()) {
                    Unit u = unitiIterator.next();
                    if (!(u instanceof Stmt)) continue;

                    Stmt stmt = (Stmt) u;

                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr ie = stmt.getInvokeExpr();

                        if (ie instanceof VirtualInvokeExpr) {
                            SootMethod tgt=null;
                            for (Iterator<Edge> it = cg.edgesOutOf(current); it.hasNext();) {
                                Edge e = it.next();
                                if (e.src() == current && e.srcUnit() == stmt) {
                                    tgt = e.tgt();
                                    if(!ie.getMethod().isJavaLibraryMethod())   
                                        targets.add(tgt);
                                }
                            }
                                
                        } else {
                            if(!ie.getMethod().isJavaLibraryMethod())   
                                targets.add(ie.getMethod());
                        }
                    }
                }
                for(SootMethod target : targets) {

                    if (!reachable.contains(target) && target.isConcrete()) {
                        reachable.add(target);
                        worklist.add(target);
                    }
                }
            }


            for (SootMethod sm : new LinkedHashSet<>(allMethods)) {
                if (!reachable.contains(sm) && !sm.isMain() && sm.isConcrete()) {
                    System.out.println("Removing unreachable method: " + sm.getSignature());

                    sm.getDeclaringClass().removeMethod(sm);
                    sm.setActiveBody(null);
                    allMethods.remove(sm);
                }
            }
        }

    }

    boolean inlinestuff(Analysis analysis)
    {
        Boolean wasInlinable = false;
        
        myPrint("Starting transformation...");
        for(SootMethod sm : new LinkedHashSet<>(allMethods)){
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
                                if(analysis.inlinableMap.containsKey(u) && target.getActiveBody().getUnits().size() < Config.INLINE_THRESHOLD)
                                {
                                    try {
                                        // Inline the call site
                                        SiteInliner.inlineSite(target, stmt, sm);
                                        wasInlinable = true;
                                        

                                        System.out.println("Inlined: " + target.getSignature() + " in method: " + sm.getSignature());

                                    } catch (Exception e) {
                                        myPrint("Failed to inline: " + target.getSignature());
                                    }
                                }
                                else if (analysis.inlinableMap.containsKey(u) && target.getActiveBody().getUnits().size() >= Config.INLINE_THRESHOLD)
                                {
                                    myPrint("Gonna try static-ising");
                                    wasInlinable = true;

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
                                        allMethods.add(newMethod);
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

                                    System.out.println("Staticized " + target.getSignature() + " -> " + newMethod.getSignature() + " in " + sm.getSignature());
                                }
                                else
                                {
                                     myPrint("Did not Inline : " + target.getSignature() + " because it is not inlinable at this call siteee " + stmt + " in method: " + sm.getSignature());
                                }
                            }
                        }
                    }
            }
        }
        return wasInlinable;
    }
}
