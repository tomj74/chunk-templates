package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.Filter;

public class BooleanFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, FilterArgs arg)
    {
        boolean b = false;
        if (text != null && text.trim().length() > 0) {
            try {
                float f = Float.valueOf(text);
                b = f != 0.0;
            } catch (NumberFormatException e) {
                if (!text.trim().equalsIgnoreCase("FALSE")) {
                    b = true;
                }
            }
        }

        return b ? Chunk.TRUE : null;
    }

    public String getFilterName()
    {
        return "bool";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"boolean"};
    }
}
