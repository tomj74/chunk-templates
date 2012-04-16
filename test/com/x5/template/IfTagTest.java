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
        c.append("{^if (~moon_material == cheese)} The moon is made of cheese! {^else} The moon is not made of cheese :( {/if}");
        assertEquals(" The moon is made of cheese! ", c.toString());
    }
    
    @Test
    public void testElsePath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "legos");
        c.append("{^if (~moon_material == cheese)} The moon is made of cheese! {^else} The moon is not made of cheese :( {/if}");
        assertEquals(" The moon is not made of cheese :( ", c.toString());
    }
    
    @Test
    public void testIfDefined()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "something");
        c.append("{^if (~moon_material)} The moon is made of something! {^else} The moon is not made of anything :( {/if}");
        assertEquals(" The moon is made of something! ", c.toString());
    }
    
    @Test
    public void testIfDefinedElsePath()
    {
        Chunk c = new Chunk();
        c.append("{^if (~moon_material)} The moon is made of something! {^else} The moon is not made of anything :( {/if}");
        assertEquals(" The moon is not made of anything :( ", c.toString());
    }

    @Test
    public void testIfNotDefined()
    {
        Chunk c = new Chunk();
        c.append("{^if (!moon_material)} The moon is not made of anything! {^else} The moon is made of something :) {/if}");
        assertEquals(" The moon is not made of anything! ", c.toString());
    }

    @Test
    public void testIfNotDefinedNoElse()
    {
        Chunk c = new Chunk();
        c.append("{^if (!moon_material)} The moon is not made of anything! {/if}");
        assertEquals(" The moon is not made of anything! ", c.toString());
    }
    
    @Test
    public void testElsePathNoElse()
    {
        Chunk c = new Chunk();
        c.append("{^if (~moon_material)} The moon is made of something! {/if}");
        assertEquals("", c.toString());
    }

    @Test
    public void testIfNotDefinedElsePath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "something");
        c.append("{^if (!moon_material)} The moon is not made of anything! {^else} The moon is made of something :) {/if}");
        assertEquals(" The moon is made of something :) ", c.toString());
    }
    
    @Test
    public void testSimpleElseIf()
    {
        // this also tests trimming first line break.
        Chunk c = new Chunk();
        c.set("moon_material", "stilton");
        c.append("{^if (~moon_material == cheese)}\n The moon is made of cheese! \n{^elseIf (~moon_material == stilton)}\n The moon is made of Stilton! \n{^else}\n The moon is not made of cheese :( \n{/if}\n");
        assertEquals(" The moon is made of Stilton! \n", c.toString());
    }
    
    @Test
    public void testSimpleElseIfTrimFalse()
    {
        // this also tests trimming first line break.
        Chunk c = new Chunk();
        c.set("moon_material", "stilton");
        c.append("{^if (~moon_material == cheese) trim=\"false\"}\n The moon is made of cheese! \n{^elseIf (~moon_material == stilton)}\n The moon is made of Stilton! \n{^else}\n The moon is not made of cheese :( \n{/if}\n");
        assertEquals("\n The moon is made of Stilton! \n", c.toString());
    }
    
    @Test
    public void testSimpleElseIfElsePath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "rock");
        c.append("{^if (~moon_material == cheese)} The moon is made of cheese! {^elseIf (~moon_material == stilton)} The moon is made of Stilton! {^else} The moon is not made of cheese :( {/if}");
        assertEquals(" The moon is not made of cheese :( ", c.toString());
    }
    
    @Test
    public void testSimpleElseIfThenPath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "cheese");
        c.append("{^if (~moon_material == cheese)} The moon is made of cheese! {^elseIf (~moon_material == cheese)} The moon is made of Stilton! {^else} The moon is not made of cheese :( {/if}");
        assertEquals(" The moon is made of cheese! ", c.toString());
    }
    
    //TODO tests for parent fallback of tag values in comparisons.
    // and check that trim="false" works!
    
    @Test
    public void testFallback()
    {
        Chunk p = new Chunk();
        Chunk c = new Chunk();
        p.set("child",c);
        p.set("name","Dad");
        p.append("P: {~child}");
        c.append("{^if (~name)} name is {~name}. {/if}");
        c.append("{^if (~name == Dad)} Dad's here! {/if}");

        c.set("parent_name", "Dad");
        c.append("{^if (~parent_name == ~name)} Dad's Dad! {/if}");
        assertEquals(p.toString(), "P:  name is Dad.  Dad's here!  Dad's Dad! ");
    }
    
    @Test
    public void testRegexCond()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "stilton");
        c.append("{^if (~moon_material =~ /cheese|stilton/)} The moon is made of cheese! {^else} nope. {/if}");
        assertEquals(" The moon is made of cheese! ", c.toString());
    }
    
    @Test
    public void testRegexCondElsePath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "rocks");
        c.append("{^if (~moon_material =~ /cheese|stilton/)} The moon is made of cheese! {^else} nope. {/if}");
        assertEquals(" nope. ", c.toString());
    }

    @Test
    public void testRegexCondNeg()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "rocks");
        c.append("{^if (~moon_material !~ /cheese|stilton/)} The moon is not made of cheese! {^else} cheese! {/if}");
        assertEquals(" The moon is not made of cheese! ", c.toString());
    }
    
    @Test
    public void testTagEqualsTag()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "stilton");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material == ~cheese_type)} The moon is made of {~cheese_type} cheese! {^else} darn! {/if}");
        assertEquals(" The moon is made of stilton cheese! ", c.toString());
    }

    @Test
    public void testTagEqualsTagElsePath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material == ~cheese_type)} The moon is made of {~cheese_type} cheese! {^else} darn! {/if}");
        assertEquals(" darn! ", c.toString());
    }
    
    @Test
    public void testTagNotEqualsTag()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material != ~cheese_type)} The moon is not made of {~cheese_type} cheese! {^else} darn! {/if}");
        assertEquals(" The moon is not made of stilton cheese! ", c.toString());
    }

    @Test
    public void testSmartTrim()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material != ~cheese_type)} The moon is not made of {~cheese_type} cheese! {^else} darn! {/if}\n");
        assertEquals(" The moon is not made of stilton cheese! ", c.toString());
    }

    @Test
    public void testSmartTrim2()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material != ~cheese_type)} The moon is not made of {~cheese_type} cheese! {^else} darn! {/if}Goobers\n");
        assertEquals(" The moon is not made of stilton cheese! Goobers\n", c.toString());
    }
    
    @Test
    public void testTrimTrue()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material != ~cheese_type) trim=\"true\"} The moon is not made of {~cheese_type} cheese! {^else} darn! {/if}Goobers\n");
        assertEquals("The moon is not made of stilton cheese!Goobers\n", c.toString());
    }
    
    @Test
    public void testNestedIf()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material == ~cheese_type)} {^if (~cheese_type == stilton)} Moon made of Stilton! {^/if} {^else} The moon is not made of {~cheese_type}! {/if}");
        assertEquals(" The moon is not made of stilton! ", c.toString());
    }
    
    @Test
    public void testNestedElse()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material == ~cheese_type)} {^if (~cheese_type == stilton)} Moon made of Stilton! {^else} not stilton! {/if} {^else} The moon is not made of {~cheese_type}! {/if}");
        assertEquals(" The moon is not made of stilton! ", c.toString());
    }
    
    @Test
    public void testSeriallyNestedElses()
    {
        // navigate serial nested if blocks,
        // and ignore all "else" clauses that pop up within those nested blocks
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material == ~cheese_type)} {^if (~cheese_type == stilton)} Moon made of Stilton! {^else} not stilton! {/if} {^if (~world_shape == round)} Heathen! {^else} Brethren! {/if} {^else} The moon is not made of {~cheese_type}! {/if}");
        assertEquals(" The moon is not made of stilton! ", c.toString());
    }

    @Test
    public void testUnmatchedIfs()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material == ~cheese_type)} {^if (~cheese_type == stilton)} Moon made of Stilton! {^else} not stilton! {/if} {^if (~world_shape == round)} Heathen! {^else} Brethren! {/fi} {^else} The moon is not made of {~cheese_type}! {/if}");
        String output = c.toString();
        assertTrue(output.indexOf("no matching end marker") > 0);
    }
    
    @Test
    public void testDeeplyNestedElses()
    {
        // navigate deeply nested if blocks,
        // and ignore all "else" clauses that pop up within those nested blocks
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material == ~cheese_type)}"
               +  "{^if (~cheese_type == stilton)} Moon made of Stilton! "
               +   "{^if (~world_shape == round)} Heathen! {^else} Brethren! {/if} "
               +  "{^else} not stilton! "
               +   "{^if (~world_shape == round)} Heathen! {^else} Brethren! {/if} "
               +  "{/if} "
               + "{^else}"
               + " The moon is not made of {~cheese_type}! "
               + "{/if}");
        assertEquals(" The moon is not made of stilton! ", c.toString());
    }
    
    @Test
    public void testDeeplyNestedElses2()
    {
        // navigate deeply nested if blocks,
        // and ignore all "else" clauses that pop up within those nested blocks
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "roquefort");
        c.append("{^if (~moon_material == ~cheese_type)}"
               +  "{^if (~cheese_type == stilton)} Moon made of Stilton! "
               +   "{^if (~world_shape == round)} Heathen! {^else} Brethren! {/if}"
               +  "{^else} not stilton! {~cheese_type}!"
               +   "{^if (~world_shape == round)} Heathen! {^else} Brethren! {/if}"
               +  "{/if}"
               + "{^else}"
               + " The moon is not made of {~cheese_type}! "
               + "{/if}");
        assertEquals(" not stilton! roquefort! Brethren! ", c.toString());
    }

}
