package com.x5.template.filters;

import java.util.Locale;

import com.x5.template.Chunk;
import com.x5.template.ChunkLocale;

public class LetterCaseFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return null;

        boolean isUpper = true;
        if (args != null && (args[0].equals("lower") || args[0].equals("lc"))) {
            isUpper = false;
        }

        ChunkLocale locale = (chunk == null ? null : chunk.getLocale());
        Locale javaLocale = null;
        if (locale != null) javaLocale = locale.getJavaLocale();

        if (javaLocale == null) {
            return isUpper ? text.toUpperCase() : text.toLowerCase();
        } else {
            if (isUpper) {
                return text.toUpperCase(javaLocale);
            } else {
                return text.toLowerCase(javaLocale);
            }
        }
    }

    public String getFilterName()
    {
        return "upper";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"uc","lower","lc"};
    }
}
