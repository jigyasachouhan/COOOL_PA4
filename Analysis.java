import java.util.*;

import polyglot.ext.param.types.Param;
import soot.*;
import soot.jimple.AnyNewExpr;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Ref;
import soot.jimple.StaticFieldRef;
import soot.jimple.ThisRef;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewExpr;
import soot.jimple.toolkits.invoke.SiteInliner;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;
// import Pgraph.*;




public class Analysis extends ForwardFlowAnalysis<Unit, Pgraph> {

    Map<Integer, Integer> lineNoToObjID = new LinkedHashMap<>(16, 0.75f, true);
    int objId = 0;
    int params=-1;
    Map<SootClass, Integer> StaticObjMap = new LinkedHashMap<>(16, 0.75f, true);
    Set<Integer> negSet = new HashSet<>();
    Map<Immediate, Integer> immediateToOBjID = new LinkedHashMap<>(16, 0.75f, true);
    Map<Unit, Boolean> inlinableMap;

    public Analysis(DirectedGraph<Unit> graph, Map<Unit, Boolean> inlinableMap) {
        super(graph);
        this.inlinableMap = inlinableMap;
        negSet.add(-1);
        doAnalysis();
        //TODO Auto-generated constructor stub
    }

    void myPrint(Object toPrint)
    {
        System.out.println(toPrint.toString());
    }

    protected int getobjID()
    {
        int newID = objId;
        objId = objId + 1;
        return newID;
    }

    protected void handleIdentityStmt(JIdentityStmt stmt, Pgraph out)
    {
        myPrint("Handling Identity Statement");
        Value rhs = stmt.getRightOp();
        Integer ID = params;
        params = params-1;
        Set<Integer> s = new HashSet<>();
        s.add(ID);
        out.sMap.put(rhs, s);
        out.hMap.put(ID, new LinkedHashMap<>(16, 0.75f, true));
        lineNoToObjID.put(ID, ID);
        if(rhs instanceof ParameterRef){
            ParameterRef pr = (ParameterRef)  rhs;
            Type tp = pr.getType();
            out.objIdToClassMap.put(ID, tp);
        }
        if(rhs instanceof ThisRef){
            ThisRef tr = (ThisRef)  rhs;
            Type tp = tr.getType();
            out.objIdToClassMap.put(ID, tp);
        }
        myPrint("Out Set after handling identity statement");
        myPrint(out);
    }

