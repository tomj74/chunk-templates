package com.x5.template.filters;

import com.x5.template.Chunk;

public class MD5Base64Filter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        return text == null ? null : MD5HexFilter.md5Base64(text);
    }

    public String getFilterName()
    {
        return "md5base64";
    }
    
    public String[] getFilterAliases()
    {
        return new String[]{"md5b64"};
    }

}
