package com.x5.template.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.Chunk;
import com.x5.template.TextFilter;

public class OnMatchFilter extends BasicFilter implements ChunkFilter
{
    @Override
    public String transformText(Chunk chunk, String text, String[] args)
    {
        String result = applyMatchTransform(chunk, text, args);
        // onmatch output is never null?
        ///return result == null ? "" : result;
        
        return result;
    }

    public String getFilterName()
    {
        return "onmatch";
    }
    
    private static String applyMatchTransform(Chunk context, String text, String[] args)
    {
        if (args == null) return text;
        if (args.length == 1 && args[0] != null && args[0].length() == 0) {
            return text;
        }
        
        for (int i=1; i<args.length; i+=2) {
            if (i+1 >= args.length) return text;
            String test = args[i];
            String value = args[i+1];
            
            if (test.equals("|nomatch|")) {
                return TextFilter.magicBraces(context, value);
            }
            
            if (text == null) continue; // won't ever match
            
            int patternStart = test.indexOf('/') + 1;
            int patternEnd = test.lastIndexOf('/');
            if (patternStart < 0 || patternStart == patternEnd) return text;
            
            boolean ignoreCase = false;
            boolean multiLine = false;
            boolean dotAll = false;
            
            String pattern = test.substring(patternStart,patternEnd);
            for (int c=test.length()-1; c>patternEnd; c--) {
                char option = test.charAt(c);
                if (option == 'i') ignoreCase = true;
                if (option == 'm') multiLine = true;
                if (option == 's') dotAll = true; // dot matches newlines too
            }
            
            if (multiLine) pattern = "(?m)" + pattern;
            if (ignoreCase) pattern = "(?i)" + pattern;
            if (dotAll) pattern = "(?s)" + pattern;

            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return TextFilter.magicBraces(context, value);
            }
        }
        
        // no match?
        // even a tag that's null, when passed into onmatch(), resolves to empty string
        return "";
    }

    /*
    private static String applyMatchTransform(Chunk context, String text, String[] args)
    {
        // TODO reimplement, args already parsed!
        // if nomatch clause, penultimate arg will == "|nomatch|"
        
        // scan for next regex, check for match, kick out on first match
        int cursor = 0;
        while (text != null && cursor < formatString.length() && formatString.charAt(cursor) != ')') {
            if (formatString.charAt(cursor) == ',') cursor++;
            if (formatString.charAt(cursor) == 'm') cursor++;
            if (formatString.charAt(cursor) == '/') cursor++;
            int regexEnd = RegexFilter.nextRegexDelim(formatString,cursor);
            if (regexEnd < 0) return text; // fatal, unmatched regex boundary

            String pattern = formatString.substring(cursor,regexEnd);

            // check for modifiers between regex end and comma
            int commaPos = formatString.indexOf(",",regexEnd+1);
            if (commaPos < 0) return text; // fatal, missing argument delimiter

            boolean ignoreCase = false;
            boolean multiLine = false;
            boolean dotAll = false;

            for (int i=commaPos-1; i>regexEnd; i--) {
                char option = formatString.charAt(i);
                if (option == 'i') ignoreCase = true;
                if (option == 'm') multiLine = true;
                if (option == 's') dotAll = true; // dot matches newlines too
            }

            if (multiLine) pattern = "(?m)" + pattern;
            if (ignoreCase) pattern = "(?i)" + pattern;
            if (dotAll) pattern = "(?s)" + pattern;

            // scan for a comma not preceded by a backslash
            int nextMatchPos = nextArgDelim(formatString,commaPos+1);
            if (nextMatchPos > 0) {
                cursor = nextMatchPos;
            } else {
                // scan for close-paren
                int closeParen = nextUnescapedDelim(")",formatString,commaPos+1);
                if (closeParen > 0) {
                    cursor = closeParen;
                } else {
                    cursor = formatString.length();
                }
            }

            if (matches(text,pattern)) {
                if (cursor == commaPos + 1) return "";
                String output = formatString.substring(commaPos+1,cursor);
                return magicBraces(context,output);
            }
        }

        // reached here?  no match
        int elseClause = formatString.lastIndexOf("nomatch(");
        if (elseClause > 0) {
            String output = formatString.substring(elseClause + "nomatch(".length());
            if (output.endsWith(")")) output = output.substring(0,output.length()-1);
            if (output.length() == 0) return output;
            return magicBraces(context,output);
        } else {
            // standard behavior without a nomatch clause is blank output
            return "";
        }
    }*/

}
