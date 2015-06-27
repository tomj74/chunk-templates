package com.x5.template.filters;

import com.x5.template.Chunk;

public class Base64DecodeFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, FilterArgs args)
    {
        if (text == null) return null;

        return base64Decode(text);
    }

    public String getFilterName()
    {
        return "base64decode";
    }

    public static String base64Decode(String text)
    {
        byte[] decoded = null;

        // use packaged com.x5.util.Base64
        byte[] textBytes;
        try {
            textBytes = text.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            textBytes = text.getBytes();
        }
        decoded = com.x5.util.Base64.decode(textBytes, 0, textBytes.length);

        if (decoded == null) {
            // on failure -- return original bytes
            return text;
        } else {
            // convert decoded bytes to string
            try {
                return new String(decoded, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                return new String(decoded);
            }
        }
    }



}
