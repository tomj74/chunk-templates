package com.x5.template;

import java.util.List;
import java.util.Map;

import com.x5.util.TableData;

public class TableOfMaps implements TableData
{
    private List<Map<String,Object>> data;
    int cursor = -1;

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

}
