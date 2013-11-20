package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.Filter;

public class AlternateFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return null;
        if (args == null) return text;

        try {
            int x = Integer.parseInt(text);
            String output = null;

            if (x % 2 == 0) {
                if (args.length == 1) {
                    output = args[0];
                } else {
                    output = args[1];
                }
            } else {
                if (args.length >= 3) {
                    output = args[2];
                }
            }

            // tag-ify if necessary
            return Filter.magicBraces(chunk, output);

        } catch (NumberFormatException e) {
            return text;
        }
    }

    public String getFilterName()
    {
        return "alternate";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"evenodd"};
    }
}
