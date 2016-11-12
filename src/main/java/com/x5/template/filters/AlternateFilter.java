package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.Filter;

public class AlternateFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, FilterArgs arg)
    {
        if (text == null) return null;

        String[] args = arg.getFilterArgs();
        if (args == null) return text;

        try {
            int x = Integer.parseInt(text);
            String output = null;

            if (x % 2 == 0) {
                output = args[0];
            } else {
                if (args.length >= 2) {
                    output = args[1];
                }
            }

            // tag-ify if necessary
            return FilterArgs.magicBraces(chunk, output, arg);

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
