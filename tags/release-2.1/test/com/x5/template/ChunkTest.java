package com.x5.template;

import java.util.ArrayList;
import java.util.List;

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
    public void testSimpleExpandWithTagDefault()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {~name:~full_name}!");
        c.unset("name");
        c.set("full_name","Bob Johnson");
        assertEquals("Hello, my name is Bob Johnson!",c.toString());
    }

    @Test
    public void testSimpleExpandWithTagDefaultAltSyntax()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name:$full_name}!");
        c.unset("name");
        c.set("full_name","Bob Johnson");
        assertEquals("Hello, my name is Bob Johnson!",c.toString());
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
    public void testSimpleRecursionAlt()
    {
        Chunk c = new Chunk();
        c.append("Hello, my name is {$name}!");
        c.set("name", "{$username}");
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
    public void testBackticksNewSyntax()
    {
        // TODO: add some tests for backtick edge cases
        // (eg, what if ~id is not defined, what if ~name21 is not defined)
        Chunk c = new Chunk();
        c.set("name", "Bob");
        c.set("name21", "Rob");
        c.set("id", "21");
        c.append("Hello, my name is {~name`$id`}!");
        
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
        String tpl = "xyz {~xyz:} {!-- old macro syntax: --} {* MACRO *} {*} {^loop} {/loop} _[token] {_[token %s %s],~arg1,~arg2}";
        Snippet testSnippet = Snippet.getSnippet(tpl);
        String recombobulated = testSnippet.toString();
        assertEquals(tpl,recombobulated);
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
        
        String targetOutput = "    Line\n    Line\n    Line\n";
        
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
        c.append("{$x.name} {$x.age} {$x.pi|sprintf(%.02f)} {$x.is_active:FALSE}");
        c.set("x", new Thing("Bob",28,true));
        
        assertEquals("Bob 28 3.14 TRUE", c.toString());
    }
    
    @Test
    public void simpleBeanTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x.name} {$x.age} {$x.pi|sprintf(%.02f)} {$x.is_active:FALSE} {$x.secret:SECRET-IS-SAFE}");
        
        ThingBean bean = new ThingBean();
        bean.setAge(28);
        bean.setName("Bob");
        bean.setActive(true);
        
        c.setToBean("x", bean);
        
        assertEquals("Bob 28 3.14 TRUE SECRET-IS-SAFE", c.toString());
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
        c.set("x", new Thing("Bob",28,true));
        
        assertEquals("Bob 28 3.14 TRUE invisible invisible", c.toString());
    }
    
    @Test
    public void circularPOJOTest()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x.name} {$x.age} {$x.pi|sprintf(%.02f)} {$x.is_active:FALSE} {$x.boss.name}\n");
        c.append("{.loop in $x.children as $child}{$child.name} {/loop}");
        c.set("x", new CircularThing("Bob",28,false));
        
        assertEquals("Bob 28 3.14 FALSE Bob\nBob Bob ", c.toString());
    }
    
    /**
     * for POJO test
     */
    public static class Thing
    {
        String name;
        int age;
        double pi = Math.PI;
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
    }
    
    /**
     * for bean tests
     */
    public static class ThingBean implements java.io.Serializable
    {
        private String name;
        private int age;
        private double pi = Math.PI;
        private boolean isActive;
        private ThingBean boss;
        private ThingBean[] children;
        
        private String secret = "BIG SECRET";
        
        public ThingBean()
        {
            this.boss = this;
            this.children = new ThingBean[]{this,this};
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
