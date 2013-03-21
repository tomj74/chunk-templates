package com.x5.template;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.x5.util.ObjectDataMap;
import com.x5.util.TableData;

public class TableOfMaps implements TableData
{
    private List<Map<String,Object>> data;
    int cursor = -1;

    @SuppressWarnings({"unchecked","rawtypes"})
    public TableOfMaps(List list)
    {
        this.data = (List<Map<String,Object>>)list;
    }
    
    public String[] getColumnLabels()
    {
        return null;
    }

    public void setColumnLabels(String[] labels)
    {
    }

    public String[] getRow()
    {
        return null;
    }

    public boolean hasNext()
    {
        if (data != null && data.size() > cursor + 1) {
            return true;
        } else {
            return false;
        }
    }

    public Map<String,Object> nextRecord()
    {
        cursor++;
        if (data == null || cursor >= data.size()) {
            return null;
        } else {
            return data.get(cursor);
        }
    }

    public void reset()
    {
        cursor = -1;
    }

    @SuppressWarnings("rawtypes")
    static TableData boxObjectArray(Object[] dataStore)
    {
        if (dataStore == null || dataStore.length < 1) {
            return null;
        }
        
        List<Map> boxedObjects = new ArrayList<Map>();
        for (int i=0; i<dataStore.length; i++) {
            boxedObjects.add(new ObjectDataMap(dataStore[i]));
        }
        
        return new TableOfMaps(boxedObjects);
    }
    
    @SuppressWarnings("rawtypes")
    static TableData boxObjectList(List dataStore)
    {
        if (dataStore == null || dataStore.size() < 1) {
            return null;
        }
        
        List<Map> boxedObjects = new ArrayList<Map>();
        Iterator i = dataStore.iterator();
        while (i.hasNext()) {
            boxedObjects.add(new ObjectDataMap(i.next()));
        }
        
        return new TableOfMaps(boxedObjects);
    }

}
