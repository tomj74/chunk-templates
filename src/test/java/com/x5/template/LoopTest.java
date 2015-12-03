package com.x5.template;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

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
        c.append("{% loop in $list array_tags=\"true\" %}  \n {$DATA[0]}<br/>\n{% endloop %}");
        assertEquals(" Frodo<br/>\n Bilbo<br/>\n Sam<br/>\n", c.toString());
    }

    @Test
    public void testSmartTrimNoLF()
    {
        Chunk c = new Chunk();
        String[] list = new String[]{"Frodo","Bilbo","Sam"};
        c.set("list", list);
        // smart trim is the default (strip first line if all whitespace)
        // note, this template has no final newline.
        c.append("{% loop in $list array_tags=\"true\" %}\n{~DATA[0]}<br/>{% endloop %}");
        assertEquals("Frodo<br/>Bilbo<br/>Sam<br/>", c.toString());
    }

    @Test
    public void testSimpleLoopNoThemeTrimAll()
    {
        Chunk c = new Chunk();
        String[] list = new String[]{"Frodo","Bilbo","Sam"};
        c.set("list", list);
        c.append("{^loop data=\"~list\" trim=\"all\" array_tags=\"true\"}\n {~DATA[0]}<br/>\n{/loop}");
        assertEquals("Frodo<br/>Bilbo<br/>Sam<br/>", c.toString());
    }

    @Test
    public void testSimpleLoopNoThemeTrimAllFromFile()
    {
        Theme theme = new Theme("themes", "test/base");
        Chunk c = theme.makeChunk("loop_test#whitespace_test");
        String[] list = new String[]{"Frodo","Bilbo","Sam"};
        c.set("list", list);
        assertEquals("Frodo<br/>Bilbo<br/>Sam<br/>", c.toString());
    }

    @Test
    public void testSimpleLoopNoThemeNoTrim()
    {
        Chunk c = new Chunk();
        String[] list = new String[]{"Frodo","Bilbo","Sam"};
        c.set("list", list);
        c.append("{^loop data=\"~list\" trim=\"false\" array_tags=\"true\"}\n{~DATA[0]}<br/>\n{/loop}");
        assertEquals("\nFrodo<br/>\n\nBilbo<br/>\n\nSam<br/>\n", c.toString());
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
        assertEquals(" 1 thingamabob<br/>\n <hr/>\n 2 doodad<br/>\n", c.toString());
    }

    @Test
    public void testLessSimpleBlockLoopOnEmpty()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_less_simple_block_loop");
        assertEquals(" <i>No widgets!</i>\n", c.toString());
    }

    @Test
    public void testSimpleLoopNoBlock()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_simple_loop_noblock");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        assertEquals("1 thingamabob<br/>\n<hr/>2 doodad<br/>\n\n", c.toString());
    }

    @Test
    public void testSimpleLoopNoBlockDelimTemplate()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_simple_loop_noblock_delim_tpl");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        assertEquals("1 thingamabob<br/>\n<hr/>\n2 doodad<br/>\n\n", c.toString());
    }

    @Test
    public void testSimpleLoopNoBlockMissingDelimTemplate()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_simple_loop_noblock_bad_delim_tpl");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        assertEquals("1 thingamabob<br/>\nchunk_test#not_exist2 doodad<br/>\n\n", c.toString());
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
    public void testUnbalancedInlineTables()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_nested_loops");
        String widgets = "[[widget_id,widget_name,new],[1,thingamabob],[2,doodad,new]]";
        String wodgets = "[[wodget_id,wodget_name,new],[1,thingamabob,new],[2,doodad]]";
        c.set("widgets",widgets);
        c.set("wodgets",wodgets);

        assertEquals("thingamabob\n thingamabob *\n --\n doodad\n==\ndoodad new!\n thingamabob\n --\n doodad *\n",
            c.toString());
    }

    @Test
    public void testInlineIfPreserveLinefeed()
    {
        Theme theme = new Theme("themes","test/base");

        Chunk c = theme.makeChunk("chunk_test#inline_if_nocollapse");
        c.set("abc",new String[]{"a","b","c"});

        assertEquals("Content\na\nb\nc\nContent\na\nb\nc\n", c.toString());
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

        String target = " 1 thingamabob<br/>\n <hr/>\n 2 doodad<br/>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testFirstLast()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_first_last");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad],[3,gadget]]";
        c.set("widgets",widgets);

        String target = "<div class=\"first\">1</div>\n<div class=\"\">2</div>\n<div class=\"last\">3</div>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testFirstLastNamed()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_first_last_named");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad],[3,gadget]]";
        c.set("widgets",widgets);

        String target = "<div class=\"first\">1</div>\n<div class=\"\">2</div>\n<div class=\"last\">3</div>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testFirstLastEdge()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_first_last_edge");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad],[3,gadget]]";
        c.set("widgets",widgets);

        String target = "<div class=\"first\">1</div>\n<div class=\"\">2</div>\n<div class=\"last\">3</div>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testFirstLastEdgeOnlyOne()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_first_last_edge");
        String widgets = "[[widget_id,widget_name],[1,thingamabob]]";
        c.set("widgets",widgets);

        String target = "<div class=\"first last\">1</div>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testFirstLastNamedEdge()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_first_last_named_edge");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad],[3,gadget]]";
        c.set("widgets",widgets);

        String target = "<div class=\"first\">1</div>\n<div class=\"\">2</div>\n<div class=\"last\">3</div>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testFirstLastNamedEdgeOnlyOne()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_first_last_named_edge");
        String widgets = "[[widget_id,widget_name],[1,thingamabob]]";
        c.set("widgets",widgets);

        String target = "<div class=\"first last\">1</div>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testFirstLastPrefixed()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_first_last_prefixed");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad],[3,gadget]]";
        c.set("widgets",widgets);

        String target = "<div class=\"first\">1</div>\n<div class=\"\">2</div>\n<div class=\"last\">3</div>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testCounterTags()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_counter");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        String target = " 0 1 1 thingamabob<br/>\n <hr/>\n 1 2 2 doodad<br/>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testCounterTag()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_counter_i");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        String target = " 0 1 thingamabob<br/>\n <hr/>\n 1 2 doodad<br/>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testCounterTagOffset()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_counter_i2");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        String target = " 0 1 5 thingamabob<br/>\n <hr/>\n 1 2 6 doodad<br/>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testCounterTagOffsetStep()
    {
        Theme theme = new Theme("themes","test/base,test/override");

        Chunk c = theme.makeChunk("chunk_test#looptest_counter_i3");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        String target = " 5 thingamabob<br/>\n <hr/>\n 3 doodad<br/>\n";
        assertEquals(target, c.toString());
    }

    @Test
    public void testWhitespaceStrip()
    {
        Theme theme = new Theme("test/base");

        Chunk c = theme.makeChunk("chunk_test#looptest_whitespace");
        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";
        c.set("widgets",widgets);

        assertEquals("<ul>\n\n<li>thingamabob</li>\n<li>doodad</li>\n\n</ul>\n<ol>\n\n<li>thingamabob</li>\n<li>doodad</li>\n\n</ol>\n",
                c.toString());
    }

    @Test
    public void testDataOptionWithTemplateRef()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("chunk_test#looptest_dataref");
        String targetOutput = theme.fetch("chunk_test#looptest_dataref_expected");
        assertEquals(targetOutput, c.toString());
    }

    @Test
    public void testLoopOverSplit()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.set("nums", "1,2,3,4");
        c.append("{.loop in $nums|split(,) as $n}{$n}{.divider} {/divider}{/loop}");
        assertEquals("1 2 3 4", c.toString());
    }

    @Test
    public void testLoopOverNestedEnumeration()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        StringTokenizer tokens = new StringTokenizer("a b c d");
        Map<String,Object> map = new java.util.HashMap<String,Object>();
        map.put("tokens", tokens);
        c.set("token_holder", map);
        c.append("{% loop in $token_holder.tokens as $token divider='-' %}{$token}{% endloop %}");

        assertEquals("a-b-c-d", c.toString());
    }

    @Test
    public void testLoopOverMap()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        Map<String,Object> map = new java.util.HashMap<String,Object>();
        map.put("a", "1");
        map.put("b", "2");
        c.set("map", map);
        c.append("{% loop in $map as $key:$value divider='-' %}{$key}:{$value}{% endloop %}");

        assertEquals("a:1-b:2", c.toString());
    }

    @Test
    public void testLoopOverNestedMap()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        Map<String,Object> mapA = new java.util.HashMap<String,Object>();
        Map<String,Object> mapB = new java.util.HashMap<String,Object>();
        mapB.put("a", "1");
        mapB.put("b", "2");
        mapA.put("child", mapB);
        c.set("parent_map", mapA);
        c.append("{% loop in $parent_map.child as $key:$value divider='-' %}{$key}:{$value}{% endloop %}");

        assertEquals("a:1-b:2", c.toString());
    }

    @Test
    public void testLoopOverChildObject()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        ChunkTest.CircularThing bob = new ChunkTest.CircularThing("Bob", 28, false);
        c.set("bob", bob);
        c.append("{% loop in $bob.boss as $key:$value divider='-' %}{$key}:{$value|type}{% endloop %}");

        assertEquals("age:STRING-boss:OBJECT-children:LIST-map:NULL-name:STRING-pi:STRING", c.toString());
    }

    @Test
    public void testLoopOverBeanChildListOfObjects()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();

        ChunkTest.ThingBean bob = new ChunkTest.ThingBean();
        bob.setName("bob");
        bob.setAge(28);
        bob.setActive(true);
        ChunkTest.ThingBean[] kids = bob.getChildren();
        kids[0].setName("jack");
        kids[1].setName("jill");

        c.set("bob", bob);
        c.append("{% loop in $bob.children as $child divider='-' %}{$child.name}{% endloop %}");

        assertEquals("jack-jill", c.toString());
    }

    @Test
    public void testLoopOverChildMap()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        ChunkTest.CircularThing bob = new ChunkTest.CircularThing("Bob", 28, false);
        Map map = new HashMap<String,String>();
        map.put("a", "1");
        map.put("b", "2");
        bob.setMap(map);
        c.set("bob", bob);
        c.append("{% loop in $bob.map as $key:$value divider='-' %}{$key}:{$value}{% endloop %}");

        assertEquals("a:1-b:2", c.toString());
    }

    @Test
    public void testLoopOverEnumeration()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        StringTokenizer tokens = new StringTokenizer("a b c d");
        c.set("tokens", tokens);
        c.append("{% loop in $tokens as $token divider='-' %}{$token}{% endloop %}");

        assertEquals("a-b-c-d", c.toString());
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
        assertEquals("WEBATDPLUS\n", output);
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
        assertEquals("WEBATDPLUS\n", output);
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
        assertEquals("WEBATDPLUS\n", output);
    }

    @Test
    public void testTreePrinter()
    {
        Theme theme = new Theme("test/base");

        Chunk c = theme.makeChunk("chunk_test#tree_test");

        String output = theme.fetch("chunk_test#tree_test_target_output");
        assertEquals(output, c.toString());
    }

    @Test
    public void testLocaleBug()
    {
        Theme theme = new Theme("themes","test/base");

        Chunk chunkA = theme.makeChunk("chunk_test#looptest_nested_loops");
        Chunk chunkB = theme.makeChunk("chunk_test#looptest_nested_loops");
        chunkB.setLocale("fr_FR");

        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,car]]";
        String wodgets = "[[wodget_id,wodget_name],[1,thingamabob],[2,car]]";
        chunkA.set("widgets",widgets);
        chunkA.set("wodgets",wodgets);
        chunkB.set("widgets",widgets);
        chunkB.set("wodgets",wodgets);

        assertEquals("thingamabob\n thingamabob *\n --\n voiture\n==\nvoiture\n thingamabob\n --\n voiture *\n",
                chunkB.toString());
        assertEquals("thingamabob\n thingamabob *\n --\n car\n==\ncar\n thingamabob\n --\n car *\n",
                chunkA.toString());

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
        assertEquals("12", outputA);

        c.unset("widgets");

        String outputB = c.toString();
        assertEquals("EMPTY", outputB);
    }

    @Test
    public void testDataResetAlt()
    {
        Theme theme = new Theme("themes","test/base");

        String widgets = "[[widget_id,widget_name],[1,thingamabob],[2,doodad]]";

        Chunk c = theme.makeChunk();
        c.append("{.loop in $widgets as $w}{$w.widget_id}{.onEmpty}EMPTY{/loop}");

        c.set("widgets", widgets);

        String outputA = c.toString();
        assertEquals("12", outputA);

        c.unset("widgets");

        String outputB = c.toString();
        assertEquals("EMPTY", outputB);
    }
}
