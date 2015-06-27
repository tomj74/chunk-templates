package com.x5.template.filters;

import java.util.Arrays;
import java.util.List;

import com.x5.template.Chunk;
import com.x5.util.ObjectDataMap;

public abstract class ObjectFilter implements ChunkFilter
{
    public Object applyFilter(Chunk chunk, String text, FilterArgs args)
    {
        return text;
    }

    public abstract String getFilterName();
    public String[] getFilterAliases()
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object applyFilter(Chunk chunk, Object object, FilterArgs args)
    {
        if (object instanceof ObjectDataMap) {
            // expose inner object for direct manipulation
            object = ((ObjectDataMap)object).unwrap();
        }
        return transformObject(chunk, object, args);
    }

    @SuppressWarnings("rawtypes")
    public abstract Object transformObject(Chunk chunk, Object object, FilterArgs args);

    public ObjectFilter() {}

}
