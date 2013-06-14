package com.x5.template.filters;

import com.x5.template.Chunk;

public class SHA1Base64Filter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        return text == null ? null : SHA1HexFilter.sha1Base64(text);
    }

    public String getFilterName()
    {
        return "sha1base64";
    }
    
    public String[] getFilterAliases()
    {
        return new String[]{"sha1b64"};
    }

}
