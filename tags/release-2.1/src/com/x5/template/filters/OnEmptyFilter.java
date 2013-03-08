package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.TextFilter;

public class OnEmptyFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        String swapFor = null;
        
        if (args != null && args.length > 0) swapFor = args[0];
        if (swapFor == null) return null;
        
        // null and empty string are both considered empty
        return (text == null || text.trim().length() == 0) ? TextFilter.magicBraces(chunk, swapFor) : text;
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
