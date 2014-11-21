package com.x5.template.filters;

import com.x5.template.Chunk;

public class EscapeQuotesFilter extends BasicFilter implements ChunkFilter
{
    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text != null) {
            text = Chunk.findAndReplace(text,"\\","\\\\");
            text = Chunk.findAndReplace(text,"\"","\\\"");
            text = Chunk.findAndReplace(text,"'","\\'");
        }
        return text;
    }

    public String getFilterName()
    {
        return "escapequotes";
    }

    public String[] getFilterAliases()
    {
        return new String[]{
            "quotedstring",
            "qs",
            "quoted",
        };
    }

}
