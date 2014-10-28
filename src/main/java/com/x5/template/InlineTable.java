package com.x5.template;

import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.x5.util.TableData;

public class InlineTable
{
    // comma or close-bracket
    private static final Pattern DELIM = Pattern.compile("[,\\]]");

    /**
     * for testing...
     */
    public static void main(String[] args)
    {
        String test = "[[code,name,price],[abc,Apples,$2.50],[xyz,Whiz-Bang \\[you\\, and everyone\\, will love it!\\],$13.99]]";
        System.out.println("Reading in table...");
        TableData table = InlineTable.parseTable(test);
        System.out.println("...finished.  Checking data structures:");

        String[] labels = table.getColumnLabels();

        Map<String,Object> record;

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

    public static TableData parseTable(String data)
    {
        return _parseTable(data);
    }

    /*
     * example [[code,name,price],[abc,Apples,$2.50],[xyz,Whiz-Bang \[you\, and everyone\, will love it!\],$13.99]]
     */
    private static SimpleTable _parseTable(String data)
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
            return null;
        }

        String[] labels = null;
        ArrayList<String[]> records = null;

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

        return new SimpleTable(labels, records);
    }

    private static int nextUnescapedDelim(Pattern delim, String input, int searchFrom)
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

}
