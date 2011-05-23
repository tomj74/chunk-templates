package com.x5.template;

import org.junit.Test;
import static org.junit.Assert.*;

public class TextFilterTest
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
        c.append("{~hello|indent(3)}");
        c.set("hello","String\r\rWith\r\rMany\r\rLines\r\r");
        assertEquals("   String\r\r   With\r\r   Many\r\r   Lines\r\r", c.toString());
    }
    
    @Test
    public void testSimpleOndefined()
    {
        Chunk c = new Chunk();
        c.append("Output: {~hello|ondefined(greetings!)}");
        c.set("hello", "hello");
        assertEquals("Output: greetings!",c.toString());
    }
    
    @Test
    public void testPassThruOndefined()
    {
        Chunk c = new Chunk();
        c.append("Output: {~hello|ondefined(greetings!)}");
        assertEquals("Output: {~hello|ondefined(greetings!)}",c.toString());
    }

    @Test
    public void testVaporizingOndefined()
    {
        Chunk c = new Chunk();
        c.append("Output: {~hello|ondefined(greetings!):}");
        assertEquals("Output: ",c.toString());
    }
    
    @Test
    public void testOndefinedParentFallback()
    {
        Chunk c = new Chunk();
        Chunk p = new Chunk();
        
        p.append("{~child}");
        p.set("child", c);
        p.set("hello", "hello"); // defined in parent, still counts as defined in child
        
        c.append("Output: {~hello|ondefined(greetings!)}");
        assertEquals("Output: greetings!",p.toString());
    }
    
    @Test
    public void testSimpleOnMatch()
    {
        Chunk c = new Chunk();
        // ok, so this onmatch is not all that simple...
        c.append("Output: {~hello|onmatch(/E.*O/i,greetings!)nomatch(darn!)}");
        c.set("hello", "hello");
        assertEquals("Output: greetings!",c.toString());
    }
    
    @Test
    public void testOnMatchNoMatch()
    {
        Chunk c = new Chunk();
        c.append("Output: {~hello|onmatch(/E.*O/i,greetings!)nomatch(yay!)}");
        c.set("hello", "hella");
        assertEquals("Output: yay!",c.toString());
    }
    
    @Test
    public void testOnMatchParentFallback()
    {
        // this also tests the case-insensitive matching regex flag //i
        Chunk c = new Chunk();
        Chunk p = new Chunk();
        
        p.append("P: {~child}");
        p.set("child", c);
        p.set("hello", "hello");
        
        c.append("Output: {~hello|onmatch(/E.*O/i,greetings!)nomatch(darn!)}");
        assertEquals("P: Output: greetings!",p.toString());
    }
    
    @Test
    public void testOnMatchFirstMatch()
    {
        Chunk c = new Chunk();
        
        c.set("hello", "catch");
        
        c.append("Output: {~hello|onmatch(/dog/,MatchOne,/cat/,MatchTwo,/catch/,MatchThree)nomatch(darn!)}");
        
        assertEquals("Output: MatchTwo",c.toString());
    }
    
    @Test
    public void testMD5()
    {
        Chunk c = new Chunk();
        
        c.append("{~xyz:XYZ|md5}");
        
        assertEquals("e65075d550f9b5bf9992fa1d71a131be",c.toString());
    }
    
    @Test
    public void testSHA1()
    {
        Chunk c = new Chunk();
        c.append("{~xyz:XYZ|sha1}");
        assertEquals("717c4ecc723910edc13dd2491b0fae91442619da",c.toString());
    }
    
    @Test
    public void testBase64()
    {
        Chunk c = new Chunk();
        c.set("xyz","Some very long string with Crazy Characters\u00EE!");
        c.append("{~xyz|base64}");
        assertEquals("U29tZSB2ZXJ5IGxvbmcgc3RyaW5nIHdpdGggQ3JhenkgQ2hhcmFjdGVyc8OuIQ==",c.toString());
    }

    @Test
    public void testBase64RoundTrip()
    {
        Chunk c = new Chunk();
        String src = "Some very long string with Crazy Characters\u00EE!";
        c.set("xyz",src);
        c.append("{~xyz|base64|base64decode}");
        assertEquals(src,c.toString());
    }
    
    @Test
    public void testSimpleRegexReplace()
    {
        Chunk c = new Chunk();
        c.set("xyz", "Lemon Lion Liar Loon Lenore Forlorn");
        c.append("{~xyz|s/L[^ ]*?n/Chunky/g}");
        assertEquals("Chunky Chunky Liar Chunky Chunkyore Forlorn",c.toString());
    }
    
    @Test
    public void testSimpleFormat()
    {
        Chunk c = new Chunk();
        c.set("howmuch","300020.4151233");
        c.append("{~howmuch|sprintf($%,.2f)}");
        assertEquals("$300,020.41",c.toString());
    }
    
    @Test
    public void testSimpleCalc()
    {
        Chunk c = new Chunk();
        c.set("howmuch","30");
        c.append("{~howmuch|calc(*10+4)|sprintf(%.0f)}");
        assertEquals("304",c.toString());
        
        c.append(" {~howmuch|calc(\"*10+4\",\"%.0f\")}");
        assertEquals("304 304",c.toString());
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
        c.set("abc","& ' \" <Tag>");
        c.append("test: {~abc|xmlescape}");
        assertEquals("test: &amp; &apos; &quot; &lt;Tag&gt;",c.toString());
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
    public void testUC()
    {
        Chunk c = new Chunk();
        c.set("xyz", "Mixed Case");
        c.append("This is no longer {~xyz|uc}.");
        assertEquals(c.toString(),"This is no longer MIXED CASE.");
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
    
    //TODO add tests for |hex and |HEX and |sel(string)
    // and |sel(~tag) and |checked(string) and |checked(~tag)
}
