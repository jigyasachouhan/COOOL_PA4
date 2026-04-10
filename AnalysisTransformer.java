import java.util.*;
import soot.*;
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Ref;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;

import soot.jimple.internal.JNewExpr;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.InstanceInvokeExpr;
import soot.util.*;

public class AnalysisTransformer extends SceneTransformer {

    static CallGraph cg;


    public void myPrint(Object toPrint)
    {
        System.out.println(toPrint.toString());
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        // cg = Scene.v().getCallGraph();
        // SootClass mainClass = Scene.v().getMainClass();
        // SootMethod mainMethod = mainClass.getMethodByName("main");
        // myPrint("Main method: " + mainMethod);
        // myPrint("Call graph edges from main method:");
        // Iterator<Edge> edges = cg.edgesOutOf(mainMethod);
        // while (edges.hasNext()) {
        //     Edge edge = edges.next();
        //     myPrint(edge);
        // }
        System.out.println("Starting transformation...");
        for(SootClass sc : Scene.v().getApplicationClasses()) {
            for(SootMethod sm : sc.getMethods()) {
                Chain<Unit> units = sm.retrieveActiveBody().getUnits();
                Iterator<Unit> unitIt = units.snapshotIterator();
                while(unitIt.hasNext()) {
                    Unit u = unitIt.next();
                    if(u instanceof JInvokeStmt) {
                        System.out.println("Found an invoke statement: " + u);
                        InvokeExpr invoke = ((JInvokeStmt) u).getInvokeExpr();
                        if(invoke instanceof InstanceInvokeExpr && !(invoke.getMethod().isConstructor())) {
                            System.out.println("It's an instance invoke expression: " + invoke);
                            units.remove(u);
                        }
                    }
            }
            }
    }
    }
}
