package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.TextFilter;

public class OnDefinedFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return null;
        
        String swapFor = null;
        
        if (args != null && args.length > 0) swapFor = args[0];
        if (swapFor == null) return null;
        
        // empty string is considered not-defined
        return (text.trim().length() == 0) ? "" : TextFilter.magicBraces(chunk, swapFor);
    }

    public String getFilterName()
    {
        return "ondefined";
    }

}
