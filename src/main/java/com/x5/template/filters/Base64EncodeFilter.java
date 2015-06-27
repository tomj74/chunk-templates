package com.x5.template.filters;

import com.x5.template.Chunk;

public class Base64EncodeFilter extends BasicFilter implements ChunkFilter
{
    public String transformText(Chunk chunk, String text, FilterArgs args)
    {
        if (text == null) return null;
        return base64(text);
    }

    public String getFilterName()
    {
        return "base64";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"base64encode"};
    }

    public static String base64(String text)
    {
        try {
            byte[] textBytes = text.getBytes("UTF-8");
            return base64(textBytes);
        } catch (java.io.UnsupportedEncodingException e) {
            return base64(text.getBytes());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String base64(byte[] bytes)
    {
        // try base 64 using two potentially available 3rd party classes
        try {
            // 1. would this really compile if sun.misc.BASE64Encoder weren't on the classpath?
            // 2. why is BASE in all caps?  is it an acronym?
            sun.misc.BASE64Encoder encoder =
                (sun.misc.BASE64Encoder) Class.forName("sun.misc.BASE64Encoder").newInstance();
            return encoder.encode(bytes);
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        // hmm, that didn't work.  maybe com.x5.util.Base64 is available?
        try {
            Class b64 = Class.forName("com.x5.util.Base64");
            Class[] paramTypes = new Class[] { byte[].class };
            java.lang.reflect.Method encode = b64.getMethod("encodeBytes", paramTypes);
            String b64text = (String) encode.invoke(null, new Object[]{ bytes });
            return b64text;
        } catch (ClassNotFoundException e2) {
        } catch (NoSuchMethodException e2) {
        } catch (IllegalAccessException e2) {
        } catch (java.lang.reflect.InvocationTargetException e2) {
        }

        // on failure -- return original bytes
        try {
            return new String(bytes,"UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(bytes);
        }
    }

}
