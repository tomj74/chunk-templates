package com.x5.template;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import com.x5.template.filters.BasicFilter;
import com.x5.template.filters.FilterArgs;
import com.x5.template.filters.ObjectFilter;

import static org.junit.Assert.*;

public class FilterTest
{
    @Test
    public void testSimpleIndent()
    {
        Chunk c = new Chunk();
        c.append("{~hello:hello|indent(3)}");
        assertEquals("   hello",c.toString());
    }

    @Test
    public void testMultilineIndent()
    {
        Chunk c = new Chunk();
        c.append("{~hello|indent(3)}");
        c.set("hello","String\nWith\nMany\nLines\n");
        assertEquals("   String\n   With\n   Many\n   Lines\n", c.toString());
    }

    @Test
    public void testMultilineIndentNoTrailingLinefeed()
    {
        Chunk c = new Chunk();
        c.append("{~hello|indent(3)}");
        c.set("hello","String\nWith\nMany\nLines");
        assertEquals("   String\n   With\n   Many\n   Lines", c.toString());
    }

    @Test
    public void testIndentNotSpaces()
    {
        Chunk c = new Chunk();
        c.append("{~hello|indent(3,_)}");
        c.set("hello","String\nWith\nMany\nLines\n");
        assertEquals("___String\n___With\n___Many\n___Lines\n", c.toString());
    }

    @Test
    public void testIndentNotSingleChar()
    {
        Chunk c = new Chunk();
        c.append("{~hello|indent(3,-=)}");
        c.set("hello","String\nWith\nMany\nLines\n");
        assertEquals("-=-=-=String\n-=-=-=With\n-=-=-=Many\n-=-=-=Lines\n", c.toString());
    }

    @Test
    public void testIndentWinCR()
    {
        Chunk c = new Chunk();
        c.append("{~hello|indent(3)}");
        c.set("hello","String\r\nWith\r\nMany\r\nLines\r\n");
        assertEquals("   String\r\n   With\r\n   Many\r\n   Lines\r\n", c.toString());
    }

    @Test
    public void testIndentMacCR()
    {
        Chunk c = new Chunk();
        c.append("{$hello|indent(3)}");
        c.set("hello","String\r\rWith\r\rMany\r\rLines\r\r");
        assertEquals("   String\r\r   With\r\r   Many\r\r   Lines\r\r", c.toString());
    }

    @Test
    public void testSimpleOndefined()
    {
        Chunk c = new Chunk();
        c.append("Output: {$hello|ondefined(greetings!)}");
        c.set("hello", "hello");
        assertEquals("Output: greetings!", c.toString());
    }

    @Test
    public void testPassThruOndefined()
    {
        Chunk c = new Chunk();
        c.append("Output: {$hello|ondefined(greetings!)}");
        assertEquals("Output: {$hello|ondefined(greetings!)}", c.toString());
    }

    @Test
    public void testVaporizingOndefined()
    {
        Chunk c = new Chunk();
        c.append("Output: {$hello|ondefined(greetings!):}");
        assertEquals("Output: ", c.toString());
    }

    @Test
    public void testOndefinedParentFallback()
    {
        Chunk c = new Chunk();
        Chunk p = new Chunk();

        p.append("{~child}");
        p.set("child", c);
        p.set("hello", "hello"); // defined in parent, still counts as defined in child

        c.append("Output: {$hello|ondefined(greetings!)}");
        assertEquals("Output: greetings!", p.toString());
    }

    @Test
    public void testSimpleOnMatch()
    {
        Chunk c = new Chunk();
        // ok, so this onmatch is not all that simple...
        c.append("Output: {$hello|onmatch(/E.*O/i,greetings!)nomatch(darn!)}");
        c.set("hello", "hello");
        assertEquals("Output: greetings!", c.toString());
    }

    @Test
    public void testOnMatchNoMatch()
    {
        Chunk c = new Chunk();
        c.append("Output: {$hello|onmatch(/E.*O/i,greetings!)nomatch(yay!)}");
        c.set("hello", "hella");
        assertEquals("Output: yay!", c.toString());
    }

