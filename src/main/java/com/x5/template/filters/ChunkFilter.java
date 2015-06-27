package com.x5.template.filters;

import com.x5.template.Chunk;

public interface ChunkFilter
{
    public Object applyFilter(Chunk chunk, String text, FilterArgs args);
    public Object applyFilter(Chunk chunk, Object obj, FilterArgs args);
    public String getFilterName();
    public String[] getFilterAliases();
}
