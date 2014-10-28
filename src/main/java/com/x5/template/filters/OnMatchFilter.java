package com.x5.template.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.Chunk;
import com.x5.template.Filter;

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
                return Filter.magicBraces(context, value);
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
                return Filter.magicBraces(context, value);
            }
        }

        // no match?
        // even a tag that's null, when passed into onmatch(), resolves to empty string
        return "";
    }

}
