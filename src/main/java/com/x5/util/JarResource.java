package com.x5.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarResource
{
    public static InputStream peekInsideJar(String jar, String resourcePath)
    {
        String resourceURL = jar + "!" + resourcePath;
        try {
            URL url = new URL(resourceURL);
            InputStream in = url.openStream();
            if (in != null) return in;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (AccessControlException e) {
        }

        try {
            // strip URL nonsense to get valid local path
            String zipPath = jar.replaceFirst("^jar:file:", "");
            // strip leading slash from resource path
            String zipResourcePath = resourcePath.replaceFirst("^/","");
            ZipFile zipFile = new ZipFile(zipPath);
            ZipEntry entry = zipFile.getEntry(zipResourcePath);
            if (entry != null) {
                return zipFile.getInputStream(entry);
            }
        } catch (java.io.IOException e) {
        } catch (AccessControlException e) {
        }

        return null;
    }
}
