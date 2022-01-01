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
        c.append("{.if ($moon_material == cheese)} The moon is made of cheese! {.else} The moon is not made of cheese :( {/if}");
        assertEquals(" The moon is made of cheese! ", c.toString());
    }

    @Test
    public void testAnd()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "cheese");
        c.set("sun_material", "cheez whiz");
        c.append("{% if $moon_material == cheese && $sun_material == 'cheez whiz'}pass{% else %}fail{% endif %}");
        assertEquals("pass", c.toString());

        c.set("moon_material", "rock");
        c.set("sun_material", "cheez whiz");
        assertEquals("fail", c.toString());

        c.set("moon_material", "cheese");
        c.set("sun_material", "incandescent plasma");
        assertEquals("fail", c.toString());

        c.resetTemplate();
        c.append("{% if ($moon_material == cheese) && ($sun_material == 'cheez whiz')}pass{% else %}fail{% endif %}");
        assertEquals("fail", c.toString());

        c.set("moon_material", "cheese");
        c.set("sun_material", "cheez whiz");
        assertEquals("pass", c.toString());
    }

    @Test
    public void testOr()
    {
        Chunk c = new Chunk();
        c.append("{% if $x == $y || $y == 3 %}pass{% else %}fail{% endif %}");
        c.set("x", 4);
        c.set("y", 4);

        assertEquals("pass", c.toString());

        c.set("y", 3);
        assertEquals("pass", c.toString());

        c.set("y", 42);
        assertEquals("fail", c.toString());
    }

    @Test
    public void testPrecedence()
    {
        Chunk c = new Chunk();
        c.append("{% if $x == $y || $y == 3 && $z == 7 %}pass{% else %}fail{% endif %}");
        c.set("x", 5);
        c.set("y", 6);
        c.set("z", 7);

        assertEquals("fail", c.toString());

        c.set("y", 3);
        assertEquals("pass", c.toString());

        c.set("z", 8);
        assertEquals("fail", c.toString());
    }

    @Test
    public void testGrouping()
    {
        Chunk c = new Chunk();
        c.append("{% if ($x == $y || $y == 3) && $z == 7 trim='false' %}pass{% else %}fail{% endif %}");
        c.set("x", 5);
        c.set("y", 6);
        c.set("z", 7);

        assertEquals("fail", c.toString());

        c.set("y", 3);
        assertEquals("pass", c.toString());

        c.set("z", 8);
        assertEquals("fail", c.toString());
    }

    @Test
    public void testAndWithBoolAndInequality()
    {
        Chunk c = new Chunk();
        c.append("{%if $a && $b != 0.0 %}pass{% else %}fail{% endif %}");
        c.set("a", "A");
        c.set("b", 1);

        assertEquals("pass", c.toString());

        c.unset("a");

        assertEquals("fail", c.toString());

        c.set("a", "A");
        c.set("b", "0.0");

        assertEquals("fail", c.toString());
    }

    @Test
    public void testAndWithBoolAndInequalityAndGrouping()
    {
        Chunk c = new Chunk();
        c.append("{%if $a && ($b != 0.0) %}pass{% else %}fail{% endif %}");
        c.set("a", "A");
        c.set("b", 1);

        assertEquals("pass", c.toString());

        c.resetTemplate();
        c.append("{%if ($a) && ($b != 0.0) %}pass{% else %}fail{% endif %}");

        assertEquals("pass", c.toString());

        c.unset("a");
        assertEquals("fail", c.toString());

        c.set("a", "A");
        c.set("b", "0.0");
        assertEquals("fail", c.toString());

        c.resetTemplate();
        c.append("{%if (($a) && ($b != 0.0)) %}pass{% else %}fail{% endif %}");

        c.set("b", 1);
        assertEquals("pass", c.toString());

        c.unset("a");
        assertEquals("fail", c.toString());

        c.set("a", "A");
        c.set("b", "0.0");
        assertEquals("fail", c.toString());
    }

    @Test
    public void testIfTagWithConstants()
    {
        Chunk c = new Chunk();
        c.append("{.if (true)} The moon is made of cheese! {.else} The moon is not made of cheese :( {/if}");
        assertEquals(" The moon is made of cheese! ", c.toString());
        c.resetTemplate();
        c.append("{.if (false)}was true{.else}was false{/if}");
        assertEquals("was false", c.toString());
        c.resetTemplate();
        c.append("{.if ()}was true{.else}was false{/if}");
        assertEquals("was false", c.toString());
        c.resetTemplate();
        c.append("{.if (0)}was true{.else}was false{/if}");
        assertEquals("was false", c.toString());
        c.resetTemplate();
        c.append("{.if (0.0)}was true{.else}was false{/if}");
        assertEquals("was false", c.toString());
        c.resetTemplate();
        c.append("{.if (1)}was true{.else}was false{/if}");
        assertEquals("was false", c.toString());
        c.resetTemplate();
        c.append("{.if (1.0)}was true{.else}was false{/if}");
        assertEquals("was false", c.toString());
        c.resetTemplate();
        c.append("{.if (goobers)}was true{.else}was false{/if}");
        assertEquals("was false", c.toString());
    }

    @Test
    public void testIfTagWithTruthyTags()
    {
        Chunk c = new Chunk();
        c.set("t", "true");
        c.set("f", "false");
        c.append("{.if ($t)} The moon is made of cheese! {.else} The moon is not made of cheese :( {/if}");
        assertEquals(" The moon is made of cheese! ", c.toString());
        c.resetTemplate();
        c.append("{.if ($f)}was true{.else}was false{/if}");
        assertEquals("was true", c.toString());
        c.resetTemplate();
        c.unset("f");
        c.append("{.if ($f)}was true{.else}was false{/if}");
        assertEquals("was false", c.toString());
    }

    @Test
    public void testIfTagWithCalcBool()
    {
        Chunk c = new Chunk();
        c.set("n", 30);
        c.set("m", 10);
        c.append("{.if $n|calc(>$m+$m)|bool }was true{.else}was false{/if}");
        assertEquals("was true", c.toString());
        c.resetTemplate();
        c.append("{.if $n|calc(<$m+$m)|bool }was true2{.else}was false2{/if}");
        assertEquals("was false2", c.toString());
    }

    @Test
    public void testQuotedComparison()
    {
        Chunk c = new Chunk();
        c.append("{.if ($x == \"velveeta\")}happy{/if}");
        c.append("{% if ($x == 'velveeta') %}happy{% endif %}");
        c.set("x", "velveeta");

        assertEquals("happyhappy", c.toString());
    }

    @Test
    public void testQuotedComparisonWithEscapes()
    {
        Chunk c = new Chunk();
        c.set("nasty_value","LionsAnd\nNewlines\rAnd\tTabs - '\u00A1Oh my!'");

        c.append("{.if ($nasty_value == 'LionsAnd\\nNewlines\\rAnd\\tTabs - \\'\\u00A1Oh my\\u0021\\'')}passed{.else}failed{/if}");

        assertEquals("passed", c.toString());
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
        c.append("{.if ($moon_material)} The moon is made of something! {/if}");
        assertEquals("", c.toString());
    }

    @Test
    public void testIfNotDefinedElsePath()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "something");
        c.append("{.if (!$moon_material)} The moon is not made of anything! {.else} The moon is made of something :) {/if}");
        assertEquals(" The moon is made of something :) ", c.toString());
    }

    @Test
    public void testSimpleElseIf()
    {
        // this also tests trimming first line break.
        Chunk c = new Chunk();
        c.set("moon_material", "stilton");
        c.append("{.if ($moon_material == cheese)}\n The moon is made of cheese! \n{.elseIf ($moon_material == stilton)}\n The moon is made of Stilton! \n{.else}\n The moon is not made of cheese :( \n{/if}\n");
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
        assertEquals("P:  name is Dad.  Dad's here!  Dad's Dad! ",p.toString());
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
        c.append("{% if ($moon_material != $cheese_type) %} The moon is not made of {$cheese_type} cheese! {% else %} darn! {% endif %}\n");
        assertEquals(" The moon is not made of stilton cheese! ", c.toString());
    }

    @Test
    public void testSmartTrim2()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{.if ($moon_material != $cheese_type)} The moon is not made of {$cheese_type} cheese! {.else} darn! {/if}Goobers\n");
        assertEquals(" The moon is not made of stilton cheese! Goobers\n", c.toString());
    }

    @Test
    public void testSmartTrimEnabled()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append(" {.if ($moon_material != $cheese_type)} \n The moon is not made of {$cheese_type} cheese! \n{.else} \n darn! \n {/if} \nGoobers\n");
        assertEquals(" The moon is not made of stilton cheese! \nGoobers\n", c.toString());
    }

    @Test
    public void testSmartTrimDisabled()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append(" {.if ($moon_material != $cheese_type) trim='false'} \n The moon is not made of {$cheese_type} cheese! \n{.else} \n darn! \n {/if} \nGoobers\n");
        assertEquals(" \n The moon is not made of stilton cheese! \nGoobers\n", c.toString());
    }

    @Test
    public void testTrimTrue()
    {
        Chunk c = new Chunk();
        c.set("moon_material", "roquefort");
        c.set("cheese_type", "stilton");
        c.append("{^if (~moon_material != ~cheese_type) trim='true'} The moon is not made of {~cheese_type} cheese! {^else} darn! {/if}Goobers\n");
        assertEquals("The moon is not made of stilton cheese!Goobers\n", c.toString());
    }

    @Test
    public void testTrimTrueDoubleQuoted()
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

    @Test
    public void testNestedParens()
    {
        Chunk c = new Chunk();
        c.set("a", 1);
        c.append("{.if (~a|qcalc(%2) == 0)}EVEN{.else}ODD{/if}");

        Chunk d = new Chunk();
        d.set("a", 2);
        d.append("{.if (~a|qcalc(%2) == 0)}EVEN{.else}ODD{/if}");

        assertEquals("ODDEVEN", c.toString() + d.toString());
    }

    @Test
    public void testLengthFilter()
    {
        Chunk c = new Chunk();
        c.set("x", new String[]{});
        c.append("{% if ($x|length == 0) %}pass{% else %}fail{% endif %}");

        assertEquals("pass", c.toString());
    }

    @Test
    public void testLengthFilterNoParens()
    {
        Chunk c = new Chunk();
        c.set("x", new String[]{});
        c.append("{% if $x|length == 0 %}pass{% else %}fail{% endif %}");

        assertEquals("pass", c.toString());
    }

    @Test
    public void testOnEmptyFilter()
    {
        Chunk c = new Chunk();
        c.set("a","");
        c.setOrDelete("b",null);
        c.set("c","non-empty");
        c.set("d","   \n   ");
        c.append("{% if ($a|onempty(EMPTY) == EMPTY) }EMPTY{% else %}FULL{% endif %} ");
        c.append("{% if ($b|onempty(EMPTY) == EMPTY) }EMPTY{% else %}FULL{% endif %} ");
        c.append("{% if ($c|onempty(EMPTY) == EMPTY) }EMPTY{% else %}FULL{% endif %} ");
        c.append("{% if ($d|onempty(EMPTY) == EMPTY) }EMPTY{% else %}FULL{% endif %}");

        assertEquals("EMPTY EMPTY FULL EMPTY", c.toString());
    }

}
