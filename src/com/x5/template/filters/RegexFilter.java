package com.x5.template.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.Chunk;
import com.x5.template.TextFilter;

public class RegexFilter extends BasicFilter implements ChunkFilter
{
    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return text;
        
        String regex = null;
        if (args != null && args.length > 0) regex = args[0];
        
        if (regex == null) return text;
        
        return applyRegex(text, regex);
    }

    public String getFilterName()
    {
        return "s";
    }

    public static int nextRegexDelim(String regex, int searchFrom)
    {
        return TextFilter.nextUnescapedDelim("/",regex,searchFrom);
    }

    public static String applyRegex(String text, String regex)
    {
        // parse perl-style regex a la s/find/replace/gmi
        int patternStart = 1;
        if (regex.charAt(0) == 's') patternStart = 2;
        int patternEnd = nextRegexDelim(regex, patternStart);

        // if the regex is not legal (missing delimiters), bail out
        if (patternEnd < 0) return text;

        int replaceEnd = nextRegexDelim(regex, patternEnd+1);
        if (replaceEnd < 0) return text;

        boolean greedy = false;
        boolean ignoreCase = false;
        boolean multiLine = false;
        boolean dotAll = false;

        for (int i=regex.length()-1; i>replaceEnd; i--) {
            char option = regex.charAt(i);
            if (option == 'g') greedy = true;
            if (option == 'i') ignoreCase = true;
            if (option == 'm') multiLine = true;
            if (option == 's') dotAll = true; // dot matches newlines too
        }

        String pattern = regex.substring(patternStart,patternEnd);
        String replaceWith = regex.substring(patternEnd+1,replaceEnd);
        replaceWith = parseRegexEscapes(replaceWith);
        // re-escape escaped backslashes, ie \ -> \\
        replaceWith = Chunk.findAndReplace(replaceWith,"\\","\\\\");

        if (multiLine) pattern = "(?m)" + pattern;
        if (ignoreCase) pattern = "(?i)" + pattern;
        if (dotAll) pattern = "(?s)" + pattern;

        boolean caseConversions = false;
        if (replaceWith.matches(".*\\\\[UL][\\$\\\\]\\d.*")) {
            // this monkey business marks up case-conversion blocks
            // since java's regex engine doesn't support perl-style
            // case-conversion.  but we do :)
            caseConversions = true;
            replaceWith = replaceWith.replaceAll("\\\\([UL])[\\$\\\\](\\d)", "!$1@\\$$2@$1!");
        }
        
        try {
            String result = null;
            
            if (greedy) {
                result = text.replaceAll(pattern,replaceWith);
            } else {
                result = text.replaceFirst(pattern,replaceWith);
            }
            
            if (caseConversions) {
                return applyCaseConversions(result);
            } else {
                return result;
            }
        } catch (IndexOutOfBoundsException e) {
            return text + "[REGEX "+regex+" Error: "+e.getMessage()+"]";
        }
    }
    
    private static String applyCaseConversions(String result)
    {
        StringBuilder x = new StringBuilder();
        
        Matcher m = Pattern.compile("!U@(.*?)@U!").matcher(result);
        int last = 0;
        while (m.find()) {
            x.append(result.substring(last, m.start()));
            x.append(m.group(1).toUpperCase());
            last = m.end();
        }
        if (last > 0) {
            x.append(result.substring(last));
            result = x.toString();
            x = new StringBuilder();
            last = 0;
        }
        
        m = Pattern.compile("!L@(.*?)@L!").matcher(result);
        while (m.find()) {
            x.append(result.substring(last, m.start()));
            x.append(m.group(1).toLowerCase());
            last = m.end();
        }
        if (last > 0) {
            x.append(result.substring(last));
            return x.toString();
        } else {
            return result;
        }
    }

    private static String parseRegexEscapes(String str)
    {
        if (str == null) return str;

        char[] strArr = str.toCharArray();
        boolean escape = false;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < strArr.length; ++i) {
            if (escape) {
                if (strArr[i] == 'b') {
                    buf.append('\b');
                } else if (strArr[i] == 't') {
                    buf.append('\t');
                } else if (strArr[i] == 'n') {
                    buf.append('\n');
                } else if (strArr[i] == 'r') {
                    buf.append('\r');
                } else if (strArr[i] == 'f') {
                    buf.append('\f');
                } else if (strArr[i] == 'U') {
                    buf.append("\\U");
                } else if (strArr[i] == 'L') {
                    buf.append("\\L");
                } else if (strArr[i] == 'u') {
                    // Unicode escape
                    int utf = Integer.parseInt(str.substring(i + 1, i + 5), 16);
                    buf.append((char)utf);
                    i += 4;
                } else if (Character.isDigit(strArr[i])) {
                    // Octal escape
                    int j = 0;
                    for (j = 1; (j < 2) && (i + j < strArr.length); ++j) {
                        if (!Character.isDigit(strArr[i+j]))
                            break;
                    }
                    int octal = Integer.parseInt(str.substring(i, i + j), 8);
                    buf.append((char)octal);
                    i += j-1;
                } else {
                    buf.append(strArr[i]);
                }
                escape = false;
            } else if (strArr[i] == '\\') {
                escape = true;
            } else {
                buf.append(strArr[i]);
            }
        }
        return buf.toString();
    }
    
    private static final Pattern INNOCUOUS_CHARS = Pattern.compile("^[-A-Za-z0-9_ <>\"']*$");
    
    public static String escapeRegex(String x)
    {
        Matcher m = INNOCUOUS_CHARS.matcher(x);
        if (m.find()) return x; // nothing to escape
        
        // nothing should leave this sub with its special regex meaning preserved
        StringBuilder noSpecials = new StringBuilder();
        for (int i=0; i<x.length(); i++) {
            char c = x.charAt(i);
            if ((c == ' ') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                           || (c >= '0' && c <= '9')) {
                // do not escape A-Z a-z 0-9, spaces
                noSpecials.append(c);
            } else {
                noSpecials.append("\\");
                noSpecials.append(c);
            }
        }
        return noSpecials.toString();
    }

}