    @Test
    public void testOnMatchParentFallback()
    {
        // this also tests the case-insensitive matching regex flag //i
        Chunk c = new Chunk();
        Chunk p = new Chunk();

        p.append("P: {$child}");
        p.set("child", c);
        p.set("hello", "hello");

        c.append("Output: {$hello|onmatch(/E.*O/i,greetings!)nomatch(darn!)}");
        assertEquals("P: Output: greetings!", p.toString());
    }

    @Test
    public void testOnMatchFirstMatch()
    {
        Chunk c = new Chunk();

        c.set("hello", "catch");

        c.append("Output: {$hello|onmatch(/dog/,MatchOne,/cat/,MatchTwo,/catch/,MatchThree)nomatch(darn!)}");

        assertEquals("Output: MatchTwo", c.toString());
    }

    @Test
    public void testMD5()
    {
        Chunk c = new Chunk();

        c.append("{$xyz:XYZ|md5}");

        assertEquals("e65075d550f9b5bf9992fa1d71a131be", c.toString());
    }

    @Test
    public void testSHA1()
    {
        Chunk c = new Chunk();
        c.append("{$xyz:XYZ|sha1}");
        assertEquals("717c4ecc723910edc13dd2491b0fae91442619da", c.toString());
    }

    @Test
    public void testBase64()
    {
        Chunk c = new Chunk();
        c.set("xyz","Some very long string with Crazy Characters\u00EE!");
        c.append("{$xyz|base64}");
        assertEquals("U29tZSB2ZXJ5IGxvbmcgc3RyaW5nIHdpdGggQ3JhenkgQ2hhcmFjdGVyc8OuIQ==", c.toString());
    }

    @Test
    public void testBase64RoundTrip()
    {
        Chunk c = new Chunk();
        String src = "Some very long string with Crazy Characters\u00EE!";
        c.set("xyz",src);
        c.append("{$xyz|base64|base64decode}");
        assertEquals(src, c.toString());
    }

    @Test
    public void testSimpleRegexReplace()
    {
        Chunk c = new Chunk();
        c.set("xyz", "Lemon Lion Liar Loon Lenore Forlorn");
        c.append("{$xyz|s/L[^ ]*?n/Chunky/g}");
        assertEquals("Chunky Chunky Liar Chunky Chunkyore Forlorn", c.toString());
    }

    @Test
    public void testSimpleFormat()
    {
        Chunk c = new Chunk();
        c.set("howmuch","300020.4151233");
        c.append("{$howmuch|sprintf($%,.2f)}");
        assertEquals("$300,020.41", c.toString());
    }

    @Test
    public void testSimpleCalc()
    {
        Chunk c = new Chunk();
        c.set("howmuch","30");
        c.append("{$howmuch|calc(*10+4)|sprintf(%.0f)}");
        assertEquals("304",c.toString());

        c.append(" {$howmuch|calc(\"*10+4\",\"%.0f\")}");
        assertEquals("304 304", c.toString());
    }

    @Test
    public void testCalcSpecial()
    {
        Chunk c = new Chunk();
        c.set("a",20);
        c.set("b",30);
        c.set("c",40);

        c.append("{.calc(\"$x+$y+$z\",$a,$b,$c)|sprintf(%.0f)}");
        assertEquals("90",c.toString());
    }

    @Test
    public void testQuickCalc()
    {
        Chunk c = new Chunk();
        c.set("howmuch","30");
        c.append("{~howmuch|qcalc(*10)} {~howmuch|qcalc(/10)} {~howmuch|qcalc(+10)} {~howmuch|qcalc(-10)} {~howmuch|qcalc(%14)} {~howmuch|qcalc(^2)}");
        assertEquals("300 3 40 20 2 900",c.toString());
    }

    @Test
    public void testNth()
    {
        Chunk c = new Chunk();
        StringBuilder template = new StringBuilder();
        for (int i=1; i<=24; i++) {
            char x = (char)('A'+i-1);
            c.set(Character.toString(x), i);
            if (i>1) template.append(" ");
            template.append(c.makeTag(x+"|th"));
        }
        // {~A|th} {~B|th} {~C|th} ...
        c.append(template.toString());
        assertEquals(c.toString(),
                "1st 2nd 3rd 4th 5th 6th 7th 8th 9th 10th 11th 12th 13th 14th 15th 16th 17th 18th 19th 20th 21st 22nd 23rd 24th");
    }

