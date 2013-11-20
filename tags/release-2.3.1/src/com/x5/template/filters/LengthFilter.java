package com.x5.template.filters;

import java.util.Collection;

import com.x5.template.Chunk;

public class LengthFilter implements ChunkFilter
{
    public Object applyFilter(Chunk chunk, String text, String[] args)
    {
        if (text == null) return "0";
        return Integer.toString(text.length());
    }

    @SuppressWarnings("rawtypes")
    public Object applyFilter(Chunk chunk, Object obj, String[] args)
    {
        if (obj == null) return "0";
        int len;
        if (obj instanceof Collection) {
            len = ((Collection)obj).size();
        } else if (obj instanceof Object[]) {
            len = ((Object[])obj).length;
        } else {
            len = obj.toString().length();
        }
        return Integer.toString(len);
    }

    public String getFilterName()
    {
        return "length";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"len"};
    }

}
