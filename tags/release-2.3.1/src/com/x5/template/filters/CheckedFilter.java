package com.x5.template.filters;

import com.x5.template.Chunk;

public class CheckedFilter extends BasicFilter implements ChunkFilter
{
    public String transformText(Chunk chunk, String text, String[] args)
    {
        return text == null ? null : SelectedFilter.checked(chunk, text, args);
    }

    public String getFilterName()
    {
        return "checked";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"check"};
    }

}
