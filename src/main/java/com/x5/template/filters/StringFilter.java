package com.x5.template.filters;

import com.x5.template.Chunk;

public class StringFilter extends BasicFilter
{

    @Override
    public String transformText(Chunk chunk, String text, FilterArgs args)
    {
        // basic filter converts argument to string by default.  no-op.
        return text;
    }

    @Override
    public String getFilterName()
    {
        return "string";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"str"};
    }
}
