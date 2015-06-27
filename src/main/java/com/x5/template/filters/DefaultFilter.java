package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.Filter;

public class DefaultFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, FilterArgs arg)
    {
        String swapFor = null;

        String[] args = arg.getFilterArgs();

        if (args != null && args.length > 0) swapFor = args[0];
        if (swapFor == null) return null;

        // only fires if expr is null (completely undefined)
        return (text == null) ? FilterArgs.magicBraces(chunk, swapFor) : text;
    }

    public String getFilterName()
    {
        return "default";
    }

}