    @Test
    public void testSelected()
    {
        Chunk c = new Chunk();
        c.append("<option value=\"{~val}\" {~val|selected(x)}>{~val|uc}</option>");
        c.set("val","x");
        assertEquals("<option value=\"x\"  selected=\"selected\" >X</option>",c.toString());
    }

    @Test
    public void testNotSelected()
    {
        Chunk c = new Chunk();
        c.append("<option value=\"{~val}\" {~val|selected(x)}>{~val|uc}</option>");
        c.set("val","z");
        assertEquals("<option value=\"z\" >Z</option>",c.toString());
    }

    @Test
    public void testSelectedMatchVariable()
    {
        Chunk c = new Chunk();
        c.append("<option value=\"{~val}\" {~val|selected(~var2)}>{~val|uc}</option>");
        c.set("val","x");
        c.set("var2","x");
        assertEquals("<option value=\"x\"  selected=\"selected\" >X</option>",c.toString());
    }

    @Test
    public void testNotSelectedMatchVariable()
    {
        Chunk c = new Chunk();
        c.append("<option value=\"{~val}\" {~val|selected(~var2)}>{~val|uc}</option>");
        c.set("val","z");
        c.set("var2","x");
        assertEquals("<option value=\"z\" >Z</option>",c.toString());
    }

    @Test
    public void testSelectedMatchVariableAlt()
    {
        Chunk c = new Chunk();
        c.append("<option value=\"{$val}\" {$val|selected($var2)}>{$val|uc}</option>");
        c.set("val","x");
        c.set("var2","x");
        assertEquals("<option value=\"x\"  selected=\"selected\" >X</option>",c.toString());
    }

    @Test
    public void testNotSelectedMatchVariableAlt()
    {
        Chunk c = new Chunk();
        c.append("<option value=\"{$val}\" {~val|selected($var2)}>{$val|uc}</option>");
        c.set("val","z");
        c.set("var2","x");
        assertEquals("<option value=\"z\" >Z</option>",c.toString());
    }

    @Test
    public void testURLEncode()
    {
        Chunk c = new Chunk();
        c.set("abc","% %");
        c.append("test: {~abc|urlencode}");
        assertEquals("test: %25+%25",c.toString());
    }

    @Test
    public void testURLDecode()
    {
        Chunk c = new Chunk();
        c.set("abc","%25+%7E");
        c.append("test: {~abc|urldecode}");
        assertEquals("test: % ~",c.toString());
    }

    @Test
    public void testURLRoundTrip()
    {
        Chunk c = new Chunk();
        c.set("abc","% %~");
        c.append("test: {~abc|urlencode|urldecode}");
        assertEquals("test: % %~",c.toString());
    }

    @Test
    public void testURLRoundTripReverse()
    {
        Chunk c = new Chunk();
        c.set("abc","%25+%7E");
        c.append("test: {~abc|urldecode|urlencode}");
        assertEquals("test: %25+%7E",c.toString());
    }

    @Test
    public void testXMLEscape()
    {
        Chunk c = new Chunk();
        // should strip final (invalid) character, convert higher-than-FF chars to
        // hex-escaped entities.
        c.set("abc","& ' \" <Tag> \u00AE \u21D4 \u2122 "+Character.valueOf((char)2));
        c.append("test: {~abc|xmlescape}");
        assertEquals("test: &amp; &apos; &quot; &lt;Tag&gt; \u00AE &#x21d4; &#x2122; ", c.toString());
    }

    @Test
    public void testApplyFilter()
    {
        assertEquals("&lt;p&gt;", Filter.applyFilter(new Chunk(),"htmlescape","<p>"));
    }

    @Test
    public void testXMLUnescape()
    {
        Chunk c = new Chunk();
        c.set("abc","&amp; &apos; \" &lt;Tag&gt; &#123; &#x03BB;");
        c.append("test: {~abc|unescape}");
        assertEquals("test: & ' \" <Tag> { \u03BB", c.toString());
    }

