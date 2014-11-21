package com.x5.template;

import org.junit.Test;
import static org.junit.Assert.*;

import com.x5.template.providers.TemplateProvider;

public class ThemeTest
{
    @Test
    public void testOverrideLayer()
    {
         Theme theme = new Theme("themes","test/base,test/override");
         Chunk c = theme.makeChunk("chunk_test");

         assertEquals("Override Layer", c.toString().trim());
    }

    @Test
    public void testOverrideLayerDirectly()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        Chunk c = theme.makeChunk("chunk_test#layer_test");

        assertEquals("Override Layer\n", c.toString());
    }

    @Test
    public void testUnindenter()
    {
        String multilineBlock = "\t  {^loop}  \t \n\t    {^if (~cond =~ /\\d{1,3}/)}\n  This\n\t    {~.else}\n  That\n\t    {^/if}\n\t  {^/loop}\n  {% ~.if ($x) %}\nX\n  {% ~.endIf %}\n";
        String clean = TemplateSet.removeBlockTagIndents(multilineBlock);
        assertEquals(clean, "{^loop}\n{^if (~cond =~ /\\d{1,3}/)}\n  This\n{~.else}\n  That\n{^/if}\n{^/loop}\n{% ~.if ($x) %}\nX\n{% ~.endIf %}\n");
    }
    
    @Test
    public void testSuperTag()
    {
        Theme theme = new Theme("test/base,test/override");
        
        Snippet x = theme.getSnippet("layer_test#snippet");
        assertEquals("Override Snippet",x.toString().trim());

        x = theme.getSnippet("layer_test#only_in_base");
        assertEquals("Only in Base",x.toString().trim());
        
        Chunk c = theme.makeChunk("layer_test");
        assertEquals("Base Layer", c.toString().trim());
    }

    @Test
    public void testNonDefaultExtension()
    {
        DummyLoader loader = new DummyLoader();
        Theme theme = new Theme(loader);
        Chunk chunk = theme.makeChunk("order", "chunk");
        assertEquals("order.chunk\n", chunk.toString());
    }

    @Test
    public void testOtherExtensions()
    {
        Theme theme = new Theme("test/base");

        Chunk txt = theme.makeChunk("text_file", "txt");
        Chunk xml = theme.makeChunk("xml_file", "xml");
        Chunk html = theme.makeChunk("html_file", "html");

        assertEquals("Main snippet.\n\n", txt.toString());
        assertEquals("<?xml version=\"1.0\"?>\n<SomeElement>Boo!</SomeElement>\n\n", xml.toString());
        assertEquals("<!DOCTYPE html>\n<html>\n<head>\n<title>HTML</title>\n</head>\n<body>\nAnother Web Page\n</body>\n</html>\n\n", html.toString());
    }

    @Test
    public void testOtherExtensionsSnippets()
    {
        Theme theme = new Theme("test/base");

        Chunk txt = theme.makeChunk("text_file#sub_snippet", "txt");
        Chunk xml = theme.makeChunk("xml_file#sub_snippet", "xml");
        Chunk html = theme.makeChunk("html_file#sub_snippet", "html");

        assertEquals("Sub snippet\n", txt.toString());
        assertEquals("<SubElement>Far!</SubElement>\n", xml.toString());
        assertEquals("<div>Use me anywhere.</div>\n", html.toString());
    }


    public static class DummyLoader extends TemplateProvider
    {
        public String loadContainerDoc(String docName)
        {
            return docName;
        }

        public String getProtocol()
        {
            return "dummy";
        }
    }
}
