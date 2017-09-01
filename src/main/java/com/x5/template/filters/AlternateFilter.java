package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.Filter;

public class AlternateFilter extends ObjectFilter implements ChunkFilter
{

    public Object transformObject(Chunk chunk, Object obj, FilterArgs arg)
    {
        if (obj == null) return null;

        Object[] args = arg.getArgsAsObjects(chunk);
        if (args == null) return obj;

        int x;
        try {
            x = Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return obj;
        }

        Object output = null;

        if (x % 2 == 0) {
            output = args[0];
        } else {
            if (args.length >= 2) {
                output = args[1];
            }
        }

        return output;
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
