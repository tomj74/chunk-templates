package com.x5.template;

import org.junit.Test;
import static org.junit.Assert.*;

public class MacroTest
{
    @Test
    public void simpleMacroTest()
    {
        Theme theme = new Theme("test/base");
        
        Chunk c = theme.makeChunk("macro_test#simple_test");
        c.set("host_name", "Bob");
        
        assertEquals("Once I ate a burrito and drank some sangria at Bob's house and I had awful indigestion afterwards.\n",
                c.toString());
    }
    
    @Test
    public void tableTest()
    {
        Theme theme = new Theme("test/base");
        
        Chunk c = theme.makeChunk("macro_test#table_test");
        
        assertEquals("<table>\n"
                + "<tr><td>spoon</td><td>$0.20</td></tr>\n"
                + "<tr><td>fork</td><td>$0.30</td></tr>\n"
                + "<tr><td>knife</td><td>$1.03</td></tr>\n"
                + "</table>\n",c.toString());
    }
    
    @Test
    public void dontFallForTheIfTest()
    {
        Theme theme = new Theme("test/base");
        
        Chunk c = theme.makeChunk("macro_test#assignment_trick_test");
        c.set("host_name", "Bob");
        
        assertEquals("Once I ate a burrito and drank some sangria at Jane's house and I had awful indigestion afterwards.\n",
                c.toString());
    }
    
    @Test
    public void standardArgsTest()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("macro_test#simplified_syntax_test");
        c.set("host_name", "Harry");
        
        String targetOutput = theme.fetch("macro_test#std_fmt_expected_output");
        
        assertEquals(targetOutput, c.toString());
    }
    
    @Test
    public void jsonArgsTest()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("macro_test#simplified_syntax_test_json");
        c.set("host_name", "Bob");
        
        String targetOutput = theme.fetch("macro_test#json_test_expected_output");
        
        assertEquals(targetOutput, c.toString());
    }
    
    @Test
    public void jsonStrictArgsTest()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("macro_test#simplified_syntax_test_json_strict");
        c.set("host_name", "Bob");
        
        String targetOutput = theme.fetch("macro_test#json_test_expected_output");
        
        assertEquals(targetOutput, c.toString());
    }
    
    @Test
    public void xmlArgsTest()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("macro_test#simplified_syntax_test_xml");
        c.set("host_name", "Bob");
        
        String targetOutput = theme.fetch("macro_test#xml_test_expected_output");
        
        assertEquals(targetOutput, c.toString());
    }
    
    @Test
    public void tableOfMapsTest()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("macro_test#table_of_maps_test");
        
        String targetOutput = theme.fetch("macro_test#table_of_maps_expected_output");
        
        assertEquals(targetOutput, c.toString());
    }
    
    @Test
    public void inlineBodyTest()
    {
        Chunk c = new Chunk();
        c.append("{^exec}{~a=2}{~x=3}{^body}a = {~a}.  x = {~x}.{/exec}");
        
        assertEquals("a = 2.  x = 3.", c.toString());
    }

    @Test
    public void inlineBodyWithEndTagTest()
    {
        // also tests smart trim...
        Chunk c = new Chunk();
        c.append("{^exec}{~a=2}{~x=3}{^body}\na = {~a}.  x = {~x}.\n{/body} alksdjflaksjdf {/exec}");
        
        assertEquals("a = 2.  x = 3.\n", c.toString());
    }
}