    protected void handleAssignStmt(Pgraph in, JAssignStmt stmt, Pgraph out)
    {
        // 
        myPrint("Handling Assignment");
        Value lhs = stmt.getLeftOp();
        Value rhs = stmt.getRightOp();

        myPrint("rhs immidiete: " + (rhs instanceof Immediate));
        myPrint("rhs Primtype: " + (rhs instanceof PrimType));
        myPrint("lhs immidiete: " + (lhs instanceof Immediate));
        myPrint("lhs Primtype: " + (lhs instanceof PrimType));
        // New Expression
        if (rhs instanceof NewExpr && lhs instanceof Local)
        {
            myPrint("new Expression");
            int ID;
            int lineno = stmt.getJavaSourceStartLineNumber();
            int myID = lineno;
            if (lineNoToObjID.keySet().contains(myID))
            {
                ID = lineNoToObjID.get(myID);
            }
            else
            {
                ID = getobjID();
            }
            Set<Integer> s = new HashSet<>();
            s.add(ID);
            out.sMap.put(lhs, s);
            out.hMap.put(ID, new LinkedHashMap<>(16, 0.75f, true));
            lineNoToObjID.put(myID, ID);
            out.objIdToClassMap.put(ID, ((NewExpr) rhs).getBaseType());
            myPrint("Out Set after handling Assignment statement");
            myPrint(out);
            return;
        }

        // Simple Assignment
        if (rhs instanceof Local && lhs instanceof Local)
        {
            myPrint("Simple Assignment");
            Set<Integer> v = out.sMap.get(rhs);
            if (v == null || v.isEmpty())
            {
                out.sMap.put(lhs, new HashSet<>());
            }
            else
            {
                out.sMap.put(lhs, new HashSet<>(v));
            }
            myPrint("Out Set after handling Assignment statement");
            myPrint(out);
            return;
        }

        // Field load: x = y.f
        if (rhs instanceof InstanceFieldRef && lhs instanceof Local)
        {
            myPrint("Field Load");
            InstanceFieldRef fr = (InstanceFieldRef) rhs;
            Value base = fr.getBase();
            SootField field = fr.getField();
            Set<Integer> bases = out.sMap.get(base);
            Set<Integer> results = new HashSet<>();

            if (bases.contains(-1))
            {
                results.add(-1);
            }

            if (bases != null)
            {
                for (int obj : bases)
                {
                    Map<SootField, Set<Integer>> fmap = out.hMap.get(obj);
                    if (fmap != null)
                    {
                        Set<Integer> targets = fmap.get(field);
                        if (targets != null)
                        {
                            results.addAll(targets);
                        }
                        else
                        {
                            results.add(-1);
                        }
                    }
                    else
                    {
                        results.add(-1);
                    }
                }
            }
            out.sMap.put(lhs, results);
            myPrint("Out Set after handling Assignment statement");
            myPrint(out);
            return;
        }

        // Field store
        // a.f = b
        if (lhs instanceof InstanceFieldRef && rhs instanceof Local)
        {
            myPrint("Field Store");
            InstanceFieldRef fr = (InstanceFieldRef) lhs;
            Value base = fr.getBase();
            SootField field = fr.getField();

            Set<Integer> bases = out.sMap.getOrDefault(base, new HashSet<>());
            Set<Integer> rhsobjs = out.sMap.getOrDefault(rhs, new HashSet<>(negSet));

            if (bases.size() == 1)
            {
                for (int obj : bases)
                {
                    if (obj == -1)
                    {
                        continue;
                    }
                    Map<SootField, Set<Integer>> m = out.hMap.getOrDefault(obj, new LinkedHashMap<>(16, 0.75f, true));
                    Set<Integer> newset = new HashSet<>(rhsobjs);
                    m.put(field, newset);
                }
            }
            else
            {
            for (int obj : bases)
            {
                if (obj == -1)
                {
                    continue;
                }
                Set<Integer> oldset = out.hMap.getOrDefault(obj, new LinkedHashMap<>(16, 0.75f, true)).getOrDefault(field, new HashSet<>(negSet));
                oldset.addAll(rhsobjs);
                if (!out.hMap.keySet().contains(obj))
                {
                    out.hMap.put(obj, new LinkedHashMap<>(16, 0.75f, true));
                }

                Map<SootField, Set<Integer>> obj_map = out.hMap.get(obj);
                obj_map.put(field, oldset);
            }
            }
            myPrint("Out Set after handling Assignment statement");
            myPrint(out);
            return;
        }
        if (rhs instanceof StaticFieldRef && lhs instanceof Local)
        {
            myPrint("Static RHS");
            if (!(lhs instanceof InstanceFieldRef))
            {
                out.sMap.put(lhs, new HashSet<>(negSet));
            }
            myPrint("Out Set after handling Assignment statement");
            myPrint(out);
            return;
        }
        if (lhs instanceof StaticFieldRef)
        {
            myPrint("Static LHS");
        }

        if (lhs instanceof Local && (rhs instanceof Immediate))
        {
            // Here we treat the primitive as a new object.
            int ID;
            Immediate rhsIM = (Immediate) rhs;

            if (immediateToOBjID.keySet().contains(rhsIM))
            {
                ID = immediateToOBjID.get(rhsIM);
            }
            else
            {
                ID = getobjID();
            }
            Set<Integer> s = new HashSet<>();
            s.add(ID);
            out.sMap.put(lhs, s);
            out.hMap.put(ID, new LinkedHashMap<>(16, 0.75f, true));
            immediateToOBjID.put(rhsIM, ID);
            myPrint("Out Set after handling Assignment statement");
            myPrint(out);
            return;            
        }

        if (lhs instanceof InstanceFieldRef && rhs instanceof Immediate)
        {
            Immediate rhsIM = (Immediate) rhs;

            myPrint("Field Store of Immidiate");
            InstanceFieldRef fr = (InstanceFieldRef) lhs;
            Value base = fr.getBase();
            SootField field = fr.getField();

            Set<Integer> bases = out.sMap.getOrDefault(base, new HashSet<>());
            int ID;
            if (immediateToOBjID.keySet().contains(rhsIM))
            {
                ID = immediateToOBjID.get(rhsIM);
            }
            else
            {
                ID = getobjID();
            }
            immediateToOBjID.put(rhsIM, ID);
            Set<Integer> rhsobjs = new HashSet<>();
            rhsobjs.add(ID);

            if (bases.size() == 1)
            {
                for (int obj : bases)
                {
                    if (obj == -1)
                    {
                        continue;
                    }
                    Map<SootField, Set<Integer>> m = out.hMap.getOrDefault(obj, new LinkedHashMap<>(16, 0.75f, true));
                    Set<Integer> newset = new HashSet<>(rhsobjs);
                    m.put(field, newset);
                }
            }
            else
            {
            for (int obj : bases)
            {
                if (obj == -1)
                {
                    continue;
                }
                Set<Integer> oldset = out.hMap.getOrDefault(obj, new LinkedHashMap<>(16, 0.75f, true)).getOrDefault(field, new HashSet<>(negSet));
                oldset.addAll(rhsobjs);
                if (!out.hMap.keySet().contains(obj))
                {
                    out.hMap.put(obj, new LinkedHashMap<>(16, 0.75f, true));
                }

                Map<SootField, Set<Integer>> obj_map = out.hMap.get(obj);
                obj_map.put(field, oldset);
            }
            }
            myPrint("Out Set after handling Assignment statement");
            myPrint(out);
            return;            
        }

    }