    @Test
    public void testDefang()
    {
        Chunk c = new Chunk();
        c.set("malicious_injection", "<script>alert('hi!')</script>");
        c.append("{~malicious_injection|defang}");
        assertTrue(c.toString().indexOf("<script>") < 0);
    }

    @Test
    public void testJoin()
    {
        Chunk c = new Chunk();
        String[] beatles = new String[]{"John","Paul","George","Ringo"};
        c.set("beatles", beatles);
        c.append("The beatles are: {~beatles|join(, )}.");
        assertEquals(c.toString(),"The beatles are: John, Paul, George, Ringo.");
    }

    @Test
    public void testJoinOnSlash()
    {
        Chunk c = new Chunk();
        String[] beatles = new String[]{"John","Paul","George","Ringo"};
        c.set("beatles", beatles);
        c.append("The beatles are: {~beatles|join(/)}.");
        assertEquals(c.toString(),"The beatles are: John/Paul/George/Ringo.");
    }

    @Test
    public void testJoinInlineTable()
    {
        Chunk c = new Chunk();
        String beatles = "[[name],[John],[Paul],[George],[Ringo]]";
        c.set("beatles", beatles);
        c.append("The beatles are: {$beatles|join(, )}.");
        assertEquals(c.toString(),"The beatles are: John, Paul, George, Ringo.");
    }

    @Test
    public void testGet()
    {
        Chunk c = new Chunk();
        String[] beatles = new String[]{"John","Paul","George","Ringo"};
        c.set("beatles", beatles);
        c.append("The 2nd beatle is: {$beatles|get(1)}.");
        assertEquals(c.toString(),"The 2nd beatle is: Paul.");

        c.resetTemplate();
        c.append("The 2nd to last beatle is: {$beatles|get(-2)}.");
        assertEquals(c.toString(),"The 2nd to last beatle is: George.");
    }

    @Test
    public void testGetTooFar()
    {
        Chunk c = new Chunk();
        String[] beatles = new String[]{"John","Paul","George","Ringo"};
        c.set("beatles", beatles);
        c.append("{$beatles|get(4):too far} {$beatles|get(-5):too far}");
        assertEquals("too far too far", c.toString());
    }

    @Test
    public void testSplit()
    {
        Chunk c = new Chunk();
        String x = "a b\n c d";
        String y = "1-2--3-4";
        c.set("x",x);
        c.set("y",y);
        c.append("{$x|split|join} {$y|split(-)|join(,)} {$y|split(/-+/)|join(,)}\n");
        c.append("{$x|split(,2)|join} {$y|split(-,2)|join(,)} {$y|split(/-+/,3)|join(,)}");
        assertEquals("abcd 1,2,,3,4 1,2,3,4\nab 1,2 1,2,3", c.toString());
    }

    @Test
    public void testPadRight()
    {
        Chunk c = new Chunk();
        c.set("blank", "");
        c.set("not_blank", "glowing");
        c.append("+{$blank|rpad}+ +{$not_blank|rpad}{$not_blank}+ +{$undefined|rpad|ondefined(oops):pass}+");
        c.append(" +{$undefined|rpad}+");
        assertEquals("++ +glowing glowing+ +pass+ +{$undefined|rpad}+", c.toString());
    }

    @Test
    public void testPadLeft()
    {
        Chunk c = new Chunk();
        c.set("blank", "");
        c.set("not_blank", "glowing");
        c.append("+{$blank|lpad}+ +{$not_blank}{$not_blank|lpad}+ +{$undefined|lpad|ondefined(oops):pass}+");
        assertEquals("++ +glowing glowing+ +pass+", c.toString());
    }

    @Test
    public void testPadRightArgs()
    {
        Chunk c = new Chunk();
        c.set("blank", "");
        c.set("not_blank", "glowing");
        c.append("+{$blank|rpad(HOT,3)}+ +{$not_blank|rpad(HOT)}+ +{$not_blank|rpad(HOT,3)}+");
        assertEquals("++ +glowingHOT+ +glowingHOTHOTHOT+", c.toString());
    }

