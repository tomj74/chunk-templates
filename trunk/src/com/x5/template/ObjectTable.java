package com.x5.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.x5.util.TableData;

/**
 * ObjectTable wraps a Map in a TableData interface,
 * making it look like a table of keys and values without
 * actually copying it into a table of keys and values.
 *
 * @author tmcclure
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class ObjectTable implements TableData, Map<String,Object>
{
    private Map obj;
    private Iterator i = null;
    private Object currentKey = null;

    public static final String KEY = "key";
    public static final String VALUE = "value";
    private static final String[] LABELS = new String[]{KEY,VALUE};

    public ObjectTable(Map map)
    {
        this.obj = map;
    }

    public String[] getColumnLabels()
    {
        return LABELS;
    }

    public void setColumnLabels(String[] labels)
    {
        // no-op.
    }

    public String[] getRow()
    {
        if (currentKey == null) return null;
        return new String[]{currentKey.toString(),obj.get(currentKey).toString()};
    }

    public boolean hasNext()
    {
        if (obj == null) return false;

        if (i == null) {
            i = getOrderedKeys().iterator();
        }

        return i.hasNext();
    }

    public Map<String,Object> nextRecord()
    {
        if (this.hasNext()) {
            currentKey = i.next();
            return this;
        } else {
            currentKey = null;
            return null;
        }
    }

    private List getOrderedKeys()
    {
        List keys = new ArrayList(obj.keySet());
        if (!(obj instanceof LinkedHashMap || obj instanceof SortedMap)) {
            Collections.sort(keys);
        }
        return keys;
    }

    public void reset()
    {
        i = getOrderedKeys().iterator();
    }

    public int size()
    {
        if (this.hasNext()) return LABELS.length;
        return 0;
    }

    public boolean isEmpty()
    {
        if (currentKey == null) return true;
        return false;
    }

    public boolean containsKey(Object key)
    {
        if (key != null) {
            if (key.equals(LABELS[0]) || key.equals(LABELS[1])) {
                return true;
            }
        }
        return false;
    }

    public boolean containsValue(Object value)
    {
        if (value == null) return false;
        if (value.equals(obj.get(currentKey))) {
            return true;
        }
        return false;
    }

    public Object get(Object key)
    {
        if (key == null || currentKey == null) return null;

        if (key.equals(KEY)) {
            return currentKey;
        } else if (key.equals(VALUE)) {
            return obj.get(currentKey);
        }

        return null;
    }

    public Object put(String key, Object value)
    {
        // read-only
        return null;
    }

    public Object remove(Object key)
    {
        // read-only
        return null;
    }

    public void putAll(Map<? extends String,? extends Object> m)
    {
        // read-only
    }

    public void clear()
    {
        // read-only
    }

    public Set<String> keySet()
    {
        HashSet<String> keys = new HashSet<String>();
        keys.add(KEY);
        keys.add(VALUE);
        return keys;
    }

    public Collection<Object> values()
    {
        if (currentKey != null) {
            HashSet<Object> values = new HashSet<Object>();
            values.add(obj.get(currentKey));
            return values;
        } else {
            return null;
        }
    }

    public Set<java.util.Map.Entry<String,Object>> entrySet()
    {
        // not implemented
        return null;
    }

}
