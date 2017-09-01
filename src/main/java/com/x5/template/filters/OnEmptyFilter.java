package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.Filter;

public class OnEmptyFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, FilterArgs arg)
    {
        String swapFor = null;

        String[] args = arg.getFilterArgs(chunk);

        if (args != null && args.length > 0) swapFor = args[0];
        if (swapFor == null) return null;

        // null and empty string and whitespace-only are all considered empty
        return (text == null || text.trim().length() == 0) ? FilterArgs.magicBraces(chunk, swapFor, arg) : text;
    }

    public String getFilterName()
    {
        return "onempty";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"else"};
    }

}
