package com.x5.template.filters;

import com.x5.template.Chunk;

public interface ChunkFilter
{
    public Object applyFilter(Chunk chunk, String text, String[] args);
    public Object applyFilter(Chunk chunk, Object obj, String[] args);
    public String getFilterName();
    public String[] getFilterAliases();
}
