package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.util.LiteXml;

public class UnescapeXMLFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, FilterArgs args)
    {
        if (text == null) return null;

        return LiteXml.unescapeXML(text);
    }

    public String getFilterName()
    {
        return "unescape";
    }

    public String[] getFilterAliases()
    {
        return new String[]{
            "unhtml",
            "unxml",
            "xmlunescape",
            "htmlunescape",
            "unescapexml",
            "unescapehtml",
        };
    }

}
