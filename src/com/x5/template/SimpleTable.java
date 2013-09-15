package com.x5.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import com.x5.util.TableData;

public class SimpleTable implements TableData, Map<String,Object>
{
    private String[] labels;
    private List<String[]> records;
    private int cursor = -1;
    private int size = 0;
    private Map<String,Integer> columnIndex;
    ///private Map<String,Object> currentRecord;

    public static final String ANON_ARRAY_LABEL = "_anonymous_";

    public SimpleTable(String[] columnLabels, Vector<String[]> tableRows)
    {
        this.labels = columnLabels;
        this.records = new ArrayList<String[]>(tableRows);
    }

    public SimpleTable(String[] columnLabels, ArrayList<String[]> tableRows)
    {
        this.labels = columnLabels;
        this.records = tableRows;
    }

    public SimpleTable(String[] columnLabels, String[][] tableRows)
    {
        this.labels = columnLabels;
        this.records = new ArrayList<String[]>();
        if (tableRows != null) {
            for (int i=0; i<tableRows.length; i++) {
                this.records.add(tableRows[i]);
            }
        }
    }

    public SimpleTable(String[] data)
    {
        if (data == null) return;
        labels = new String[]{ANON_ARRAY_LABEL};
        // make a single-column table out of a String array.
        records = new ArrayList<String[]>();
        for (int i=0; i<data.length; i++) {
            String[] record = new String[]{data[i]};
            records.add(record);
        }
    }

    @SuppressWarnings("rawtypes")
    public SimpleTable(List list)
    {
        if (list == null) return;
        labels = new String[]{ANON_ARRAY_LABEL};
        // make a single-column table out of a String array.
        records = new ArrayList<String[]>();
        for (int i=0; i<list.size(); i++) {
            String[] record = new String[]{list.get(i).toString()};
            records.add(record);
        }
    }

    public String[] getColumnLabels()
    {
        return labels;
    }

    public void setColumnLabels(String[] labels)
    {
        this.labels = labels;
    }

    public String[] getRow()
    {
        if (cursor < 0) {
            cursor = 0;
            size = records == null ? 0 : records.size();
        }

        if (size > cursor) {
            return records.get(cursor);
        } else {
            return null;
        }
    }

    public boolean hasNext()
    {
        if (size > cursor + 1) {
            return true;
        } else if (size == 0) {
            size = records == null ? 0 : records.size();
            return (size > cursor + 1);
        } else {
            return false;
        }
    }

    public Map<String, Object> nextRecord()
    {
        cursor++;
        if (size > cursor) {
            return this;
        } else if (size == 0) {
            size = records == null ? 0 : records.size();
            return (size > cursor) ? this : null;
        } else {
            return null;
        }
    }

    public void reset()
    {
        this.cursor = -1;
    }


    // for efficiency, this obj is returned as the record object as well.

    public int size()
    {
        return (labels == null) ? 0 : labels.length;
    }

    public boolean isEmpty()
    {
        return labels == null;
    }

    public boolean containsKey(Object key)
    {
        if (columnIndex == null) indexColumns();
        if (columnIndex == null) {
            return false;
        } else {
            return columnIndex.containsKey(key);
        }
    }

    private void indexColumns()
    {
        if (labels != null) {
            columnIndex = new HashMap<String,Integer>(labels.length);
            for (int i=0; i<labels.length; i++) {
                columnIndex.put(labels[i], i);
            }
        }
    }

    public boolean containsValue(Object value)
    {
        String[] record = getRow();

        if (record == null) return false;

        for (int i=0; i<record.length; i++) {
            if (value.equals(record[i])) return true;
        }
        return false;
    }

    public Object get(Object key)
    {
        if (labels == null) return null;
        if (columnIndex == null) indexColumns();
        if (columnIndex != null && columnIndex.containsKey(key)) {
            String[] record = getRow();
            try {
                return record[columnIndex.get(key)];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        } else {
            return null;
        }
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
        if (labels == null) return null;
        if (columnIndex == null) indexColumns();
        if (columnIndex != null) {
            return columnIndex.keySet();
        } else {
            return null;
        }
    }

    public Collection<Object> values()
    {
        String[] record = getRow();
        if (record == null) {
            return null;
        } else {
            List<Object> list = new ArrayList<Object>();
            for (int i=0; i<record.length; i++) {
                list.add(record[i]);
            }
            return list;
        }
    }

    public Set<java.util.Map.Entry<String,Object>> entrySet()
    {
        // not implemented
        return null;
    }
}