    @Test
    public void testPadLeftArgs()
    {
        Chunk c = new Chunk();
        c.set("blank", "");
        c.set("not_blank", "glowing");
        c.append("+{$blank|lpad(HOT,3)}+ +{$not_blank|lpad(HOT)}+ +{$not_blank|lpad(HOT,3)}+");
        assertEquals("++ +HOTglowing+ +HOTHOTHOTglowing+", c.toString());
    }

    @Test
    public void testUC()
    {
        Chunk c = new Chunk();
        c.set("xyz", "Mixed Case");
        c.append("This is no longer {$xyz|uc}.");
        assertEquals(c.toString(),"This is no longer MIXED CASE.");
    }

    @Test
    public void testUpperWithTurkishLocale()
    {
        Chunk c = new Chunk();
        c.set("abc","i");
        c.setLocale("tr_TR");
        c.append("{$abc|uc}");
        assertEquals(c.toString(),"\u0130");
    }

    @Test
    public void testLC()
    {
        Chunk c = new Chunk();
        c.set("xyz", "Mixed Case");
        c.append("This is no longer {~xyz|lc}.");
        assertEquals(c.toString(),"This is no longer mixed case.");
    }

    @Test
    public void testRegexReplaceUC()
    {
        Chunk c = new Chunk();
        c.set("xyz", "Mixed Case");
        c.append("This is no longer {~xyz|s/Mi(.*)$/mi\\U$1/}.");
        assertEquals(c.toString(),"This is no longer miXED CASE.");
    }

    @Test
    public void testRegexReplaceOne()
    {
        Chunk c = new Chunk();
        c.set("xyz", "Dog Dog Dog");
        c.append("Where is my {~xyz|s/Dog/Cat/}?");
        assertEquals(c.toString(),"Where is my Cat Dog Dog?");
    }

    @Test
    public void testRegexReplaceAll()
    {
        Chunk c = new Chunk();
        c.set("xyz", "Dog Dog Dog");
        c.append("Where is my {~xyz|s/Dog/Cat/g}?");
        assertEquals(c.toString(),"Where is my Cat Cat Cat?");
    }

    @Test
    public void testRegexBackrefs()
    {
        // backrefs must be perl-style (eg $1) for now;
        // standard backslash backrefs (eg \1) are not yet supported.
        Chunk c = new Chunk();
        c.set("xyz", "Apples Bananas Chaucer");
        c.append("Where is my {~xyz|s/([A-Z])([a-z]*)/$0 $1-$2/g}?");

        assertEquals(c.toString(),"Where is my Apples A-pples Bananas B-ananas Chaucer C-haucer?");
    }

    @Test
    public void testRegexBraces()
    {
        // nested braces inside a regex are actually ok, they won't break the tag parser.
        Chunk c = new Chunk();
        c.set("xyz", "Moon Moron Mon Maroon Toboggan");
        c.append("regex braces test: {~xyz|s/[abgor]{3,4}/---/g}");
        assertEquals(c.toString(),"regex braces test: Moon M---n Mon M---n T---gan");
    }

    @Test
    public void testTranslationFilter()
    {
        ChunkLocale.registerLocale("de_DE", new String[]{"blue","blau"});

        Chunk c = new Chunk();
        c.setLocale("de_DE");
        c.set("color","blue");
        c.append(c.makeTag("color|xlate"));

        assertEquals("blau",c.toString());
    }

    @Test
    public void testAlternateFilter()
    {
        Chunk c = new Chunk();
        c.set("stooges",new String[]{"Larry","Curly","Moe"});
        c.append("{^loop in ~stooges as name counter_tags=\"true\" divider=\"-\"}{~name}:{~0|alternate(EVEN,ODD)}{/loop}");

        assertEquals("Larry:EVEN-Curly:ODD-Moe:EVEN", c.toString());
    }

