package com.x5.template;

import org.junit.Test;
import static org.junit.Assert.*;

public class IfTagTest
{
    @Test
    public void testSimpleIfTag()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "cheese");
        c.append("{^if (~moon_material == cheese)} The moon is made of cheese! {^else} The moon is not made of cheese :( {^/if}");
        assertEquals("The moon is made of cheese!", c.toString());
    }
    
    @Test
    public void testElsePath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "legos");
        c.append("{^if (~moon_material == cheese)} The moon is made of cheese! {^else} The moon is not made of cheese :( {^/if}");
        assertEquals("The moon is not made of cheese :(", c.toString());
    }
    
    @Test
    public void testIfDefined()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "something");
        c.append("{^if (~moon_material)} The moon is made of something! {^else} The moon is not made of anything :( {^/if}");
        assertEquals("The moon is made of something!", c.toString());
    }
    
    @Test
    public void testIfDefinedElsePath()
    {
        Chunk c = new Chunk();
        c.append("{^if (~moon_material)} The moon is made of something! {^else} The moon is not made of anything :( {^/if}");
        assertEquals("The moon is not made of anything :(", c.toString());
    }

    @Test
    public void testIfNotDefined()
    {
        Chunk c = new Chunk();
        c.append("{^if (!moon_material)} The moon is not made of anything! {^else} The moon is made of something :) {^/if}");
        assertEquals("The moon is not made of anything!", c.toString());
    }

    @Test
    public void testIfNotDefinedElsePath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "something");
        c.append("{^if (!moon_material)} The moon is not made of anything! {^else} The moon is made of something :) {^/if}");
        assertEquals("The moon is made of something :)", c.toString());
    }
    
    @Test
    public void testSimpleElseIf()
    {
        // this also tests trimming line breaks.
        Chunk c = new Chunk();
        c.set("moon_material", "stilton");
        c.append("{^if (~moon_material == cheese)}\n The moon is made of cheese! \n{^elseIf (~moon_material == stilton)}\n The moon is made of Stilton! \n{^else}\n The moon is not made of cheese :( \n{^/if}\n");
        assertEquals(" The moon is made of Stilton! \n", c.toString());
    }
    
    @Test
    public void testSimpleElseIfElsePath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "rock");
        c.append("{^if (~moon_material == cheese)} The moon is made of cheese! {^elseIf (~moon_material == stilton)} The moon is made of Stilton! {^else} The moon is not made of cheese :( {^/if}");
        assertEquals("The moon is not made of cheese :(", c.toString());
    }
    
    @Test
    public void testSimpleElseIfThenPath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "cheese");
        c.append("{^if (~moon_material == cheese)} The moon is made of cheese! {^elseIf (~moon_material == cheese)} The moon is made of Stilton! {^else} The moon is not made of cheese :( {^/if}");
        assertEquals("The moon is made of cheese!", c.toString());
    }
    
    //TODO tests for parent fallback tests for tag values in comparisons.
    // and check that trim="false" works!
    
    @Test
    public void testRegexCond()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "stilton");
        c.append("{^if (~moon_material =~ /cheese|stilton/)} The moon is made of cheese! {^else} nope. {^/if}");
        assertEquals("The moon is made of cheese!", c.toString());
    }
    
    @Test
    public void testRegexCondElsePath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "rocks");
        c.append("{^if (~moon_material =~ /cheese|stilton/)} The moon is made of cheese! {^else} nope. {^/if}");
        assertEquals("nope.", c.toString());
    }

    @Test
    public void testRegexCondNeg()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "rocks");
        c.append("{^if (~moon_material !~ /cheese|stilton/)} The moon is not made of cheese! {^else} cheese! {^/if}");
        assertEquals("The moon is not made of cheese!", c.toString());
    }
    
    @Test
    public void testTagEqualsTag()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "stilton");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material == ~cheese_type)} The moon is made of {~cheese_type} cheese! {^else} darn! {^/if}");
        assertEquals("The moon is made of stilton cheese!", c.toString());
    }

    @Test
    public void testTagEqualsTagElsePath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material == ~cheese_type)} The moon is made of {~cheese_type} cheese! {^else} darn! {^/if}");
        assertEquals("darn!", c.toString());
    }
    
    @Test
    public void testTagNotEqualsTag()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material != ~cheese_type)} The moon is not made of {~cheese_type} cheese! {^else} darn! {^/if}");
        assertEquals("The moon is not made of stilton cheese!", c.toString());
    }

    @Test
    public void testSmartTrim()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material != ~cheese_type)} The moon is not made of {~cheese_type} cheese! {^else} darn! {^/if}\n");
        assertEquals("The moon is not made of stilton cheese!", c.toString());
    }

    @Test
    public void testSmartTrim2()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material != ~cheese_type)} The moon is not made of {~cheese_type} cheese! {^else} darn! {^/if}Goobers\n");
        assertEquals("The moon is not made of stilton cheese!Goobers\n", c.toString());
    }
}
