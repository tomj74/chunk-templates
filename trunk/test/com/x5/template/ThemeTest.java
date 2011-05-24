package com.x5.template;

import org.junit.Test;
import static org.junit.Assert.*;

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
        String multilineBlock = "  {^loop}\n    {^if (~cond =~ /\\d{1,3}/)}\n  This\n    {~.else}\n  That\n    {^/if}\n  {^/loop}\n";
        String clean = TemplateSet.removeBlockTagIndents(multilineBlock);
        assertEquals(clean, "{^loop}\n{^if (~cond =~ /\\d{1,3}/)}\n  This\n{~.else}\n  That\n{^/if}\n{^/loop}\n");
    }
}