    @Test
    public void testSortFilter()
    {
        Chunk c = new Chunk();
        c.set("stooges",new String[]{"Larry","Curly","Moe"});
        c.append("{$stooges|sort|join}");

        assertEquals("CurlyLarryMoe", c.toString());

        c.set("stooges",new String[]{"Larry","Curly","Moe",null});
        assertEquals("LarryCurlyMoe", c.toString());
    }

    @Test
    public void testOnEmptyFilter()
    {
        Chunk c = new Chunk();

        String x = null;
        c.set("empty_tag",x);
        c.set("std_tag","boo hoo!");

        c.append("{~tag|onempty(boo!)} {~empty_tag|onempty(foo!)} {~std_tag|onempty(hufu!)}");

        assertEquals("boo! foo! boo hoo!", c.toString());
    }

    @Test
    public void testDefaultFilter()
    {
        Chunk c = new Chunk();

        String x = null;
        c.set("empty_tag",x);
        c.set("std_tag","boo hoo!");

        c.append("{% $tag|default(boo!) %} {% $empty_tag|default(foo!) %} {% $std_tag|default(hufu!) %}");

        assertEquals("boo!  boo hoo!", c.toString());
    }

    @Test
    public void testPoorlyBehavedUserFilter()
    {
        Theme theme = new Theme();
        theme.registerFilter(new LeftTrimFilter());
        Chunk c = theme.makeChunk();
        // name is not defined, so filter throws NullPointerException,
        // doesn't handle null properly
        c.append("xxx.{~name|ltrim:}.xxx");

        // this test intentionally triggers a problem.
        // this hides the unsightly stderr output which will result.
        System.setErr(new PrintStream(new ByteArrayOutputStream()));

        assertEquals("xxx..xxx",c.toString());
    }

