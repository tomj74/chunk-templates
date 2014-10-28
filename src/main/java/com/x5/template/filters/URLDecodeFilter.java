package com.x5.template.filters;

import com.x5.template.Chunk;

public class URLDecodeFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return null;

        // url-decode
        try {
            return java.net.URLDecoder.decode(text, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return text;
        }
    }

    public String getFilterName()
    {
        return "urldecode";
    }
}
