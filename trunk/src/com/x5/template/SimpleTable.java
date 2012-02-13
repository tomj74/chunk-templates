package com.x5.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import com.x5.util.TableData;

public class SimpleTable implements TableData
{
	private String[] labels;
    private ArrayList<String[]> records;
    private int cursor = -1;
    private Map<String,String> currentRecord;
    
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
        if (cursor < 0) cursor = 0;

        if (records != null && records.size() > cursor) {
            return records.get(cursor);
        } else {
            return null;
        }
	}

	public boolean hasNext()
	{
        if (records != null && records.size() > cursor + 1) {
            return true;
        } else {
            return false;
        }
	}

	public Map<String, String> nextRecord()
	{
        cursor++;
        String[] values = getRow();

        if (values == null) return null;

        if (currentRecord == null) {
            currentRecord = new HashMap<String,String>(values.length);
        } else {
            currentRecord.clear();
        }

        for (int i=0; i<labels.length; i++) {
            String label = labels[i];
            currentRecord.put(label,values[i]);
        }

        return currentRecord;
	}
}