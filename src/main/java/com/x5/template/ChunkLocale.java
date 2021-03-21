package com.x5.template;

import java.util.HashMap;
import java.util.Map;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.MissingResourceException;

import com.x5.template.providers.TranslationsProvider;

public class ChunkLocale
{
    private String localeCode;
    private Map<String,String> translations;

    public static ChunkLocale getInstance(String localeCode, Chunk context)
    {
        return new ChunkLocale(localeCode, context);
    }

    private ChunkLocale(String localeCode, Chunk context)
    {
        this.localeCode = localeCode;

        TranslationsProvider provider = context.getTranslationsProvider();
        if (provider == null) {
            provider = new DefaultTranslationsProvider();
        }

        this.translations = provider.getTranslations(localeCode);
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
            Chunk context, Map<String,String> translations)
    {
        if (string == null) return null;

        String xlated = string;
        if (translations != null && translations.containsKey(string)) {
            xlated = translations.get(string);
        }

        if (args == null || context == null || !xlated.contains("%s")) {
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
