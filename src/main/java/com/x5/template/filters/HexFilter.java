package com.x5.template.filters;

import com.x5.template.Chunk;

public class HexFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, FilterArgs args)
    {
        if (text == null) return null;

        String hex = null;
        try {
            hex = new java.math.BigInteger(1,text.getBytes("UTF-8")).toString(16);
        } catch (java.io.UnsupportedEncodingException e) {
            hex = new java.math.BigInteger(1,text.getBytes()).toString(16);
        }
        if (hex == null) return text;

        return hex;
    }

    public String getFilterName()
    {
        return "hex";
    }

}
