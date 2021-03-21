package com.x5.template;

import com.x5.template.providers.TranslationsProvider;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class LocaleTagTest
{
    @Test
    public void testRemoveClutter()
    {
        // make sure locale-tag clutter is removed when template is processed
        // even if no translation locale is provided
        Chunk c = new Chunk();
        c.append("Bla bla bla _[A new car!] bla bla");
        assertEquals("Bla bla bla A new car! bla bla", c.toString());
    }

    @Test
    public void testFmtStringStatic()
    {
        // substitute static string correctly
        Chunk c = new Chunk();
        c.append("Bla bla bla {_[A new %s car!],blue} bla bla");
        assertEquals("Bla bla bla A new blue car! bla bla", c.toString());
    }

    @Test
    public void testArg()
    {
        // substitute dynamic tag correctly
        Chunk c = new Chunk();
        c.set("color", "blue");
        c.append("Bla bla bla {_[A new %s car!],~color} bla bla");
        assertEquals("Bla bla bla A new blue car! bla bla", c.toString());
    }

    @Test
    public void testManyArgs()
    {
        // substitute dynamic tag correctly
        Chunk c = new Chunk();
        c.set("color", "blue");
        c.set("make", "lexus");
        c.append("Bla bla bla {_[A new %s %s!],~color,~make} bla bla");
        assertEquals("Bla bla bla A new blue lexus! bla bla", c.toString());
    }

    @Test
    public void testManyTokens()
    {
        // don't get tripped up when a braced token follows an unbraced
        // token (bug 74)
        Chunk c = new Chunk();
        c.set("color", "blue");
        c.set("make", "lexus");
        c.append("Bla bla bla _[car] - {_[A new %s %s made of %s!],$color,$make,plastic} bla bla");
        assertEquals("Bla bla bla car - A new blue lexus made of plastic! bla bla", c.toString());
    }

    @Test
    public void testRemoveClutterBraced()
    {
        // make sure locale-tag clutter is removed when template is processed
        // even if no translation locale is provided
        Chunk c = new Chunk();
        c.append("Bla bla bla {_[A new car!]} bla bla");
        assertEquals("Bla bla bla A new car! bla bla", c.toString());
    }

    @Test
    public void testMoreThanOne()
    {
        // make sure locale-tag clutter is removed when template is processed
        // even if no translation locale is provided
        Chunk c = new Chunk();
        c.append("Bla bla bla _[A new car!] bla _[A new house!] bla");
        assertEquals("Bla bla bla A new car! bla A new house! bla", c.toString());
    }

    @Test
    public void testMoreThanOneBraced()
    {
        // make sure locale-tag clutter is removed when template is processed
        // even if no translation locale is provided
        Chunk c = new Chunk();
        c.append("Bla bla bla {_[A new car!]} bla {_[A new house!]} bla");
        assertEquals("Bla bla bla A new car! bla A new house! bla", c.toString());
    }

    @Test
    public void testMoreThanOneMixed()
    {
        // make sure locale-tag clutter is removed when template is processed
        // even if no translation locale is provided
        Chunk c = new Chunk();
        c.append("Bla bla bla {_[A new car!]} bla _[A new house!] bla. ");
        c.append("Bla bla bla {_[A new pet!]} bla _[A new life!] bla");
        assertEquals("Bla bla bla A new car! bla A new house! bla. Bla bla bla A new pet! bla A new life! bla", c.toString());
    }

    @Test
    public void testMismatchedArgs()
    {
        // substitute dynamic tag correctly
        Chunk c = new Chunk();
        c.set("color", "blue");
        c.set("make", "lexus");
        // not enough args, formatter should bail.
        c.append("Bla bla bla {_[A new %s %s!],~color} bla bla");
        assertEquals("Bla bla bla A new %s %s! bla bla", c.toString());
    }

    @Test
    public void testDeepTranslationAndFormat()
    {
        // translate and substitute dynamic tag correctly
        Chunk c = new Chunk();
        c.set("color", "_[blue]");
        c.set("make", "lexus");
        String[] translations = new String[]{"A new %s %s!","Eine neue %s %s!","blue","blau"};
        c.setTranslationsProvider(new TestTranslationsProvider("de_DE", translations));
        c.setLocale("de_DE");
        c.append("Bla bla bla {_[A new %s %s!],~color,~make} bla bla");
        assertEquals("Bla bla bla Eine neue blau lexus! bla bla", c.toString());
    }

    @Test
    public void testTranslationCustomProvider()
    {
        // translate and substitute dynamic tag correctly
        Chunk c = new Chunk();
        String[] translations = new String[]{"A new car!","Eine neue Auto!"};
        c.setTranslationsProvider(new TestTranslationsProvider("de_DE", translations));
        c.setLocale("de_DE");
        c.append("Bla bla bla _[A new car!] bla bla");
        assertEquals("Bla bla bla Eine neue Auto! bla bla", c.toString());
    }

    @Test
    public void testTranslationCustomProviderWithTheme()
    {
        // translate and substitute dynamic tag correctly
        Theme theme = new Theme();
        String[] translations = new String[]{"A new car!","Eine neue Auto!"};
        theme.setTranslationsProvider(new TestTranslationsProvider("de_DE", translations));
        theme.setLocale("de_DE");

        Chunk c = theme.makeChunk();
        c.append("Bla bla bla _[A new car!] bla bla");
        assertEquals("Bla bla bla Eine neue Auto! bla bla", c.toString());
    }

    @Test
    public void testTranslationNewSyntax()
    {
        // translate and substitute dynamic tag correctly
        Chunk c = new Chunk();
        c.set("color", "_[blue]");
        c.set("make", "lexus");
        String[] translations = new String[]{"A new %s %s!","Eine neue %s %s!","blue","blau"};
        c.setTranslationsProvider(new TestTranslationsProvider("de_DE", translations));
        c.setLocale("de_DE");
        c.append("Bla bla bla {% _[A new %s %s!],$color,$make %} bla bla");
        assertEquals("Bla bla bla Eine neue blau lexus! bla bla", c.toString());
    }

    @Test
    public void testTranslation()
    {
        // translate and substitute dynamic tag correctly
        Chunk c = new Chunk();
        c.setLocale("fr_FR");
        c.append("Bla bla bla _[A new car!] bla bla");
        assertEquals("Bla bla bla Un nouveau auto! bla bla", c.toString());
    }

    @Test
    public void testFormatDefaultLocale()
    {
        Chunk c = new Chunk();
        /////c.setLocale("fr_FR");
        c.append("{$x:3000000|sprintf(%,.2f)}");
        assertEquals("3,000,000.00", c.toString());
    }

    @Test
    public void testFormatEuroLocale()
    {
        Chunk c = new Chunk();
        c.setLocale("fr_FR");
        c.append("{$x:3000000|sprintf(%,.2f)}");
        String nbsp = "\u00A0"; // unicode non-breaking space
        assertEquals("3"+nbsp+"000"+nbsp+"000,00", c.toString());
    }

    @Test
    public void testTranslationFilter()
    {
        Chunk c = new Chunk();
        c.setLocale("de_DE");
        c.setTranslationsProvider(new TestTranslationsProvider("de_DE", new String[]{"blue","blau"}));
        c.set("color","blue");
        c.append(c.makeTag("color|xlate"));

        assertEquals("blau",c.toString());
    }

    /**
     * Dumb translations provider that only works for a single locale.
     */
    class TestTranslationsProvider implements TranslationsProvider {
        private String localeCode;
        private Map<String, String> translations;

        public TestTranslationsProvider(String localeCode, String[] testStrings) {
            this.localeCode = localeCode;
            if (testStrings != null && testStrings.length > 1) {
                this.translations = new HashMap<String,String>();
                for (int i=0; i+1<testStrings.length; i++) {
                    String a = testStrings[i];
                    String b = testStrings[i+1];
                    translations.put(a,b);
                }
            }
        }

        public Map<String, String> getTranslations(String localeCode) {
            if (localeCode != this.localeCode) {
                return null;
            }
            return this.translations;
        }
    }

}
