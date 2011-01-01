package com.x5.template;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.x5.util.TableData;

public class InlineTable implements TableData
{
    private String[] labels;
    private ArrayList<String[]> records;
    private int cursor = -1;
    private Map currentRecord;

    // comma or close-bracket
    private static final Pattern DELIM = Pattern.compile("[,\\]]");

    /**
     * for testing...
     */
    public static void main(String[] args)
    {
        String test = "[[code,name,price],[abc,Apples,$2.50],[xyz,Whiz-Bang \\[you\\, and everyone\\, will love it!\\],$13.99]]";
        System.out.println("Reading in table...");
        TableData table = new InlineTable(test);
        System.out.println("...finished.  Checking data structures:");

        String[] labels = table.getColumnLabels();

        Map<String,String> record;

        while (table.hasNext()) {
            record = table.nextRecord();
            for (int i=0; i<labels.length; i++) {
                if (i>0) System.out.print(", ");
                String label = labels[i];
                System.out.print(label + "=" + record.get(label));
            }
            System.out.println();
        }
    }

    public InlineTable(String data)
    {
        parseTable(data);
    }
    
    public InlineTable(String[] data)
    {
    	if (data == null) return;
    	labels = new String[]{"anonymous"};
    	// make a single-column table out of a String array.
    	records = new ArrayList<String[]>();
    	for (int i=0; i<data.length; i++) {
    		String[] record = new String[]{data[i]};
    		records.add(record);
    	}
    }

    /*
     * example [[code,name,price],[abc,Apples,$2.50],[xyz,Whiz-Bang \[you\, and everyone\, will love it!\],$13.99]]
     */
    private void parseTable(String data)
    {
        // first elt of array is labels
        // remainder is record data
        ArrayList<String> row = new ArrayList<String>();
        int marker = 0;
        int dataLen = data.length();
        // scan for bracket marking start of data
        marker = data.indexOf("[");

        if (marker < 0) {
            //System.err.println("could not locate start of data");
            return;
        }

        // parse a row
        while (marker > -1 && marker < dataLen) {
            // scan for bracket marking start of row
            marker = data.indexOf("[",marker+1);
            if (marker < 0) {
                //System.err.println("no more rows");
                break;
            }

            // parse a single field
            while (marker > 0 && marker < dataLen && data.charAt(marker) != ']') {
                marker++;
                // scan for unescaped comma OR unescaped close-bracket
                int delimPos = nextUnescapedDelim(DELIM, data, marker);
                if (delimPos > 0) {
                    String field = data.substring(marker,delimPos);
                    // apply escapes
                    field = field.replace("\\[","[");
                    field = field.replace("\\]","]");
                    field = field.replace("\\,",",");
                    row.add(field);
                    //System.err.println("parsed one field");
                } else {
                    //System.err.println("missing field/row delimiter!");
                }
                marker = delimPos;
            }

            if (row.size() > 0) {
                // record row
                String[] parsedRow = new String[row.size()];
                parsedRow = row.toArray(parsedRow);
                if (labels == null) {
                    labels = parsedRow;
                } else {
                    if (records == null) records = new ArrayList<String[]>();
                    records.add(parsedRow);
                }
                row.clear();
            }
            // scan to start of next row
            if (marker > 0) marker = data.indexOf(",",marker+1);
        }
    }

    public static int nextUnescapedDelim(Pattern delim, String input, int searchFrom)
    {
        Matcher m = delim.matcher(input);
        boolean hasDelim = m.find(searchFrom);

        if (!hasDelim) return -1;
        int delimPos = m.start();

        boolean isProvenDelimeter = false;
        while (!isProvenDelimeter) {
            // count number of backslashes that precede this forward slash
            int bsCount = 0;
            while (delimPos-(1+bsCount) >= searchFrom && input.charAt(delimPos - (1+bsCount)) == '\\') {
                bsCount++;
            }
            // if odd number of backslashes precede this delimiter char, it's escaped
            // if even number precede, it's not escaped, it's the true delimiter
            // (because it's preceded by either no backslash or an escaped backslash)
            if (bsCount % 2 == 0) {
                isProvenDelimeter = true;
            } else {
                // keep looking for real delimiter
                if (m.find()) {
                    delimPos = m.start();
                } else {
                    // if the input is not legal (missing delimiters??), bail out
                    return -1;
                }
            }
        }
        return delimPos;
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

    public Map<String,String> nextRecord()
    {
        cursor++;
        String[] values = getRow();

        if (values == null) return null;

        if (currentRecord == null) {
            currentRecord = new HashMap<String,String>();
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