    void collectReachable(Value base, Pgraph out, Set<Integer> killSet)
    {
        Set<Integer> newInts = out.sMap.get(base);

        if (newInts == null || newInts.isEmpty())
        {
            return;
        }

        for (int i : newInts)
        {
            dfsHeap(i, out, killSet);
        }
    }

    void dfsHeap(int node, Pgraph out, Set<Integer> killSet)
    {
        if (killSet.contains(node))
        {
            return;
        }
        killSet.add(node);

        Map<SootField, Set<Integer>> children = out.hMap.get(node);
        if (children == null)
        {
            return;
        }
        for (Set<Integer> i : children.values())
        {
            if ( i == null)
            {
                continue;
            }
            for (Integer j : i)
            {
                dfsHeap(j, out, killSet);
            }
        }

    }

    protected void handleInvoke(Pgraph in, JInvokeStmt stmt, Pgraph out)
    {
        myPrint("Handling Invoke");
        Set<Integer> killSet = new HashSet<>();
        InvokeExpr invoke = stmt.getInvokeExpr();
        if (invoke instanceof InstanceInvokeExpr)
        {
            Value base = ((InstanceInvokeExpr) invoke).getBase();
            collectReachable(base, out, killSet);
        }
        if (invoke.getArgs() != null)
        {
        for (Value arg : invoke.getArgs())
        {
            collectReachable(arg, out, killSet);
        }
        }
        myPrint("Out Set before handling Invoke statement");
        myPrint(out);
        for (int i : killSet)
        {
            myPrint("killing: " + i);
            out.hMap.put(i, new LinkedHashMap<>(16, 0.75f, true));;
        }
        myPrint("Out Set after handling Invoke statement");
        myPrint(out);

        if (invoke instanceof VirtualInvokeExpr) {
            VirtualInvokeExpr vie = (VirtualInvokeExpr) invoke;

            // Base object (the receiver)
            Local base = (Local) vie.getBase();

            // Method being called
            SootMethod method = vie.getMethod();

            // Arguments
            List<Value> args = vie.getArgs();
            SootMethod target = vie.getMethod();


            // Skip unsafe cases
            if (!target.isConcrete()) return;
            if (target.getDeclaringClass().isInterface()) return;

            Set<Integer> bases = in.sMap.getOrDefault(base, new HashSet<>());
            Set<Type> types = new HashSet<>();
            for(Integer b : bases)
            {
                if(in.objIdToClassMap.containsKey(b))
                    types.add(in.objIdToClassMap.get(b));
            }
            if(types.size() == 1)
            {
                System.out.println("This is inlineable");
                Type t = types.iterator().next();
                SootClass concreteClass = ((RefType) t).getSootClass();

                // Make sure the method actually exists in this class
                if (!concreteClass.declaresMethod(method.getSubSignature())) return;

                SootMethod resolved = concreteClass.getMethod(method.getSubSignature());

                // Set the refined method reference
                vie.setMethodRef(resolved.makeRef());

                this.inlinableMap.put(stmt, true);
            }
            
        }

    }

