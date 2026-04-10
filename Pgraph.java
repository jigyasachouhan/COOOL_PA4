import java.util.*;
import soot.*;

public class Pgraph {
    Map<Value, Set<Integer>> sMap;
    Map<Integer, Map<SootField, Set<Integer>>> hMap;
    Map<Integer, SootClass> objIdToClassMap;

    public Pgraph() {
        this.sMap = new LinkedHashMap<>(16, 0.75f, true);
        this.hMap = new LinkedHashMap<>(16, 0.75f, true);
    }

    public void clear() {
        sMap.clear();
        hMap.clear();
    }

    
    public Map<Value, Set<Integer>> getSmapcopy()
    {
        return deepcopysmap(sMap);
    }
    public Map<Integer, Map<SootField,Set<Integer>>> getHmapcopy()
    {
        return deepcopyhmap(hMap);
    }

    public static Map<Value, Set<Integer>> deepcopysmap(Map<Value, Set<Integer>> original)
    {
        if (original == null)
        {
            return null;
        }
        Map<Value, Set<Integer>> copy = new LinkedHashMap<>(16, 0.75f, true);

        for (Map.Entry<Value, Set<Integer>> entry : original.entrySet())
        {
            Value key = entry.getKey();
            Set<Integer> value = entry.getValue();
            Set<Integer> newValue = (value != null) ? new HashSet<>(value) : null;
            copy.put(key, newValue);
        }
        return copy;
    }
    public static Map<Integer, Map<SootField, Set<Integer>>> deepcopyhmap(Map<Integer, Map<SootField, Set<Integer>>> original)
    {
        if (original == null)
        {
            return null;
        }
        Map<Integer, Map<SootField, Set<Integer>>> copy = new LinkedHashMap<>(16, 0.75f, true);
        for (Map.Entry<Integer, Map<SootField, Set<Integer>>> entry : original.entrySet())
        {
            Integer key = entry.getKey();
            Map<SootField, Set<Integer>> value = entry.getValue();
            Map<SootField, Set<Integer>> newValue = new LinkedHashMap<>(16, 0.75f, true);
            if (value == null)
            {
                newValue = null;
                copy.put(key, newValue);
                continue;
            }
            for (Map.Entry<SootField, Set<Integer>> innerEntry : value.entrySet())
            {
                SootField innerkey = innerEntry.getKey();
                Set<Integer> innerValue = innerEntry.getValue();
                Set<Integer> newInnerValue = (innerValue != null) ? new HashSet<>(innerValue) : null;
                newValue.put(innerkey, newInnerValue);
            }
            copy.put(key, newValue);
        }
        return copy;
    }


    public Pgraph deepcopy()
    {
        Pgraph copy = new Pgraph();
        copy.hMap = deepcopyhmap(hMap);
        copy.sMap = deepcopysmap(sMap);
        return copy;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Pgraph)) return false;
        Pgraph other = (Pgraph) o;
        return (mapEquals(sMap, other.sMap) && heapEquals(hMap, other.hMap));
    }

    private boolean heapEquals(Map<Integer,Map<SootField,Set<Integer>>> m1,
            Map<Integer,Map<SootField,Set<Integer>>> m2) {
        // TODO Auto-generated method stub
        if (m1 == null && m2 == null)
        {
            return true;
        }
        if (m1 == null || m2 == null)
        {
            return false;
        }
        if (m1.size() != m2.size())
        {
            return false;
        }

        for (Map.Entry<Integer, Map<SootField, Set<Integer>>> entry : m1.entrySet())
        {
            Map<SootField, Set<Integer>> s2 = m2.get(entry.getKey());
            if (s2 == null || !fieldMapEquals(s2, entry.getValue()))
            {
                return false;
            }
        }
        return true; 
    }

    private boolean mapEquals(Map<Value,Set<Integer>> m1, Map<Value,Set<Integer>> m2) {
        // TODO Auto-generated method stub
        if (m1 == null && m2 == null)
        {
            return true;
        }
        if (m1 == null || m2 == null)
        {
            return false;
        }
        if (m1.size() != m2.size())
        {
            return false;
        }
        for (Map.Entry<Value, Set<Integer>> entry : m1.entrySet())
        {
            Set<Integer> s2 = m2.get(entry.getKey());
            if (s2 == null || !entry.getValue().equals(s2))
            {
                return false;
            }
        }
        return true;
    }

    private boolean fieldMapEquals(Map<SootField, Set<Integer>> m1, Map<SootField, Set<Integer>> m2)
    {
        if (m1 == null && m2 == null)
        {
            return true;
        }
        if (m1 == null || m2 == null)
        {
            return false;
        }
        if (m1.size() != m2.size())
        {
            return false;
        }

        for (Map.Entry<SootField, Set<Integer>> entry : m1.entrySet())
        {
            Set<Integer> s2 = m2.get(entry.getKey());
            if (s2 == null || !entry.getValue().equals(s2))
            {
                return false;
            }
        }
        return true;
    }


    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + deepstackhash(sMap);
        result = 32 * result + deepheaphash(hMap);
        return result;
    }

    private int deepheaphash(Map<Integer,Map<SootField,Set<Integer>>> hmap) {
        // // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'deepheaphash'");
        int hash = 0;
        if (hmap == null)
        {
            return 0;
        }
        for (Map.Entry<Integer, Map<SootField, Set<Integer>>> entry : hmap.entrySet()) {
            hash += entry.getKey().hashCode() * 31 + deepfieldhash(entry.getValue());
        }
        return hash; 
    }

    private int deepstackhash(Map<Value,Set<Integer>> smap) {
        // // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'deepstackhash'");
        int hash = 0;
        if (smap == null)
        {
            return 0;
        }
        for (Map.Entry<Value, Set<Integer>> entry : smap.entrySet()) {
            hash += entry.getKey().hashCode() * 31 + entry.getValue().hashCode();
        }
        return hash;
    }

    private int deepfieldhash(Map<SootField, Set<Integer>> fmap) {
        int hash = 0;
        if (fmap == null)
        {
            return 0;
        }
        for (Map.Entry<SootField, Set<Integer>> entry : fmap.entrySet()) {
            hash += entry.getKey().hashCode() * 31 + entry.getValue().hashCode();
        }
        return hash;        
    }


    @Override
    public String toString()
    {
        String myString = "";
        myString += "sMap\n";
        myString += sMap.toString();
        myString += "\n";
        myString += "hMap\n";
        myString += hMap.toString();
        return myString;
    }
}