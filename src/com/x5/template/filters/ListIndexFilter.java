package com.x5.template.filters;

import java.util.List;

import com.x5.template.Chunk;

public class ListIndexFilter extends ListFilter
{
    public String getFilterName()
    {
        return "get";
    }

    public Object transformList(Chunk chunk, List list, String[] args)
    {
        if (list == null || args.length < 1) return null;

        int i = Integer.parseInt(args[0]);
        if (i < 0) {
            // permit negative indices
            i = list.size() + i;
        }
        if (i < 0 || i >= list.size()) {
            return null;
        } else {
            return list.get(i);
        }
    }

}
