package com.x5.template;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.x5.util.ObjectDataMap;

import static org.junit.Assert.*;

public class ChunkTest
{
    @Test
    public void testSimpleDefault()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name:Bob}!");
        assertEquals("Hello, my name is Bob!",c.toString());
    }

    @Test
    public void testSimpleDefaultIgnoreWhitespace()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {% $name:Bob %}!");
        assertEquals("Hello, my name is Bob!",c.toString());
    }

    @Test
    public void testSimpleDefaultIncludeWhitespace()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name:Bob }!");
        assertEquals("Hello, my name is Bob !",c.toString());
    }

    @Test
    public void testMissingExpression()
    {
        Chunk c = new Chunk();
        c.append("Hello, {%} my {%%} name is {% %}!");
        assertEquals("Hello, {%} my {%%} name is {% %}!",c.toString());
    }

    @Test
    public void testBadExpression()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {% monkeys %}!");
        assertEquals("Hello, my name is {% monkeys %}!",c.toString());
    }

    @Test
    public void testUndefinedExpression()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {% $monkeys %}!");
        assertEquals("Hello, my name is {% $monkeys %}!",c.toString());
    }

    @Test
    public void testMissingEndPercent()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {% $name:Bob }!");
        assertEquals("Hello, my name is Bob!",c.toString());
    }

    @Test
    public void testMissingEndBrace()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {% $name:Bob !");
        assertEquals("Hello, my name is {% $name:Bob !",c.toString());
    }

    @Test
    public void testPassThru()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name}!");
        assertEquals("Hello, my name is {$name}!",c.toString());
    }

    @Test
    public void testSimpleExpand()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name}!");
        c.set("name","Harold");
        assertEquals("Hello, my name is Harold!",c.toString());
    }

    @Test
    public void testPrimitiveSet()
    {
        Chunk c = new Chunk();
        c.append("{% $d|sprintf(%.11f) %} ");
        c.append("{% $D|sprintf(%.11f) %} ");
        c.append("{% $f|sprintf(%.02f) %} ");
        c.append("{% $F|sprintf(%.02f) %} ");
        c.append("{$int} ");
        c.append("{$Int} ");
        c.append("{$long} ");
        c.append("{$Long} ");
        c.append("{$bool} ");
        c.append("{$Bool} ");
        c.append("{$char} ");
        c.append("{$Char} ");
        c.append("{$byte} ");
        c.append("{$Byte} ");
        Double d = new Double(Math.E);
        c.set("D",d);
        c.set("d",d.doubleValue());
        Float f = new Float(Math.PI);
        c.set("F",f);
        c.set("f",f.floatValue());
        Long l = new Long(4111111111111111L);
        c.set("Long",l);
        c.set("long",l.longValue());
        Integer i = new Integer(-3);
        c.set("Int",i);
        c.set("int",i.intValue());
        Character ch = new Character('C');
        c.set("Char",ch);
        c.set("char",ch.charValue());
        Boolean b = new Boolean(true);
        c.set("Bool",b);
        c.set("bool",b.booleanValue());
        Byte byt = new Byte((byte)127);
        c.set("Byte",byt);
        c.set("byte",byt.byteValue());
        assertEquals("2.71828174591 2.71828174591 3.14 3.14 -3 -3 "
                     + "4111111111111111 4111111111111111 TRUE TRUE C C 127 127 ",
                     c.toString());
    }

    @Test
    public void testSimpleExpandWithDefault()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name:Bob}!");
        c.set("name","Harold");
        assertEquals("Hello, my name is Harold!",c.toString());
    }

    @Test
    public void testSimpleExpandWithTagDefault()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name:$full_name}!");
        c.unset("name");
        c.set("full_name","Bob Johnson");
        assertEquals("Hello, my name is Bob Johnson!",c.toString());
    }

    @Test
    public void testSimpleExpandWithTagDefaultAltSyntax()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name:~full_name}!");
        c.unset("name");
        c.set("full_name","Bob Johnson");
        assertEquals("Hello, my name is Bob Johnson!",c.toString());
    }

    @Test
    public void testFilteredDefault()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name:O'Reilly|qs}!");
        assertEquals("Hello, my name is O\\'Reilly!",c.toString());
    }

    @Test
    public void testUnfilteredDefault()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name|qs:O'Reilly}!");
        assertEquals("Hello, my name is O'Reilly!",c.toString());
    }

    @Test
    public void testSimpleRecursion()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name}!");
        c.set("name", "{$username}");
        c.set("username", "Bob");
        assertEquals("Hello, my name is Bob!", c.toString());
    }

    @Test
    public void testSimpleRecursionAlt()
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
        c.append("Hello, my name is {$name}!");
        c.set("name", "Bob and my cat is also {$name}");
        assertTrue(c.toString().indexOf("max template recursions") > 0);
    }

    @Test
    public void testParentFallback()
    {
        Chunk p = new Chunk();
        Chunk c = new Chunk();
        p.set("name", "Dad");
        p.set("child", c);
        p.append("{$child}");
        c.append("Hello, my name is {$name}!");
        assertEquals(p.toString(),"Hello, my name is Dad!");
    }

    @Test
    public void testChildPrecedence()
    {
        Chunk p = new Chunk();
        Chunk c = new Chunk();
        p.set("name", "Dad");
        p.set("child", c);
        p.append("{$child}");
        c.append("Hello, my name is {$name}!");
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
        g.append("G: {$parent}");
        p.append("P: {$child}");
        c.append("C: Hello, my name is {$name}!");
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
        g.append("G: {$parent}  Grandpa is {$name}!");
        p.append("P: {$child}");
        c.append("C: Hello, my name is {$name}!");

        p.set("name", "Parent");

        assertEquals(g.toString(),"G: P: C: Hello, my name is Parent!  Grandpa is Grandpa!");
    }

    @Test
    public void testIfNull()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name}!");

        String name = null;
        c.set("name",name,"UNKNOWN");

        assertEquals(c.toString(), "Hello, my name is UNKNOWN!");
    }

    @Test
    public void testNullToEmptyString()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name}!");

        String name = null;
        c.set("name",name);

        assertEquals(c.toString(), "Hello, my name is !");
    }

    @Test
    public void testNullToPassThru()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name}!");

        String name = null;
        c.setOrDelete("name",name);

        assertEquals(c.toString(), "Hello, my name is {$name}!");
    }

    @Test
    public void testSetOrDelete()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name}!");

        c.set("name","Bob");

        String name = null;
        c.setOrDelete("name",name);

        assertEquals(c.toString(), "Hello, my name is {$name}!");
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
    public void testBackticksNewSyntax()
    {
        // TODO: add some tests for backtick edge cases
        // (eg, what if $id is not defined, what if $name21 is not defined)
        Chunk c = new Chunk();
        c.set("name", "Bob");
        c.set("name21", "Rob");
        c.set("id", "21");
        c.append("Hello, my name is {$name`$id`}!");

        assertEquals(c.toString(), "Hello, my name is Rob!");
    }

    @Test
    public void testFilteredBacktick()
    {
        // TODO: add some tests for backtick edge cases
        // (eg, what if ~id is not defined, what if ~name21 is not defined)
        Chunk c = new Chunk();
        c.set("name", "Bob");
        c.set("name22", "Rob");
        c.set("id", "21");
        c.append("Hello, my name is {$name`$id|qcalc(+1)`}!");

        assertEquals(c.toString(), "Hello, my name is Rob!");
    }

    @Test
    public void testUncappedLiteral()
    {
        Chunk c = new Chunk();
        c.append("Pass through! {~gronk:bubbles!} <!-- {^literal} --> passing through {~gronk:} Pass on!");

        assertEquals("Pass through! bubbles! <!-- {^literal} --> passing through {~gronk:} Pass on!", c.toString());
    }

    @Test
    public void testUncappedLiteralInFile()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("chunk_test#uncapped_literal");

        assertEquals("Scooby Doo says ruff ruff!\n{% literal %}\n{#}\n", c.toString());
    }

    @Test
    public void testLiteralInFile()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("chunk_test#literal_test");

        assertEquals("bye\n<!-- {.literal} -->\n{$tags:hello}\n{#confuser}I am the CONFUSER{#}\n<!-- {/literal} -->\nbye\n", c.toString());
    }

    @Test
    public void testNewLiteralInFile()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("chunk_test#new_literal_test");

        assertEquals("bye\n<!-- {% literal %} -->\n{$tags:hello}\n{#confuser}I am the CONFUSER{#}\n<!-- {% endliteral %} -->\nbye\n", c.toString());
    }

    @Test
    public void testLiteral()
    {
        Chunk c = new Chunk();
        c.append("Pass through! {~gronk:bubbles!} {!--comment--} <!-- {^literal} --> {!--comment--} passing through {~gronk:} <!-- {^} --> Pass on {~process_me:happily}!");

        assertEquals(c.toString(), "Pass through! bubbles!  <!-- {^literal} --> {!--comment--} passing through {~gronk:} <!-- {^} --> Pass on happily!");
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
    public void testLiteralNewSyntax()
    {
        Chunk c = new Chunk();
        c.append("Pass through! {~gronk:bubbles!} <!-- {.literal} --> passing through {~gronk:} <!-- {^} --> Pass on {~process_me:happily}!");

        assertEquals(c.toString(), "Pass through! bubbles! <!-- {.literal} --> passing through {~gronk:} <!-- {^} --> Pass on happily!");
    }

    @Test
    public void testDefaultIsInclude()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("chunk_test#default_include");
        assertEquals("Hello Include!\n\n", c.toString());
    }

    @Test
    public void testIncludeShorthand()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk();
        c.append("{+chunk_test#no_widgets}");
        assertEquals("<i>No widgets!</i>\n", c.toString());
    }

    @Test
    public void testIncludeIfShorthand()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk();
        c.append("{+(!widgets)chunk_test#no_widgets}");
        assertEquals("<i>No widgets!</i>\n", c.toString());
    }

    @Test
    public void testSnippetRoundTrip()
    {
        String tpl = "xyz {~xyz:} {!-- old macro syntax: --} {* MACRO *} {*} {^loop} {/loop} _[token] {_[token %s %s],~arg1,~arg2}";
        Snippet testSnippet = Snippet.getSnippet(tpl);
        String recombobulated = testSnippet.toString();
        assertEquals(tpl, recombobulated);
    }

    @Test
    public void testSnippetRoundTripAlt()
    {
        String tpl = "xyz {$xyz:} {!-- old macro syntax: --} {* MACRO *} {*} {.loop} {/loop} _[token] {_[token %s %s],$arg1,$arg2}";
        Snippet testSnippet = Snippet.getSnippet(tpl);
        String recombobulated = testSnippet.toString();
        assertEquals(tpl,recombobulated);
    }

    @Test
    public void testCommentStripping()
    {
        String tpl = "{!-- comment 1 --}ABC{!-- comment 2 --}123!";
        Chunk c = new Chunk();
        c.append(tpl);

        assertEquals("ABC123!",c.toString());
    }

    @Test
    public void testCommentStrippingWithLinebreaks()
    {
        String tpl = "{!-- comment 1 --}\nABC\n{!-- comment 2 --}\n123!\n keep me {!-- take me --}\n {!-- take me --} keep me too\n";
        Chunk c = new Chunk();
        c.append(tpl);

        assertEquals("ABC\n123!\n keep me \n  keep me too\n",c.toString());
    }

    @Test
    public void testJavascriptHeadFake()
    {
        String tpl = "<script>$(document).ready(function(){$('selector').doSomething(':','{$tag:test}');});</script>";
        Chunk c = new Chunk();
        c.append(tpl);

        assertEquals("<script>$(document).ready(function(){$('selector').doSomething(':','test');});</script>",c.toString());
    }

    @Test
    public void testBlockSpanningAppends()
    {
        String tpl = "{.exec @inline xml}";
        String xml = "<values><item><description>hello</description></item></values>";
        String tpl2 = "{.body}{$item.description}{/body}{/exec}";

        Chunk c = new Chunk();
        c.append(tpl);
        c.append(xml);
        c.append(tpl2);

        assertEquals("hello", c.toString());
    }

    @Test
    public void testBlockSpanningAppendsWithChunk()
    {
        String tpl = "{.exec @inline xml}";
        String xml = "<values><item><description>hello</description></item></values>";
        String tpl2 = "{.body}{$item.description}";
        Chunk inTheMiddle = new Chunk();
        String tpl3 = "{/body}{/exec}";

        inTheMiddle.set("hello", "cello");
        inTheMiddle.append(" {$hello}");

        Chunk c = new Chunk();
        c.append(tpl);
        c.append(xml);
        c.append(tpl2);
        c.append(inTheMiddle);
        inTheMiddle.set("hello", "fellow");
        c.append(inTheMiddle);
        c.append(tpl3);

        assertEquals("hello fellow fellow", c.toString());
    }

    @Test
    public void commentMagicWhitespaceTest()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("whitespace_test");

        String targetOutput = "Line\n    Line\n    Line\n    Line\n    {$tag}\n    Line\n\n";

        assertEquals(targetOutput, c.toString());
    }

    @Test
    public void commentAfterIfTest()
    {
        Theme theme = new Theme("test/base");
        Chunk c = theme.makeChunk("whitespace_test#comment_after_if");

        String targetOutput = "    LineA\n    LineB\n    LineC\n";

        assertEquals(targetOutput, c.toString());
    }

    @Test
    public void escapeMagicDefaultTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x:\\.000314159|sprintf(%.2e)}");

        assertEquals("3.14e-04",c.toString());
    }

    @Test
    public void simplePOJOTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x.name} {$x.age} {$x.e|sprintf(%.02f)} {$x.pi|sprintf(%.02f)} {$x.is_active:FALSE}");
        c.set("x", new Thing("Bob",28,true));

        assertEquals("Bob 28 2.72 3.14 TRUE", c.toString());
    }

    @Test
    public void objectRendersAsStringPojoTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x}::{$x.big:}");
        c.set("x", new BigDecimal("9999999999999999999999999999999999999999"));

        assertEquals("9999999999999999999999999999999999999999::", c.toString());

        c.set("x", new Thing("Bob", 28, true));
        assertEquals("OBJECT:com.x5.template.ChunkTest$Thing::", c.toString());

        c.set("x", new BigThing("Bob", 28, true));
        assertEquals("Thing-Bob::9999999999999999999999999999999999999999", c.toString());
    }

    @Test
    public void canAccessInheritedPublicPojoFields()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();

        c.set("x", new BigThing("Bob", 28, true));
        c.append("{$x.age}");

        assertEquals("28", c.toString());
    }

    @Test
    public void doubleCapsuleTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$user_name} {$x.user_name}");
        OldThing userA = new OldThing("Bob",28,true);
        OldThing userB = new OldThing("Joe",30,true);
        c.addData(userA);
        c.addData(userB,"x");

        assertEquals("Bob Joe", c.toString());
    }

    @Test
    public void nestedCapsuleTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x.user_name} {$x.user_child.user_name}");
        c.append(" {$user_name} {$user_child.user_name}");
        OldThing userA = new OldThing("Bob",28,true);
        OldThing userB = new OldThing("Joe",30,true);
        userA.setChild(userB);
        c.set("x",userA);
        c.addData(userA);

        assertEquals("Bob Joe Bob Joe", c.toString());
    }

    @Test
    public void capsuleIntTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$user_age} {$x.user_age} {$y.user_age}");
        OldThing userA = new OldThing("Bob",27,true);
        c.addData(userA);
        c.addData(userA,"y");
        c.set("x",userA);
        userA.setAge(28);

        assertEquals("27 28 28", c.toString());
    }

    @Test
    public void doubleCapsuleTest2()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$a.user_name} {$b.user_name}");
        OldThing userA = new OldThing("Bob",28,true);
        OldThing userB = new OldThing("Joe",30,true);
        c.set("a",userA);
        c.set("b",userB);

        assertEquals("Bob Joe", c.toString());
    }

    @Test
    public void simpleBeanTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x.name} {$x.age} {$x.e|sprintf(%.02f)} {$x.pi|sprintf(%.02f)} {$x.is_active:FALSE} {$x.secret:SECRET-IS-SAFE}");

        ThingBean bean = new ThingBean();
        bean.setAge(28);
        bean.setName("Bob");
        bean.setActive(true);

        c.setToBean("x", bean);

        assertEquals("Bob 28 2.72 3.14 TRUE SECRET-IS-SAFE", c.toString());
    }

    @Test
    public void beanInBeanTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x.name} {$x.boss.name} {$x.children|type} {% loop in $x.children as $child %}{$child.name|s/ /-/}{% divider %} {% endloop %}");

        ThingBean bean = new ThingBean();
        bean.setAge(28);
        bean.setName("Bob");
        bean.setActive(true);

        c.setToBean("x", bean);

        assertEquals("Bob Bob LIST copy-A copy-B", c.toString());
    }

    @Test
    public void arrayOfPOJOTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{.loop in $list as $x}{$x.name} {$x.age} {$x.pi|sprintf(%.02f)} {$x.is_active:FALSE}{/loop}");
        c.set("list", new Thing[]{new Thing("Bob",28,true)});

        assertEquals("Bob 28 3.14 TRUE", c.toString());
    }

    @Test
    public void listOfPOJOTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{.loop in $list as $x}{$x.name} {$x.age} {$x.pi|sprintf(%.02f)} {$x.is_active:FALSE}{/loop}");
        List<Thing> list = new ArrayList<Thing>();
        list.add(new Thing("Bob",28,true));
        c.set("list", list);

        assertEquals("Bob 28 3.14 TRUE", c.toString());
    }

    @Test
    public void POJOFieldVisibilityTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x.name} {$x.age} {$x.pi|sprintf(%.02f)} {$x.is_active:FALSE} {$x.hidden:invisible} {$x.hiddentwo:invisible}");
        c.set("x", new Thing("Bob", 28, true));

        assertEquals("Bob 28 3.14 TRUE invisible invisible", c.toString());
    }

    @Test
    public void snakeCaseTest()
    {
        assertEquals("snake_xml_case", ObjectDataMap.splitCamelCase("SnakeXMLCase"));
        assertEquals("snake_xml", ObjectDataMap.splitCamelCase("SnakeXML"));
        assertEquals("snake_xm", ObjectDataMap.splitCamelCase("SnakeXM"));
        assertEquals("snake_x_ma", ObjectDataMap.splitCamelCase("SnakeXMa"));
        assertEquals("snake_x_maze", ObjectDataMap.splitCamelCase("SnakeXMaze"));
        assertEquals("snake_maze", ObjectDataMap.splitCamelCase("SnakeMaze"));
        assertEquals("snake2_case", ObjectDataMap.splitCamelCase("Snake2Case"));
        assertEquals("snake2_xml_case", ObjectDataMap.splitCamelCase("Snake2XMLCase"));
    }

    @Test
    public void circularPOJOTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x.name} {$x.age} {$x.pi|sprintf(%.02f)} {$x.is_active:FALSE} {$x.boss.name}\n");
        c.append("{.loop in $x.children as $child}{$child.name} {/loop}");
        c.set("x", new CircularThing("Bob", 28, false));

        assertEquals("Bob 28 3.14 FALSE Bob\nBob Bob ", c.toString());
    }

    @Test
    public void POJOLoopTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{% loop in $things as $x %}{$x.name}{% divider %} {% endloop %}");
        Thing x = new Thing("Alice", 28, true);
        Thing y = new Thing("Bob", 28, true);
        Thing z = new Thing("Carol", 28, true);
        List<Thing> things = new ArrayList<Thing>();
        things.add(x);
        things.add(y);
        things.add(z);
        c.set("things", things);

        assertEquals("Alice Bob Carol", c.toString());
    }

    @Test
    public void BeanLoopTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{% loop in $things as $x %}{$x.name}{% divider %} {% endloop %}");
        ThingBean x = new ThingBean();
        ThingBean y = new ThingBean();
        ThingBean z = new ThingBean();
        x.setName("Alice");
        y.setName("Bob");
        z.setName("Carol");
        List<ThingBean> things = new ArrayList<ThingBean>();
        things.add(x);
        things.add(y);
        things.add(z);
        c.set("things", things);

        assertEquals("Alice Bob Carol", c.toString());
    }

    @Test
    public void BeanLoopWorkaroundTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{% loop in $things as $x %}{$x.name}{% divider %} {% endloop %}");
        ThingBean x = new ThingBean();
        ThingBean y = new ThingBean();
        ThingBean z = new ThingBean();
        x.setName("Alice");
        y.setName("Bob");
        z.setName("Carol");
        List<java.util.Map> things = new ArrayList<java.util.Map>();
        things.add(ObjectDataMap.wrapBean(x));
        things.add(ObjectDataMap.wrapBean(y));
        things.add(ObjectDataMap.wrapBean(z));
        c.set("things", things);

        assertEquals("Alice Bob Carol", c.toString());
    }

    /**
     * for addData test
     */
    public static class OldThing implements com.x5.util.DataCapsule
    {
        private String name;
        private int age;
        private boolean isActive;
        private OldThing childThing;

        public OldThing(String name, int age, boolean isActive)
        {
            this.name = name;
            this.age = age;
            this.isActive = isActive;
        }

        public void setAge(int age)
        {
            this.age = age;
        }

        public void setChild(OldThing child)
        {
            this.childThing = child;
        }

        public String getExportPrefix()
        {
            return "user";
        }

        public String getName()
        {
            return name;
        }

        public OldThing getChild()
        {
            return childThing;
        }

        public int getAge()
        {
            return age;
        }

        public boolean isActive()
        {
            return isActive;
        }

        public String[] getExports()
        {
            return new String[]{
                "getName",
                "getAge",
                "isActive",
                "getChild"
            };
        }
    }

    /**
     * for POJO test
     */
    public static class Thing
    {
        String name;
        public int age;
        double pi = Math.PI;
        Double e = new Double(Math.E);
        boolean isActive;
        // these fields should not be visible to the template
        protected String hidden;
        private String hiddentwo;

        public Thing(String name, int age, boolean isActive)
        {
            this.name = name;
            this.age = age;
            this.isActive = isActive;
            // these fields should not be visible to the template
            this.hidden = "hidden";
            this.hiddentwo = "hidden";
        }
    }

    public static class BigThing extends Thing
    {
        BigDecimal big = new BigDecimal("9999999999999999999999999999999999999999");

        public BigThing(String name, int age, boolean isActive)
        {
            super(name, age, isActive);
        }

        public String toString()
        {
            return "Thing-" + name;
        }
    }

    /**
     * for Circular-references POJO test
     */
    public static class CircularThing
    {
        String name;
        int age;
        double pi = Math.PI;
        boolean isActive;
        CircularThing boss;
        CircularThing[] children;
        Map<String,String> map;

        public CircularThing(String name, int age, boolean isActive)
        {
            this.name = name;
            this.age = age;
            this.isActive = isActive;
            // I am my own boss!
            this.boss = this;
            // I traveled back in time, I am my own dad!
            this.children = new CircularThing[]{this,this};
        }

        public void setMap(Map<String,String> map) {
            this.map = map;
        }
    }

    /**
     * for bean tests
     */
    public static class ThingBean implements java.io.Serializable, Cloneable
    {
        public final double PI = Math.PI;
        private String name;
        private int age;
        private double pi = Math.PI;
        private Double e = new Double(Math.E);
        private boolean isActive;
        private ThingBean boss;
        private ThingBean[] children;

        private String secret = "BIG SECRET";

        public ThingBean()
        {
            this.boss = this;
            ThingBean childA = this;
            ThingBean childB = this;
            try {
                childA = (ThingBean)this.clone();
                childB = (ThingBean)this.clone();
                childA.setName(getName() == null ? "copy A" : getName() + " copy A");
                childB.setName(getName() == null ? "copy B" : getName() + " copy B");
            } catch (CloneNotSupportedException e) {
            }
            this.children = new ThingBean[]{childA,childB};
        }

        public String getName()
        {
            return name;
        }

        public int getAge()
        {
            return age;
        }

        public double getPi()
        {
            return pi;
        }

        public Double getE()
        {
            return e;
        }

        public boolean isActive()
        {
            return isActive;
        }

        public ThingBean getBoss()
        {
            return boss;
        }

        public ThingBean[] getChildren()
        {
            return children;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public void setAge(int age)
        {
            this.age = age;
        }

        public void setPi(double pi)
        {
            this.pi = pi;
        }

        public void setActive(boolean isActive)
        {
            this.isActive = isActive;
        }

        public void setBoss(ThingBean boss)
        {
            this.boss = boss;
        }

        public void setChildren(ThingBean[] children)
        {
            this.children = children;
        }

        private void setSecret(String secret)
        {
            this.secret = secret;
        }

        private String getSecret()
        {
            return secret;
        }
    }
}
