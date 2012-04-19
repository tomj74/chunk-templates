package com.x5.template.filters;

import com.x5.template.Chunk;

public class FormatFilter extends BasicFilter
{
    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return null;
        
        String fmtString = null;
        if (args != null && args.length > 0) fmtString = args[0];
        
        if (fmtString == null) return "";
        
        return applyFormatString(text, fmtString);
    }
    
    public String getFilterName()
    {
        return "sprintf";
    }
    
    private static String applyFormatString(String text, String formatString)
    {
        // strip calling wrapper ie "sprintf(%.03f)" -> "%.03f"
        if (formatString.startsWith("sprintf(")) {
            formatString = formatString.substring(8);
            if (formatString.endsWith(")")) {
                formatString = formatString.substring(0,formatString.length()-1);
            }
        }
        // strip quotes if arg is quoted
        char first = formatString.charAt(0);
        char last = formatString.charAt(formatString.length()-1);
        if (first == last && (first == '\'' || first == '"')) {
            formatString = formatString.substring(1,formatString.length()-1);
        }

        return formatNumberFromString(formatString, text);
    }

    public static String formatNumberFromString(String formatString, String value)
    {
        char expecting = formatString.charAt(formatString.length()-1);
        try {
            if ("sS".indexOf(expecting) > -1) {
                return String.format(formatString, value);
            } else if ("eEfgGaA".indexOf(expecting) > -1) {
                float f = Float.valueOf(value);
                return String.format(formatString, f);
            } else if ("doxX".indexOf(expecting) > -1) {
                if (value.trim().startsWith("#")) {
                    long l = Long.parseLong(value.trim().substring(1),16);
                    return String.format(formatString, l);
                } else if (value.trim().startsWith("0X") || value.trim().startsWith("0x")) {
                    long l = Long.parseLong(value.trim().substring(2),16);
                    return String.format(formatString, l);
                } else {
                    float f = Float.valueOf(value);
                    return String.format(formatString, (long)f);
                }
            } else if ("cC".indexOf(expecting) > -1) {
                if (value.trim().startsWith("0X") || value.trim().startsWith("0x")) {
                    int i = Integer.parseInt(value.trim().substring(2),16);
                    return String.format(formatString, (char)i);
                } else {
                    float f = Float.valueOf(value);
                    return String.format(formatString, (char)f);
                }
            } else {
                return "[Unknown format "+expecting+": \""+formatString+"\","+value+"]";
            }
        } catch (NumberFormatException e) {
            return value;
        } catch (java.util.IllegalFormatException e) {
            return "["+e.getClass().getName()+": "+e.getMessage()+" \""+formatString+"\","+value+"]";
        }
    }

}
