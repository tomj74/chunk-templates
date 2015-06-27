package com.x5.template.filters;

import java.util.Locale;

import com.x5.template.Chunk;
import com.x5.template.ChunkLocale;

public class LetterCaseFilter extends BasicFilter implements ChunkFilter
{
    int OP_UPPER = 0;
    int OP_LOWER = 1;
    int OP_CAPITALIZE = 2;
    int OP_TITLE = 4;

    public String transformText(Chunk chunk, String text, FilterArgs args)
    {
        if (text == null) return null;

        int op = OP_UPPER;
        if (args != null) {
            String filterName = args.getFilterName();
            if (filterName.equals("lower") || filterName.equals("lc")) {
                op = OP_LOWER;
            } else if (filterName.equals("capitalize") || filterName.equals("cap")) {
                op = OP_CAPITALIZE;
            } else if (filterName.equals("title")) {
                op = OP_TITLE;
            }
        }

        ChunkLocale locale = (chunk == null ? null : chunk.getLocale());
        Locale javaLocale = null;
        if (locale != null) javaLocale = locale.getJavaLocale();

        if (javaLocale == null) {
            if (op == OP_UPPER) {
                return text.toUpperCase();
            } else if (op == OP_LOWER) {
                return text.toLowerCase();
            } else if (op == OP_CAPITALIZE) {
                return capitalize(text,null,false);
            } else if (op == OP_TITLE) {
                return capitalize(text,null,true);
            }
        } else {
            if (op == OP_UPPER) {
                return text.toUpperCase(javaLocale);
            } else if (op == OP_LOWER) {
                return text.toLowerCase(javaLocale);
            } else if (op == OP_CAPITALIZE) {
                return capitalize(text,javaLocale,false);
            } else if (op == OP_TITLE) {
                return capitalize(text,javaLocale,true);
            }
        }
        // will never fall through to here
        return null;
    }

    private String capitalize(String text, Locale javaLocale, boolean lcFirst)
    {
        if (lcFirst) text = javaLocale == null ? text.toLowerCase() : text.toLowerCase(javaLocale);
        char[] chars = text.toCharArray();
        boolean found = false;
        for (int i=0; i<chars.length; i++) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = javaLocale == null ? Character.toUpperCase(chars[i]) : Character.toString(chars[i]).toUpperCase(javaLocale).charAt(0);
                found = true;
            } else if (Character.isWhitespace(chars[i]) || chars[i]=='.' || chars[i]=='\'') {
                found = false;
            }
        }
        return String.valueOf(chars);
    }

    public String getFilterName()
    {
        return "upper";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"uc","lower","lc","capitalize","cap","title"};
    }
}
