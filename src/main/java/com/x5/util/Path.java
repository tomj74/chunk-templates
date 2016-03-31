package com.x5.util;

public class Path
{
    public static String ensureTrailingFileSeparator(String path)
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

    public static String ensureTrailingPathSeparator(String path)
    {
        if (path != null) {
            char lastChar = path.charAt(path.length()-1);
            if (lastChar != '/') {
                return path + '/';
            }
        }
        return path;
    }
}
