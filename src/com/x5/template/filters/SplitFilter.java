package com.x5.template.filters;

import java.util.ArrayList;
import java.util.List;

import com.x5.template.Chunk;

public class SplitFilter implements ChunkFilter
{
    public static final String DEFAULT_DELIM = "/\\s+/";
    
    public Object applyFilter(Chunk chunk, String text, String[] args)
    {
        if (text == null) return text;
        
        String delim = null;
        int limit = -1;
        
        if (args == null || args.length < 1 || args[0].length() < 1 || args[0].equals("split")) {
            delim = DEFAULT_DELIM;
        } else {
            if (args.length == 1) {
                delim = args[0];
            } else if (args.length > 2) {
                delim = args[1];
                if (delim.length() == 0) delim = DEFAULT_DELIM;
                try {
                    limit = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {}
            }
        }
        if (delim.length() > 1 && delim.charAt(0) == '/' && delim.charAt(delim.length()-1) == '/') {
            String regexDelim = delim.substring(1,delim.length()-1);
            if (limit > 0) {
                String[] parts = text.split(regexDelim,limit+1);
                if (parts.length > limit) {
                    String[] limited = new String[limit];
                    System.arraycopy(parts,0,limited,0,limit);
                    return limited;
                } else {
                    return parts;
                }
            } else {
                return text.split(regexDelim);
            }
        } else {
            return splitNonRegex(text,delim,limit);
        }
    }

    public Object applyFilter(Chunk chunk, Object obj, String[] args)
    {
        if (obj == null) return null;
        return applyFilter(chunk, obj.toString(), args);
    }

    public String getFilterName()
    {
        return "split";
    }

    public String[] getFilterAliases()
    {
        return null;
    }

    public static String[] splitNonRegex(String input, String delim)
    {
        return splitNonRegex(input,delim,-1);
    }
    
    public static String[] splitNonRegex(String input, String delim, int limit)
    {
        List<String> l = new ArrayList<String>();

        int cursor = 0;
        int delimLen = delim.length();
        while (true) {
            int index = input.indexOf(delim,cursor);
            if (index == -1) {
                l.add(input.substring(cursor));
            } else {
                l.add(input.substring(cursor, index));
                cursor = index + delimLen;
            }
            if (index == -1 || (limit > 0 && l.size() >= limit)) {
                return l.toArray(new String[l.size()]);
            }
        }
    }
}
