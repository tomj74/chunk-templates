package com.x5.template.filters;

import com.x5.template.Chunk;

public class Base64DecodeFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return null;
        
        return base64Decode(text);
    }

    public String getFilterName()
    {
        return "base64decode";
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String base64Decode(String text)
    {
        byte[] decoded = null;
        // try base 64 using two potentially available 3rd party classes
        try {
            // 1. would this really compile if sun.misc.BASE64Encoder weren't on the classpath?
            // 2. why is BASE in all caps?  is it an acronym?
            sun.misc.BASE64Decoder decoder =
                (sun.misc.BASE64Decoder) Class.forName("sun.misc.BASE64Decoder").newInstance();
            decoded = decoder.decodeBuffer(text);
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (java.io.IOException e) {
        }

        if (decoded == null) {
            // hmm, that didn't work.  maybe com.x5.util.Base64 is available?
            byte[] textBytes;
            try {
                textBytes = text.getBytes("UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                textBytes = text.getBytes();
            }
            try {
                Class b64 = Class.forName("com.x5.util.Base64");
                Class[] paramTypes = new Class[] { byte[].class, Integer.TYPE, Integer.TYPE };
                java.lang.reflect.Method decode = b64.getMethod("decode", paramTypes);
                decoded = (byte[]) decode.invoke(null, new Object[]{ textBytes, new Integer(0), new Integer(textBytes.length) });
            } catch (ClassNotFoundException e2) {
            } catch (NoSuchMethodException e2) {
            } catch (IllegalAccessException e2) {
            } catch (java.lang.reflect.InvocationTargetException e2) {
            }
        }

        if (decoded == null) {
            // on failure -- return original bytes
            return text;
        } else {
            // convert decoded bytes to string
            try {
                return new String(decoded,"UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                return new String(decoded);
            }
        }
    }



}
