package com.x5.util;

import java.util.HashMap;
import java.util.Map;

public class DataCapsuleTable implements TableData
{
    private DataCapsule[] records;
    private int cursor = -1;
    private Map<String,Object> currentRecord;
    private String[] columnLabels;

    public static DataCapsuleTable extractData(Object[] objArray)
    {
        if (objArray == null) {
            return null;
        }

        int capsuleCount = 0;

        DataCapsule[] dataCapsules = new DataCapsule[objArray.length];
        for (int i=0; i<objArray.length; i++) {
            Object o = objArray[i];
            if (o != null && o instanceof DataCapsule) {
                dataCapsules[i] = (DataCapsule)o;
                capsuleCount++;
            }
        }
        if (capsuleCount == 0) return null;

        return new DataCapsuleTable(dataCapsules);
    }

    public DataCapsuleTable(DataCapsule[] dataCapsules)
    {
        this.records = dataCapsules;
    }

    public String[] getColumnLabels()
    {
        if (columnLabels == null) {
            DataCapsuleReader fish = getReader();
            return fish.getColumnLabels();
        } else {
            return columnLabels;
        }
    }

    public Object[] getRowRaw()
    {
        if (cursor < 0) cursor = 0;

        if (records != null && records.length > cursor) {
            DataCapsuleReader fish = getReader();
            return fish.extractData(records[cursor]);
        } else {
            return null;
        }
    }

    private DataCapsuleReader getReader()
    {
        // careful not to advance cursor unintentionally
        // eg when just retrieving labels
        int readerIndex = cursor;
        if (readerIndex < 0) readerIndex = 0;
        if (records != null && records.length > readerIndex) {
            DataCapsule atCursor = records[readerIndex];
            return DataCapsuleReader.getReader(atCursor);
        }

        return null;
    }

    // convert non-strings in data to strings
    public String[] getRow()
    {
        Object[] rawRow = getRowRaw();
        String[] row = new String[rawRow.length];
        for (int i=0; i<rawRow.length; i++) {
            Object x = rawRow[i];
            if (x == null) {
                row[i] = null;
            } else if (x instanceof String) {
                row[i] = (String)x;
            } else {
                row[i] = x.toString();
            }
        }
        return row;
    }

    public boolean hasNext()
    {
        if (records != null && records.length > cursor + 1) {
            return true;
        } else {
            return false;
        }
    }

    public Map<String, Object> nextRecord()
    {
        cursor++;
        String[] values = getRow();

        if (values == null) return null;

        if (currentRecord == null) {
            // someday it might be nice if we could
            // map strings to nested DataCapsule[]s
            // and maybe even individual DataCapsule objects
            // ie, in addition to simple Strings
            // for now, restricting table data to String objects.
            currentRecord = new HashMap<String,Object>();
        } else {
            currentRecord.clear();
        }

        String[] labels = getColumnLabels();
        for (int i=0; i<labels.length; i++) {
            String label = labels[i];
            currentRecord.put(label,values[i]);
        }

        return currentRecord;
    }

    public void setColumnLabels(String[] labels)
    {
        this.columnLabels = labels;
    }

    public void reset()
    {
        this.cursor = -1;
    }
}
