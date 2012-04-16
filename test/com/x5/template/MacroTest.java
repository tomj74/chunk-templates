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
}
