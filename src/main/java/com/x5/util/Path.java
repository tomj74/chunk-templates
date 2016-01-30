package com.x5.util;

public class Path
{
    public static String ensureTrailingSeparator(String path)
    {
        if (path != null) {
            char lastChar = path.charAt(path.length()-1);
            char fs = System.getProperty("file.separator").charAt(0);
            if (lastChar != '\\' && lastChar != '/' && lastChar != fs) {
                return path + fs;
            }
        }
        return path;
    }
}
