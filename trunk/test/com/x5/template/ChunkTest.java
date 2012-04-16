package com.x5.template;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChunkTest
{
    @Test
    public void testSimpleDefault()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name:Bob}!");
        assertEquals("Hello, my name is Bob!",c.toString());
    }
    
    @Test
    public void testPassThru()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name}!");
        assertEquals("Hello, my name is {~name}!",c.toString());
    }
    
    @Test
    public void testSimpleExpand()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name}!");
        c.set("name","Harold");
        assertEquals("Hello, my name is Harold!",c.toString());
    }

    @Test
    public void testSimpleExpandWithDefault()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name:Bob}!");
        c.set("name","Harold");
        assertEquals("Hello, my name is Harold!",c.toString());
    }

    @Test
    public void testFilteredDefault()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name:O'Reilly|qs}!");
        assertEquals("Hello, my name is O\\'Reilly!",c.toString());
    }

    @Test
    public void testUnfilteredDefault()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name|qs:O'Reilly}!");
        assertEquals("Hello, my name is O'Reilly!",c.toString());
    }
    
    @Test
    public void testSimpleRecursion()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name}!");
        c.set("name", "{~username}");
        c.set("username", "Bob");
        assertEquals("Hello, my name is Bob!", c.toString());
    }
    
    @Test
    public void testInfiniteRecursion()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name}!");
        c.set("name", "Bob and my cat is also {~name}");
        assertTrue(c.toString().indexOf("max template recursions") > 0);
    }
    
    @Test
    public void testParentFallback()
    {
        Chunk p = new Chunk();
        Chunk c = new Chunk();
        p.set("name", "Dad");
        p.set("child", c);
        p.append("{~child}");
        c.append("Hello, my name is {~name}!");
        assertEquals(p.toString(),"Hello, my name is Dad!");
    }
    
    @Test
    public void testChildPrecedence()
    {
        Chunk p = new Chunk();
        Chunk c = new Chunk();
        p.set("name", "Dad");
        p.set("child", c);
        p.append("{~child}");
        c.append("Hello, my name is {~name}!");
        c.set("name", "Son");
        assertEquals(p.toString(),"Hello, my name is Son!");
    }
    
    @Test
    public void testGrandparentFallback()
    {
        Chunk g = new Chunk();
        Chunk p = new Chunk();
        Chunk c = new Chunk();
        
        g.set("name", "Grandpa");
        g.set("parent", p);
        p.set("child", c);
        g.append("G: {~parent}");
        p.append("P: {~child}");
        c.append("C: Hello, my name is {~name}!");
        assertEquals(g.toString(),"G: P: C: Hello, my name is Grandpa!");
    }
    
    @Test
    public void testParentPrecedence()
    {
        Chunk g = new Chunk();
        Chunk p = new Chunk();
        Chunk c = new Chunk();
        
        g.set("name", "Grandpa");
        g.set("parent", p);
        p.set("child", c);
        g.append("G: {~parent}  Grandpa is {~name}!");
        p.append("P: {~child}");
        c.append("C: Hello, my name is {~name}!");
        
        p.set("name", "Parent");
        
        assertEquals(g.toString(),"G: P: C: Hello, my name is Parent!  Grandpa is Grandpa!");        
    }
    
    @Test
    public void testIfNull()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name}!");

        String name = null;
        c.set("name",name,"UNKNOWN");
        
        assertEquals(c.toString(), "Hello, my name is UNKNOWN!");
    }

    @Test
    public void testNullToEmptyString()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name}!");

        String name = null;
        c.set("name",name);
        
        assertEquals(c.toString(), "Hello, my name is !");
    }
    
    @Test
    public void testNullToPassThru()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name}!");

        String name = null;
        c.setOrDelete("name",name);
        
        assertEquals(c.toString(), "Hello, my name is {~name}!");
    }
    
    @Test
    public void testSetOrDelete()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name}!");

        c.set("name","Bob");

        String name = null;
        c.setOrDelete("name",name);
        
        assertEquals(c.toString(), "Hello, my name is {~name}!");
    }
    
    @Test
    public void testBackticks()
    {
        // TODO: add some tests for backtick edge cases
        // (eg, what if ~id is not defined, what if ~name21 is not defined)
        Chunk c = new Chunk();
        c.set("name", "Bob");
        c.set("name21", "Rob");
        c.set("id", "21");
        c.append("Hello, my name is {~name`~id`}!");
        
        assertEquals(c.toString(), "Hello, my name is Rob!");
    }
    
    @Test
    public void testUncappedLiteral()
    {
        Chunk c = new Chunk();
        c.append("Pass through! {~gronk:bubbles!} <!-- {^literal} --> passing through {~gronk:} Pass on!");
        
        assertEquals(c.toString(), "Pass through! bubbles! <!-- {^literal} --> passing through {~gronk:} Pass on!");
    }
    
    @Test
    public void testUncappedLiteralInFile()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("chunk_test#uncapped_literal");
        
        assertEquals(c.toString(), "Scooby Doo says ruff ruff!\n{^literal}\n{#}\n");
    }

    @Test
    public void testLiteral()
    {
        Chunk c = new Chunk();
        c.append("Pass through! {~gronk:bubbles!} <!-- {^literal} --> passing through {~gronk:} <!-- {^} --> Pass on {~process_me:happily}!");
        
        assertEquals(c.toString(), "Pass through! bubbles! <!-- {^literal} --> passing through {~gronk:} <!-- {^} --> Pass on happily!");
    }

    @Test
    public void testLiteralAltSyntaxA()
    {
        Chunk c = new Chunk();
        c.append("Pass through! {~gronk:bubbles!} <!-- {^^} --> passing through {~gronk:} <!-- {^} --> Pass on {~process_me:happily}!");
        
        assertEquals(c.toString(), "Pass through! bubbles! <!-- {^^} --> passing through {~gronk:} <!-- {^} --> Pass on happily!");
    }

    @Test
    public void testLiteralAltSyntaxB()
    {
        Chunk c = new Chunk();
        c.append("Pass through! {~gronk:bubbles!} <!-- {^literal} --> passing through {~gronk:} <!-- {/literal} --> Pass on {~process_me:happily}!");
        
        assertEquals(c.toString(), "Pass through! bubbles! <!-- {^literal} --> passing through {~gronk:} <!-- {/literal} --> Pass on happily!");
    }
    
    @Test
    public void testIncludeShorthand()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk();
        c.append("{+chunk_test#no_widgets}");
        assertEquals("<i>No widgets!</i>\n",c.toString());
    }

    @Test
    public void testIncludeIfShorthand()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk();
        c.append("{+(!widgets)chunk_test#no_widgets}");
        assertEquals("<i>No widgets!</i>\n",c.toString());
    }
    
    @Test
    public void testSnippetRoundTrip()
    {
        String tpl = "xyz {~xyz:} {* MACRO *} {*} {^loop} {/loop}";
        Snippet testSnippet = new Snippet(tpl);
        String recombobulated = testSnippet.toString();
        assertEquals(tpl,recombobulated);
    }
}
