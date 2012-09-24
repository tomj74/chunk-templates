package com.x5.template.filters;

import java.util.Locale;
import java.util.MissingResourceException;

import com.x5.template.Chunk;

public class FormatFilter extends BasicFilter
{
    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return null;
        
        String fmtString = null;
        // get the entire, unparsed string from inside the parens
        if (args != null && args.length > 0) fmtString = args[0];
        
        if (fmtString == null) return "";
        
        String localeCode = null;
        if (chunk != null && chunk.getLocale() != null) {
            localeCode = chunk.getLocale().toString();
        }
        
        return applyFormatString(text, fmtString, localeCode);
    }
    
    public String getFilterName()
    {
        return "sprintf";
    }
    
    private static String applyFormatString(String text, String formatString, String localeCode)
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

        return formatNumberFromString(formatString, text, localeCode);
    }

    public static String formatNumberFromString(String formatString, String value)
    {
        return formatNumberFromString(formatString, value, null);
    }
    
    public static Locale getLocale(String localeCode)
    {
        if (localeCode != null && localeCode.contains("_")) {
            String[] langAndCountry = localeCode.split("_");
            if (langAndCountry.length > 1) {
                String lang    = langAndCountry[0];
                String country = langAndCountry[1];
                if (lang != null && lang.trim().length() > 0) {
                    if (country != null && country.trim().length() > 0) {
                        Locale locale = new Locale(lang, country);
                        // confirm that this is a valid locale
                        try {
                            if (locale.getISO3Country() != null) {
                                if (locale.getISO3Language() != null) {
                                    return locale;
                                }
                            }
                        } catch (MissingResourceException e) {
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    public static String formatNumberFromString(String formatString, String value, String localeCode)
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
            
            Locale locale = getLocale(localeCode);
            
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
