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
        c.append("{^loop data=\"~list\" array_tags=\"true\"}  \n {~DATA[0]}<br/>\n{/loop}");
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
        c.append("{^loop data=\"~list\" array_tags=\"true\"}\n{~DATA[0]}<br/>{/loop}");
        assertEquals("Frodo<br/>Bilbo<br/>Sam<br/>",c.toString());
    }
    
    @Test
    public void testSimpleLoopNoThemeTrimAll()
    {
        Chunk c = new Chunk();
        String[] list = new String[]{"Frodo","Bilbo","Sam"};
        c.set("list", list);
        c.append("{^loop data=\"~list\" trim=\"all\" array_tags=\"true\"}\n {~DATA[0]}<br/>\n{/loop}");
        assertEquals("Frodo<br/>Bilbo<br/>Sam<br/>",c.toString());
    }
    
    @Test
    public void testSimpleLoopNoThemeNoTrim()
    {
        Chunk c = new Chunk();
        String[] list = new String[]{"Frodo","Bilbo","Sam"};
        c.set("list", list);
        c.append("{^loop data=\"~list\" trim=\"false\" array_tags=\"true\"}\n{~DATA[0]}<br/>\n{/loop}");
        assertEquals("\nFrodo<br/>\n\nBilbo<br/>\n\nSam<br/>\n",c.toString());
    }
    
    @Test
    public void testSimpleBlockLoop()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_simple_block_loop");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);
        assertEquals(" 1 thingamabob<br/>\n 2 doodad<br/>\n", c.toString());
    }
    
    @Test
    public void testLessSimpleBlockLoop()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_less_simple_block_loop");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);
        assertEquals(" 1 thingamabob<br/>\n <hr/>\n 2 doodad<br/>\n",c.toString());
    }
    
    @Test
    public void testLessSimpleBlockLoopOnEmpty()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_less_simple_block_loop");
        assertEquals(" <i>No widgets!</i>\n",c.toString());
    }
    
    @Test
    public void testSimpleLoopNoBlock()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_simple_loop_noblock");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        assertEquals("1 thingamabob<br/>\n<hr/>2 doodad<br/>\n\n",c.toString());
    }
    
    @Test
    public void testSimpleLoopNoBlockDelimTemplate()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_simple_loop_noblock_delim_tpl");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        assertEquals("1 thingamabob<br/>\n<hr/>\n2 doodad<br/>\n\n",c.toString());        
    }
    
    @Test
    public void testSimpleLoopNoBlockMissingDelimTemplate()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_simple_loop_noblock_bad_delim_tpl");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        assertEquals("1 thingamabob<br/>\nchunk_test#not_exist2 doodad<br/>\n\n",c.toString());        
    }
    
    @Test
    public void testNestedLoops()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_nested_loops");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        String wodgets = "[[wodget_id,wodget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);
        c.set("wodgets",wodgets);
        
        assertEquals("thingamabob\n thingamabob *\n --\n doodad\n==\ndoodad\n thingamabob\n --\n doodad *\n",
                c.toString());
    }
    
    @Test
    public void testNestedLoopsEZSyntax()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_nested_loops_ez_syntax");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);
        
        assertEquals("1. 1:thingamabob\n thingamabob *\n --\n doodad\n==\n2. 2:doodad\n thingamabob\n --\n doodad *\n",
                c.toString());
    }
    
    @Test
    public void testEZSyntax()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_ez_syntax");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);
        
        assertEquals(c.toString()," 1 thingamabob<br/>\n <hr/>\n 2 doodad<br/>\n");
    }
    
    @Test
    public void testFirstLast()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_first_last");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad],[3,gadget]]";
        c.set("widgets",widgets);
        
        assertEquals(c.toString(),"<div class=\"first\">1</div>\n<div class=\"\">2</div>\n<div class=\"last\">3</div>\n");
    }

    @Test
    public void testFirstLastNamed()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_first_last_named");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad],[3,gadget]]";
        c.set("widgets",widgets);
        
        assertEquals(c.toString(),"<div class=\"first\">1</div>\n<div class=\"\">2</div>\n<div class=\"last\">3</div>\n");
    }

    @Test
    public void testFirstLastPrefixed()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_first_last_prefixed");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad],[3,gadget]]";
        c.set("widgets",widgets);
        
        assertEquals(c.toString(),"<div class=\"first\">1</div>\n<div class=\"\">2</div>\n<div class=\"last\">3</div>\n");
    }

    @Test
    public void testCounterTags()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_counter");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);
        
        assertEquals(c.toString()," 0 1 1 thingamabob<br/>\n <hr/>\n 1 2 2 doodad<br/>\n");
    }
    
    @Test
    public void testCounterTag()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_counter_i");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);
        
        assertEquals(c.toString()," 0 1 thingamabob<br/>\n <hr/>\n 1 2 doodad<br/>\n");
    }
    
    @Test
    public void testBadCloseTag()
    {
        Theme theme = new Theme("themes","test/base,test/override");
        
        Chunk c = theme.makeChunk("chunk_test#looptest_bad_close_tag");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);
        
        assertTrue(c.toString().startsWith("[ERROR in template! loop block with no matching end marker! ]"));
    }
    
    @Test
    public void testIfRegexInsideLoop()
    {
        Theme theme = new Theme("themes","test/base");
        
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";

        Chunk c = theme.makeChunk("chunk_test#if_regex_inside_loop");
        c.set("program_code","WEBATDPLUS");
        c.set("widgets", widgets);

        String output = c.toString();
        assertEquals(output,"WEBATDPLUS\n");
    }
    
    @Test
    public void testIfRegexInsideLoopFunkySyntax()
    {
        Theme theme = new Theme("themes","test/base");
        
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";

        Chunk c = theme.makeChunk("chunk_test#if_regex_inside_loop_tilde");
        c.set("program_code","WEBATDPLUS");
        c.set("widgets", widgets);

        String output = c.toString();
        assertEquals(output,"WEBATDPLUS\n");
    }
    
    @Test
    public void testAltPrefixSyntax()
    {
        Theme theme = new Theme("themes","test/base");
        
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";

        Chunk c = theme.makeChunk("chunk_test#alt_prefix_test");
        c.set("program_code","WEBATDPLUS");
        c.set("widgets", widgets);

        String output = c.toString();
        assertEquals(output,"WEBATDPLUS\n");
    }
    
    @Test
    public void testDataReset()
    {
        Theme theme = new Theme("themes","test/base");
        
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        
        Chunk c = theme.makeChunk();
        c.append("{^loop in ~widgets as w}{~w.widget_id}{^onEmpty}EMPTY{/loop}");
        
        c.set("widgets", widgets);
        
        String outputA = c.toString();
        assertEquals("12",outputA);
        
        c.unset("widgets");
        
        String outputB = c.toString();
        assertEquals("EMPTY",outputB);
    }
    
    @Test
    public void testDataResetAlt()
    {
        Theme theme = new Theme("themes","test/base");
        
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        
        Chunk c = theme.makeChunk();
        c.append("{.loop in ~widgets as w}{$w.widget_id}{.onEmpty}EMPTY{/loop}");
        
        c.set("widgets", widgets);
        
        String outputA = c.toString();
        assertEquals("12",outputA);
        
        c.unset("widgets");
        
        String outputB = c.toString();
        assertEquals("EMPTY",outputB);
    }
}
