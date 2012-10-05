package com.x5.template;

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.Format;

import com.csvreader.CsvReader;

public class ChunkLocale
{
    private String localeCode;
    private HashMap<String,String> translations;
    
    private static HashMap<String,ChunkLocale> locales = new HashMap<String,ChunkLocale>();
    
    public static ChunkLocale getInstance(String localeCode, Chunk context)
    {
        ChunkLocale instance = locales.get(localeCode);
        if (instance != null) {
            return instance;
        } else {
            instance = new ChunkLocale(localeCode, context);
            locales.put(localeCode,instance);
            return instance;
        }
    }
    
    public static void registerLocale(String localeCode, String[] translations)
    {
        // this is mainly here just for testing.
        ChunkLocale instance = new ChunkLocale(localeCode, translations);
        locales.put(localeCode, instance);
    }
    
    private ChunkLocale(String localeCode, Chunk context)
    {
        this.localeCode = localeCode;
        loadTranslations(context);
    }
    
    private ChunkLocale(String localeCode, String[] strings)
    {
        // this is mainly here just for testing.
        this.localeCode = localeCode;
        if (strings != null && strings.length > 1) {
            this.translations = new HashMap<String,String>();
            for (int i=0; i+1<strings.length; i++) {
                String a = strings[i];
                String b = strings[i+1];
                translations.put(a,b);
            }
        }
    }
    
    private void loadTranslations(Chunk context)
    {
        // locate matching csv file and load translations
        try {
            InputStream in = locateLocaleDB(context);
            if (in == null) return;
            
            Charset charset = grokLocaleDBCharset();
            CsvReader reader = new CsvReader(in,charset);
            reader.setUseComments(true); // ignore lines beginning with #
            
            String[] entry;
            entry = reader.getValues();
            
            translations = new HashMap<String,String>();
            
            while (entry != null) {
                if (entry.length > 1 && entry[0] != null && entry[1] != null) {
                    String key = entry[0];
                    String localString = entry[1];
                    translations.put(key,localString);
                }
                entry = reader.readRecord() ? reader.getValues() : null;
            }
        } catch (IOException e) {
            System.err.println("ERROR loading locale DB: "+localeCode);
            e.printStackTrace(System.err);
        }
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
    
    private InputStream locateLocaleDB(Chunk context)
    throws java.io.IOException
    {
        // (1) if localePath is defined, check there for a file named xx_XX/translate.csv
        String localePath = System.getProperty("chunk.localedb.path");
        if (localePath != null) {
            File folder = new File(localePath);
            if (folder.exists()) {
                File file = new File(folder,localeCode + "/translate.csv");
                if (file.exists()) {
                    return new FileInputStream(file);
                }
            }
        }
        
        // (2) check the classpath for a resource named /locale/xx_XX/translate.csv
        String path = "/locale/" + localeCode + "/translate.csv";
        InputStream in = this.getClass().getResourceAsStream(path);
        if (in != null) return in;
        
        // (3a) TODO - use context to grok app's resource context
        // and check there (eg, will work inside servlet context)
        
        // (3) check inside jars on the classpath...
        String cp = System.getProperty("java.class.path");
        if (cp == null) return null;
        
        String[] jars = cp.split(":");
        if (jars == null) return null;
        
        for (String jar : jars) {
            if (jar.endsWith(".jar")) {
                in = peekInsideJar("jar:file:"+jar, path);
                if (in != null) return in;
            }
        }
        
        // (4) give up!
        return null;
    }
    
    private InputStream peekInsideJar(String jar, String resourcePath)
    {
        String resourceURL = jar + "!" + resourcePath;
        try {
            URL url = new URL(resourceURL);
            InputStream in = url.openStream();
            if (in != null) return in;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
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
        }
        
        return null;
    }
    
    public String translate(String string, String[] args, Chunk context)
    {
        return processFormatString(string,args,context,translations);
    }
    
    public static String processFormatString(String string, String[] args, Chunk context)
    {
        return processFormatString(string,args,context,null);
    }
    
    public static String processFormatString(String string, String[] args,
            Chunk context, HashMap<String,String> translations)
    {
        if (string == null) return null;
        
        String xlated = string;
        if (translations != null && translations.containsKey(string)) {
            xlated = translations.get(string);
        }
        
        if (!xlated.contains("%s") || args == null || context == null) {
            return xlated;
        }
        
        // prepare format-substitution values from args
        // eg for strings like "Hello %s, welcome to the site!"
        Object[] values = new String[args.length];
        
        for (int i=0; i<args.length; i++) {
            String tagName = args[i];
            if (tagName.startsWith("~") || tagName.startsWith("$")) {
                Object val = context.get(tagName.substring(1));
                String valString = (val == null ? "" : val.toString());
                values[i] = valString;
            } else {
                // not a tag, a static value
                values[i] = tagName;
            }
        }
        
        try {
            return String.format(xlated,values);
        } catch (IllegalFormatException e) {
            return xlated;
        }
    }

    public Locale getJavaLocale()
    {
        if (localeCode != null && localeCode.contains("_")) {
            String[] langAndCountry = localeCode.split("_");
            if (langAndCountry.length > 1) {
                String lang    = langAndCountry[0];
                String country = langAndCountry[1];
                if (lang != null && lang.trim().length() > 0) {
                    if (country != null && country.trim().length() > 0) {
                        Locale locale = new Locale(lang, country);
                        // confirm that this is a valid locale
                        try {
                            if (locale.getISO3Country() != null) {
                                if (locale.getISO3Language() != null) {
                                    // VALID LOCALE!  RETURN!
                                    return locale;
                                }
                            }
                        } catch (MissingResourceException e) {
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    public String toString()
    {
        return this.localeCode;
    }
   
}
