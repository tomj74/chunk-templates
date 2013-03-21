package com.x5.template.filters;

import com.x5.template.Chunk;

public class SHA1HexFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        return text == null ? null : sha1Hex(text);
    }

    public String getFilterName()
    {
        return "sha1";
    }
    
    public String[] getFilterAliases()
    {
        return new String[]{"sha1hex"};
    }
    
    public static String sha1Hex(String text)
    {
        return sha1(text, false);
    }

    public static String sha1Base64(String text)
    {
        return sha1(text, true);
    }

    public static String sha1(String text, boolean base64)
    {
        return MD5HexFilter.hashCrypt("SHA-1",text,base64);
    }


}