    @Test
    public void testTypeFilter()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x|type} {$y|type} {$z|type} {$c|type} {$not_there|type}");
        c.set("x","hello");
        c.set("y",new String[]{"a","b","c"});

        HashMap<String,Object> z = new HashMap<String,Object>();
        z.put("abc", "123");
        c.set("z",z);

        c.set("c",c);

        assertEquals("STRING LIST OBJECT CHUNK NULL",c.toString());
    }

    @Test
    public void testStringFilter()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();

        ArrayList<String> z = new ArrayList<String>();
        z.add("rice");
        z.add("spam");
        z.add("seaweed");
        c.set("z", z);

        c.append("{$z|get(0)} {$z|str} {$z|str|get(0)}");

        assertEquals("rice [rice, spam, seaweed] [rice, spam, seaweed]", c.toString());
    }

    @Test
    public void testSliceFilter()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x|slice(::-1)|get(0)}");
        c.set("x",new String[]{"A","B","C"});
        assertEquals("C", c.toString());

        c.resetTemplate();
        c.append("{$x|slice(1:2)|get(0)}");
        assertEquals("B", c.toString());

        c.resetTemplate();
        c.append("{$x|slice(-1:)|get(0)}");
        assertEquals("C", c.toString());

        c.resetTemplate();
        c.append("{$x|slice(-2:-1)|get(0)}");
        assertEquals("B", c.toString());

        c.resetTemplate();
        c.append("{$x|slice(2::-2)|get(1)}");
        assertEquals("A", c.toString());
    }

    @Test
    public void testReverseFilter()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x|reverse|join}");
        c.set("x",new String[]{"A","B","C"});
        assertEquals("CBA", c.toString());
    }

    @Test
    public void testLengthFilter()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x|length} {$y|length}");
        c.set("x",new String[]{"A","B","C"});
        c.set("y","abcd");
        assertEquals("3 4", c.toString());
    }

    @Test
    public void testCapitalizeFilter()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x|capitalize} {$y|capitalize} {$z|capitalize}");
        c.set("x","johnny o'Brien james mcDonald");
        c.set("y","MAD PROPS");
        c.set("z","my favorite\nthings.");
        assertEquals("Johnny O'Brien James McDonald MAD PROPS My Favorite\nThings.", c.toString());

        c.resetTemplate();
        c.append("{$x|title} {$y|title} {$z|title}");
        assertEquals("Johnny O'Brien James Mcdonald Mad Props My Favorite\nThings.", c.toString());
    }

    @Test
    public void testBadArgs()
    {
        Theme theme = new Theme();
        Chunk c = theme.makeChunk();
        c.append("{$x|slice(a,b,c)|join}");
        c.set("x",new String[]{"A","B","C"});
        assertEquals("ABC", c.toString());

        c.resetTemplate();
        c.set("x","a b c d e");
        c.append("{$x|split(,giraffe)|join}");
        assertEquals("abcde", c.toString());
    }

    @Test
    public void testUserFilter()
    {
        Theme theme = new Theme();
        theme.registerFilter(new LeftTrimFilter());
        Chunk c = theme.makeChunk();
        c.append("xxx{$name|ltrim}xxx{$name|trim}xxx");
        c.set("name","  \nBob  ");
        assertEquals("xxxBob  xxxBobxxx",c.toString());
    }

    @Test
    public void testUserFilterLoadFromFile()
    {
        Theme theme = new Theme("test/base");
        theme.registerFilter(new LeftTrimFilter());
        Chunk c = theme.makeChunk("chunk_test#ltrim_test");
        assertEquals("xxxBob  xxxBobxxx\n",c.toString());
    }

    @Test
    public void testUserBigDecimalFilter()
    {
        Theme theme = new Theme("test/base");
        theme.registerFilter(new BigDecimalFilter());
        Chunk c = theme.makeChunk();
        c.set("x", new BigDecimal("3e40"));
        c.append("{$x|bignum}");

        assertEquals("30000000000000000000000000000000000000000", c.toString());
    }

    @Test
    public void testUserDateTimeFilter()
    {
        Theme theme = new Theme("test/base");
        theme.registerFilter(new DateTimeFilter());
        Chunk c = theme.makeChunk();
        Date d = new Date(1435350295866L);
        c.set("x", d);
        c.append("{$x|date(yyyy-MM-dd)} {$x|date} {$x|date(,America/Los_Angeles)} {$x|date(,EST)} {$x|date(,UTC)}");

        assertEquals("2015-06-26 2015-06-26T20:24:55+0000 2015-06-26T13:24:55-0700 2015-06-26T15:24:55-0500 2015-06-26T20:24:55+0000",
            c.toString());
    }

    public class LeftTrimFilter extends BasicFilter
    {

        public String transformText(Chunk chunk, String text, FilterArgs args)
        {
            ///if (text == null) return null;

            int i=0;
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;

            return (i == 0) ? text : text.substring(i);
        }

        public String getFilterName()
        {
            return "ltrim";
        }

    }

    public class BigDecimalFilter extends ObjectFilter
    {
        public String getFilterName()
        {
            return "bignum";
        }

        public Object transformObject(Chunk chunk, Object object, FilterArgs args)
        {
            if (object instanceof BigDecimal) {
                BigDecimal big = (BigDecimal)object;
                return big.toPlainString();
            } else {
                return "ERR: NOT A BIG DECIMAL";
            }
        }
    }

    public class DateTimeFilter extends ObjectFilter
    {
        private static final String DEFAULT_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

        public Object transformObject(Chunk chunk, Object obj, FilterArgs arg)
        {
            if (!(obj instanceof Date)) {
                return "ERR: Not a java.util.Date";
            }

            Date date = (Date)obj;

            String format = null;
            String timezone = "UTC";

            String[] args = arg.getFilterArgs();
            if (args != null) {
                if (args.length == 1) {
                    format = args[0];
                } else if (args.length > 1) {
                    format = args[0];
                    timezone = args[1];
                }
            }

            if (format == null || format.trim().length() == 0) format = DEFAULT_FORMAT;

            try {
                ChunkLocale chunkLocale = chunk.getLocale();
                Locale javaLocale = chunkLocale == null ? null : chunkLocale.getJavaLocale();
                SimpleDateFormat formatter = javaLocale == null
                    ? new SimpleDateFormat(format)
                    : new SimpleDateFormat(format, javaLocale);
                formatter.setTimeZone(TimeZone.getTimeZone(timezone));
                return formatter.format(date);
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
        }

        public String getFilterName()
        {
            return "date";
        }
    }

    //TODO add tests for |hex and |HEX
}
