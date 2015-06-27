package com.x5.template.filters;

import com.x5.template.Chunk;

public class URLEncodeFilter extends BasicFilter implements ChunkFilter
{
    public String transformText(Chunk chunk, String text, FilterArgs args)
    {
        if (text == null) return null;

        // url-encode
        try {
            return java.net.URLEncoder.encode(text,"UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return text;
        }
    }

    public String getFilterName()
    {
        return "urlencode";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"url"};
    }
}
