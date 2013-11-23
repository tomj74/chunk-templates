package com.x5.template.filters;

import com.x5.template.Chunk;

public class OrdinalSuffixFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return null;
        return ordinalSuffix(text);
    }

    public String getFilterName()
    {
        return "th";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"ord","ordsuffix"};
    }

    private static String ordinalSuffix(String num)
    {
        if (num == null) return null;
        int x = Integer.parseInt(num);
        int mod100 = x % 100;
        int mod10 = x % 10;
        if (mod100 - mod10 == 10) {
            return num + "th";
        } else {
            switch (mod10) {
              case 1: return num + "st";
              case 2: return num + "nd";
              case 3: return num + "rd";
              default: return num + "th";
            }
        }
    }
}
