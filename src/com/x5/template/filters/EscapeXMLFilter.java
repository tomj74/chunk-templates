package com.x5.template.filters;

import com.x5.template.Chunk;

public class EscapeXMLFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return null;
        text = Chunk.findAndReplace(text,"&","&amp;");
        text = Chunk.findAndReplace(text,"<","&lt;");
        text = Chunk.findAndReplace(text,">","&gt;");
        text = Chunk.findAndReplace(text,"\"","&quot;");
        text = Chunk.findAndReplace(text,"'","&apos;");
        return text;
    }

    public String getFilterName()
    {
        return "xml";
    }
    
    public String[] getFilterAliases()
    {
        return new String[]{
            "html",
            "xmlescape",
            "htmlescape",
            "xmlesc",
            "htmlesc"
        };
    }

}
