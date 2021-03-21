package com.x5.template;

import com.csvreader.CsvReader;
import com.x5.template.providers.TranslationsProvider;
import com.x5.util.JarResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;

/**
 * The DefaultTranslationsProvider loads translations from two-column csv files.
 * They can be on the filesystem or in the classpath.
 *
 * Most server-side apps will find it easiest to place them in a folder named
 * locale/ under a resources/ folder that your build copies into the release jar/war.
 *
 * (1) if chunk.localedb.path is defined:
 * check the filesystem for a file in that folder named xx_XX/translate.csv
 *
 * (2) check the classpath for a resource named /locale/xx_XX/translate.csv
 *
 * Where xx_XX is the locale code eg de_DE or en_US
 */
public class DefaultTranslationsProvider implements TranslationsProvider {
    private static Map<String, Map<String,String>> cache = new HashMap<String, Map<String,String>>();

    public DefaultTranslationsProvider() {
    }

    public Map<String, String> getTranslations(String localeCode) {
        if (cache.containsKey(localeCode)) {
            return cache.get(localeCode);
        }

        Map<String, String> translations = loadTranslations(localeCode);
        cache.put(localeCode, translations);
        return translations;
    }

    private Map<String, String> loadTranslations(String localeCode)
    {
        Map<String, String> translations = new HashMap<String, String>();

        // locate matching csv file and load translations
        try {
            InputStream in = locateLocaleDB(localeCode);
            if (in == null) return translations;

            Charset charset = grokLocaleDBCharset();
            CsvReader reader = new CsvReader(in,charset);
            reader.setUseComments(true); // ignore lines beginning with #

            String[] entry;

            while (reader.readRecord()) {
                entry = reader.getValues();

                if (entry != null && entry.length > 1 && entry[0] != null && entry[1] != null) {
                    String key = entry[0];
                    String localString = entry[1];
                    translations.put(key,localString);
                }
            }
        } catch (IOException e) {
            System.err.println("ERROR loading locale DB: "+localeCode);
            e.printStackTrace(System.err);
        }

        return translations;
    }

    private Charset grokLocaleDBCharset()
    {
        String override = System.getProperty("chunk.localedb.charset");
        if (override != null) {
            Charset charset = null;
            try {
                charset = Charset.forName(override);
            } catch (IllegalCharsetNameException e) {
            } catch (UnsupportedCharsetException e) {
            }
            if (charset != null) return charset;
        }

        try {
            return Charset.forName("UTF-8"); // sensible default
        } catch (Exception e) {
        }

        // ok fine, whatever you got.
        return Charset.defaultCharset();
    }

    @SuppressWarnings("rawtypes")
    private InputStream locateLocaleDB(String localeCode)
            throws java.io.IOException
    {
        // (1) if chunk.localedb.path is defined,
        // check there for a file named xx_XX/translate.csv
        String sysLocalePath = System.getProperty("chunk.localedb.path");
        if (sysLocalePath != null) {
            File folder = new File(sysLocalePath);
            if (folder.exists()) {
                File file = new File(folder, localeCode + "/translate.csv");
                if (file.exists()) {
                    return new FileInputStream(file);
                }
            }
        }

        // (2) check the classpath for a resource named /locale/xx_XX/translate.csv
        String path = "/locale/" + localeCode + "/translate.csv";
        InputStream in = this.getClass().getResourceAsStream(path);
        if (in != null) return in;

        // (2a) check the caller's class resources
        Class classInApp = TemplateSet.grokCallerClass();
        if (classInApp != null) {
            in = classInApp.getResourceAsStream(path);
            if (in != null) {
                return in;
            }
        }

        // (3a) TODO - use context to grok app's resource context
        // and check there (eg, should work inside servlet context)

        // (3) check inside jars on the classpath...
        String cp = System.getProperty("java.class.path");
        if (cp == null) return null;

        String[] jars = cp.split(":");
        if (jars == null) return null;

        for (String jar : jars) {
            if (jar.endsWith(".jar")) {
                in = JarResource.peekInsideJar("jar:file:"+jar, path);
                if (in != null) return in;
            }
        }

        // (4) give up!
        return null;
    }

}
