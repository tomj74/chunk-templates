package com.x5.template.filters;

import com.x5.template.Chunk;

public class HexUpperFilter extends HexFilter implements ChunkFilter
{
    public String transformText(Chunk chunk, String text, String[] args)
    {
        String hex = super.transformText(chunk, text, args);
        return hex == null ? null : hex.toUpperCase();
    }
    
    public String getFilterName()
    {
        return "HEX";
    }
}
