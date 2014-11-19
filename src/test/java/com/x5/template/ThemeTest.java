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
        String multilineBlock = "\t  {^loop}  \t \n\t    {^if (~cond =~ /\\d{1,3}/)}\n  This\n\t    {~.else}\n  That\n\t    {^/if}\n\t  {^/loop}\n";
        String clean = TemplateSet.removeBlockTagIndents(multilineBlock);
        assertEquals(clean, "{^loop}\n{^if (~cond =~ /\\d{1,3}/)}\n  This\n{~.else}\n  That\n{^/if}\n{^/loop}\n");
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