    @Override
    protected void flowThrough(Pgraph in, Unit stmt, Pgraph out) {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'flowThrough'");

        // A few cases to deal with here.

        // What types can Unit be:

        // myPrint("Reached here");
        // out = in.deepcopy();
        out.hMap = in.getHmapcopy();
        out.sMap = in.getSmapcopy();
        out.objIdToClassMap = in.getobjmapcopy();


        // myPrint("Hello?");
        myPrint("=======================");
        myPrint("handingl statement");
        myPrint(stmt);
        if (stmt instanceof JInvokeStmt)
        {
            handleInvoke(in, (JInvokeStmt) stmt, out);
        }
        if (stmt instanceof JIdentityStmt)
        {
            handleIdentityStmt((JIdentityStmt) stmt, out);
        }
        if (stmt instanceof JAssignStmt)
        {
            handleAssignStmt(in, (JAssignStmt) stmt, out);
        }
        myPrint("=======================");
    }

    @Override
    protected void copy(Pgraph arg0, Pgraph arg1) {
        // TODO Auto-generated method stub
        // myPrint("Copy arg0");
        // myPrint(arg0);
        // myPrint("Copy arg1");
        // myPrint(arg1);
        // arg1 = arg0.deepcopy();
        // myPrint(arg1);
        arg1.hMap = arg0.getHmapcopy();
        arg1.sMap = arg0.getSmapcopy();
    }

    @Override
    protected void merge(Pgraph in1, Pgraph in2, Pgraph out) {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'merge'");
        out.clear();

        // myPrint("Merge");
        myPrint("==================================");
        myPrint("MERGING");
        myPrint(in1);
        myPrint(in2);
        myPrint("+=======================================");

        if (empty(in1))
        {
            out.hMap = in2.getHmapcopy();
            out.sMap = in2.getSmapcopy();
        }
        else if (empty(in2))
        {
            out.hMap = in1.getHmapcopy();
            out.sMap = in1.getSmapcopy();
        }
        else
        {
        out.hMap = mergeHmap(in1.hMap, in2.hMap);
        out.sMap = mergeSmap(in1.sMap, in2.sMap);
        }
    }

    private boolean empty (Pgraph p)
    {
        return ((p.hMap.size() == 0) && (p.sMap.size() == 0));
    }
        

