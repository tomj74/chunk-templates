package com.x5.template;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoopTest
{
    @Test
    public void testSimpleLoopNoTheme()
    {
        Chunk c = new Chunk();
        String[] list = new String[]{"Frodo","Bilbo","Sam"};
        c.set("list", list);
        // smart trim is the default (strip first line if all whitespace)
        c.append("{^loop data=\"~list\"}  \n {~DATA[0]}<br/>\n{^/loop}");
        assertEquals(" Frodo<br/>\n Bilbo<br/>\n Sam<br/>\n",c.toString());
    }

    @Test
    public void testSmartTrimNoLF()
    {
        Chunk c = new Chunk();
        String[] list = new String[]{"Frodo","Bilbo","Sam"};
        c.set("list", list);
        // smart trim is the default (strip first line if all whitespace)
        // note, this template has no final newline.
        c.append("{^loop data=\"~list\"}\n{~DATA[0]}<br/>{^/loop}");
        assertEquals("Frodo<br/>Bilbo<br/>Sam<br/>",c.toString());
    }
    
    @Test
    public void testSimpleLoopNoThemeTrimAll()
    {
        Chunk c = new Chunk();
        String[] list = new String[]{"Frodo","Bilbo","Sam"};
        c.set("list", list);
        c.append("{^loop data=\"~list\" trim=\"all\"}\n {~DATA[0]}<br/>\n{^/loop}");
        assertEquals("Frodo<br/>Bilbo<br/>Sam<br/>",c.toString());
    }
    
    @Test
    public void testSimpleLoopNoThemeNoTrim()
    {
        Chunk c = new Chunk();
        String[] list = new String[]{"Frodo","Bilbo","Sam"};
        c.set("list", list);
        c.append("{^loop data=\"~list\" trim=\"false\"}\n{~DATA[0]}<br/>\n{^/loop}");
        assertEquals("\nFrodo<br/>\n\nBilbo<br/>\n\nSam<br/>\n",c.toString());
    }
    
    @Test
    public void testSimpleBlockLoop()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_simple_block_loop");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);
        assertEquals(c.toString()," 1 thingamabob<br/>\n 2 doodad<br/>\n");
    }
    
    @Test
    public void testLessSimpleBlockLoop()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_less_simple_block_loop");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);
        assertEquals(c.toString()," 1 thingamabob<br/>\n <hr/>\n 2 doodad<br/>\n");
    }
    
    @Test
    public void testLessSimpleBlockLoopOnEmpty()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_less_simple_block_loop");
        assertEquals(c.toString(),"<i>No widgets!</i>");
    }
    
    @Test
    public void testSimpleLoopNoBlock()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_simple_loop_noblock");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        assertEquals(c.toString(),"1 thingamabob<br/>\n<hr/>2 doodad<br/>\n\n");
    }
    
    @Test
    public void testSimpleLoopNoBlockDelimTemplate()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_simple_loop_noblock_delim_tpl");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        assertEquals(c.toString(),"1 thingamabob<br/>\n<hr/>\n2 doodad<br/>\n\n");        
    }
    
    @Test
    public void testSimpleLoopNoBlockMissingDelimTemplate()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_simple_loop_noblock_bad_delim_tpl");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        assertEquals(c.toString(),"1 thingamabob<br/>\nchunk_test#not_exist2 doodad<br/>\n\n");        
    }
}
