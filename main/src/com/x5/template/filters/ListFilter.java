package com.x5.template.filters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.x5.template.Chunk;

public abstract class ListFilter implements ChunkFilter
{
    public Object applyFilter(Chunk chunk, String text, String[] args)
    {
        // transform text input into a list of length one
        return applyFilter(chunk, new String[]{text}, args);
    }

    public abstract String getFilterName();
    public String[] getFilterAliases()
    {
        return null;
    }

    public Object applyFilter(Chunk chunk, Object object, String[] args)
    {
        List<Object> list = null;
        if (object instanceof List) {
            // preserve List
            list = (List<Object>)object;
        } else if (object instanceof Object[]) {
            // turn arrays into List
            list = Arrays.asList((Object[])object);
        } else {
            // turn single objects into List of length one
            list = Arrays.asList(object);
        }
        return transformList(chunk, list, args);
    }

    public abstract Object transformList(Chunk chunk, List list, String[] args);

    public ListFilter() {}

}
