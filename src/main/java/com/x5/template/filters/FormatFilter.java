package com.x5.template.filters;

import java.util.Locale;

import com.x5.template.Chunk;
import com.x5.template.ChunkLocale;

public class FormatFilter extends BasicFilter
{
    public String transformText(Chunk chunk, String text, FilterArgs arg)
    {
        String fmtString = arg.getUnparsedArgs();

        if (fmtString == null) return "";

        ChunkLocale locale = null;
        if (chunk != null) {
            locale = chunk.getLocale();
        }

        return applyFormatString(text, fmtString, locale);
    }

    public String getFilterName()
    {
        return "sprintf";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"fmt","format"};
    }

    private static String applyFormatString(String text, String formatString, ChunkLocale locale)
    {
        // strip quotes if arg is quoted
        char first = formatString.charAt(0);
        char last = formatString.charAt(formatString.length()-1);
        if (first == last && (first == '\'' || first == '"')) {
            formatString = formatString.substring(1, formatString.length()-1);
        }

        return formatNumberFromString(formatString, text, locale);
    }

    public static String formatNumberFromString(String formatString, String value)
    {
        return formatNumberFromString(formatString, value, null);
    }

    public static Locale getJavaLocale(ChunkLocale locale)
    {
        if (locale == null) return null;
        return locale.getJavaLocale();
    }

    public static String formatNumberFromString(String formatString, String value, ChunkLocale chunkLocale)
    {
        // This assumes that the expr *ends* with eg %s or %d or %.3f
        // and the final char is the intended number format.
        //
        // Instead, should seek (first? final?) unescaped % and extract
        // the format from that group, allowing more format-template
        // to exist after the number output. TODO.
        //
        char expecting = formatString.charAt(formatString.length()-1);
        try {

            Locale locale = getJavaLocale(chunkLocale);

            if ("bB".indexOf(expecting) > -1) {
                boolean b = false;
                if (value != null && value.trim().length() > 0) {
                    try {
                        float f = Float.valueOf(value);
                        b = f != 0.0;
                    } catch (NumberFormatException e) {
                        if (!value.trim().equalsIgnoreCase("FALSE")) {
                            b = true;
                        }
                    }
                }
                return String.format(locale, formatString, b);
            }

            // the remaining cases will not accept null input
            if (value == null) {
                return null;
            }

            if ("sS".indexOf(expecting) > -1) {
                return String.format(locale, formatString, value);
            } else if ("eEfgGaA".indexOf(expecting) > -1) {
                float f = Float.valueOf(value);
                return String.format(locale, formatString, f);
            } else if ("doxX".indexOf(expecting) > -1) {
                if (value.trim().startsWith("#")) {
                    long l = Long.parseLong(value.trim().substring(1),16);
                    return String.format(locale, formatString, l);
                } else if (value.trim().startsWith("0X") || value.trim().startsWith("0x")) {
                    long l = Long.parseLong(value.trim().substring(2),16);
                    return String.format(locale, formatString, l);
                } else {
                    float f = Float.valueOf(value);
                    return String.format(locale, formatString, (long)f);
                }
            } else if ("cC".indexOf(expecting) > -1) {
                if (value.trim().startsWith("0X") || value.trim().startsWith("0x")) {
                    int i = Integer.parseInt(value.trim().substring(2),16);
                    return String.format(locale, formatString, (char)i);
                } else {
                    float f = Float.valueOf(value);
                    return String.format(locale, formatString, (char)f);
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