    private Map<Value, Set<Integer>> mergeSmap(Map<Value,Set<Integer>> sMap, Map<Value,Set<Integer>> sMap2) {
        if (sMap == null && sMap2 == null)
        {
            return new LinkedHashMap<>(16, 0.75f, true);
        }
        if (sMap == null)
        {
            sMap = new LinkedHashMap<>(16, 0.75f, true);
        }
        if (sMap2 == null)
        {
            sMap2 = new LinkedHashMap<>(16, 0.75f, true);
        }
        Set<Value> keys1 = sMap.keySet();
        Set<Value> keys2 = sMap2.keySet();
        Set<Value> allkeys = mergeSet1(keys1, keys2);
        Map<Value, Set<Integer>> mergedSet = new LinkedHashMap<>(16, 0.75f, true);
        for (Value k : allkeys)
        {
            Set<Integer> i1 = sMap.get(k);
            Set<Integer> i2 = sMap2.get(k);
            Set<Integer> merged = mergeSet(i1, i2);
            mergedSet.put(k, merged);
        }
        return mergedSet;
    }
    private Set<Integer> mergeSet(Set<Integer> s1, Set<Integer> s2)
    {     
        if (s1 == null && s2 == null)
        {
            Set<Integer> newSet = new HashSet<>();
            return newSet;
        }
        if (s1 == null)
        {
            // Set<T> newSet = new HashSet<>(s2);
            // return newSet;
            s1 = new HashSet<>();
        }
        if (s2 == null)
        {
            s2 = new HashSet<>();
        }
        Set<Integer> newSet = new HashSet<>(s1);
        if (s1.isEmpty() || s2.isEmpty())
        {
            newSet.add(-1);
        }
        newSet.addAll(s2);
        return newSet;
    }

    private <T> Set<T> mergeSet1(Set<T> s1, Set<T> s2)
    {     
        if (s1 == null && s2 == null)
        {
            Set<T> newSet = new HashSet<>();
            return newSet;
        }
        if (s1 == null)
        {
            // Set<T> newSet = new HashSet<>(s2);
            // return newSet;
            s1 = new HashSet<>();
        }
        if (s2 == null)
        {
            s2 = new HashSet<>();
        }
        Set<T> newSet = new HashSet<>(s1);
        newSet.addAll(s2);
        return newSet;
    }

    private Map<Integer, Map<SootField, Set<Integer>>> mergeHmap(Map<Integer,Map<SootField,Set<Integer>>> hMap,
            Map<Integer,Map<SootField,Set<Integer>>> hMap2) {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'mergeHmap'");
        Map<Integer, Map<SootField, Set<Integer>>> mergedMap = new LinkedHashMap<>(16, 0.75f, true);

        Set<Integer> keys1 = hMap.keySet();
        Set<Integer> keys2 = hMap2.keySet();
        Set<Integer> allkeys = mergeSet1(keys1, keys2);

        for (Integer k : allkeys)
        {
            Map<SootField, Set<Integer>> map1 = hMap.get(k);
            Map<SootField, Set<Integer>> map2 = hMap2.get(k);
            if (map1 == null && map2 == null)
            {
                mergedMap.put(k, new LinkedHashMap<>(16, 0.75f, true));
                continue;
            }
            if (map1 == null)
            {
                map1 = new LinkedHashMap<>(16, 0.75f, true);
            }
            if (map2 == null)
            {
                map2 = new LinkedHashMap<>(16, 0.75f, true);
            }
            Set<SootField> innerk1 = map1.keySet();
            Set<SootField> innerk2 = map2.keySet();
            Map<SootField, Set<Integer>> newMap = new LinkedHashMap<>(16, 0.75f, true);
            Set<SootField> merged_inner_keys = mergeSet1(innerk1, innerk2);
            
            for (SootField inner_key : merged_inner_keys)
            {
                Set<Integer> v1 = map1.get(inner_key);
                Set<Integer> v2 = map2.get(inner_key);
                Set<Integer> merged = mergeSet(v1, v2);
                newMap.put(inner_key, merged);
            }
            mergedMap.put(k, newMap);
        }

        return mergedMap;
    }

    @Override
    protected Pgraph newInitialFlow() {
        // TODO Auto-generated method stub
        return new Pgraph();
    }

    
}