package com.x5.template;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConfigTest
{
    @Test
    public void testEmptyConfig()
    {
        Map<String,String> cfg = new HashMap<String,String>();
        ThemeConfig config = new ThemeConfig(cfg);
        Theme theme = new Theme(config);
        Chunk c = theme.makeChunk("bare#snippet");
        assertEquals("Simple!\n", c.toString());
    }

    @Test
    public void testLayeredConfig()
    {
        Map<String,String> cfg = new HashMap<String,String>();
        cfg.put("layers", "test/base");
        ThemeConfig config = new ThemeConfig(cfg);
        Theme theme = new Theme(config);
        Chunk c = theme.makeChunk("layer_test#snippet");
        assertEquals("Base Snippet\n", c.toString());
    }

    @Test
    public void testMultiLayeredConfig()
    {
        Map<String,String> cfg = new HashMap<String,String>();
        cfg.put("layers", "test/base,test/override");
        ThemeConfig config = new ThemeConfig(cfg);
        Theme theme = new Theme(config);
        Chunk c = theme.makeChunk("chunk_test");
        assertEquals("Override Layer", c.toString().trim());

        // swap order, test that precedence has reversed
        cfg.put("layers", "test/override,test/base");
        config = new ThemeConfig(cfg);
        theme = new Theme(config);
        c = theme.makeChunk("chunk_test");
        assertEquals("Base Layer", c.toString().trim());
    }

    @Test
    public void testUserFilterConfig()
    {
        Map<String,String> cfg = new HashMap<String,String>();
        cfg.put("filters", "com.x5.template.FilterTest$LeftTrimFilter");

        ThemeConfig config = new ThemeConfig(cfg);
        Theme theme = new Theme(config);
        Chunk c = theme.makeChunk();
        c.append("XXX{$tag|ltrim}XXX");
        c.set("tag", "  spacey  ");
        assertEquals("XXXspacey  XXX", c.toString());
    }

    @Test
    public void testHideErrors()
    {
        Map<String,String> cfg = new HashMap<String,String>();
        cfg.put("hide_errors", "FALSE");

        ThemeConfig config = new ThemeConfig(cfg);
        Theme theme = new Theme(config);
        Chunk c = theme.makeChunk("file_that_does_not_exist");
        assertEquals("[chtml template 'file_that_does_not_exist' not found]<!-- looked in [themes/file_that_does_not_exist.chtml] -->", c.toString());

        cfg = new HashMap<String,String>();
        cfg.put("hide_errors", "TRUE");
        cfg.put("error_log", "test_errors");

        config = new ThemeConfig(cfg);
        theme = new Theme(config);
        c = theme.makeChunk("file_that_does_not_exist");
        assertEquals("", c.toString());
    }

    @Test
    public void testLocale()
    {
        Map<String,String> cfg = new HashMap<String,String>();
        cfg.put("locale", "FR_fr");

        ThemeConfig config = new ThemeConfig(cfg);
        Theme theme = new Theme(config);
        Chunk c = theme.makeChunk();
        c.append("{$x|sprintf(%.2f)}");
        c.set("x", "3.1415926");
        assertEquals("3,14", c.toString());
    }

    @Test
    public void testLatin1Encoding()
    {
        Map<String,String> cfg = new HashMap<String,String>();
        cfg.put("encoding", "ISO-8859-1");

        ThemeConfig config = new ThemeConfig(cfg);
        Theme theme = new Theme(config);
        Chunk c = theme.makeChunk("latin1#invalid_utf8");
        c.set("x", "3.1415926");
        assertEquals("\u00C0\u00C1\u00C2\u00C3 3.14", c.toString());
    }

    @Test
    public void testWrongEncoding()
    {
        Map<String,String> cfg = new HashMap<String,String>();
        cfg.put("encoding", "UTF-8");

        ThemeConfig config = new ThemeConfig(cfg);
        Theme theme = new Theme(config);
        Chunk c = theme.makeChunk("latin1#invalid_utf8");
        c.set("x", "3.1415926");
        assertEquals("\uFFFD\uFFFD\uFFFD\uFFFD 3.14", c.toString());
    }

    @Test
    public void testUtf8Encoding()
    {
        Map<String,String> cfg = new HashMap<String,String>();
        cfg.put("encoding", "UTF-8");

        ThemeConfig config = new ThemeConfig(cfg);
        Theme theme = new Theme(config);
        Chunk c = theme.makeChunk("utf8#valid_utf8");
        c.set("x", "3.1415926");
        assertEquals("\u00C0\u00C1\u00C2\u00C3 3.14", c.toString());
    }

}
