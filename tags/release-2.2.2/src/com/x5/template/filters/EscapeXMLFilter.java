package com.x5.template.filters;

import com.x5.template.Chunk;

public class EscapeXMLFilter extends BasicFilter implements ChunkFilter
{
    private static final String findMe = "&<>\"'";
    private static final String[] replaceWith =
            new String[]{"&amp;","&lt;","&gt;","&quot;","&apos;"};

    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return null;

        boolean escapedSomething = false;
        char c;
        StringBuilder escaped = new StringBuilder();
        // strip invalid chars, escape findMe's, escape non-ascii
        for (int i=0; i<text.length(); i++) {
            c = text.charAt(i);
            int whichOne = findMe.indexOf(c);
            if (whichOne > -1)
            {
                escaped.append(replaceWith[whichOne]);
                escapedSomething = true;
            }
            else if ((c == 0x9) || (c == 0xA) || (c == 0xD) ||
                     ((c >= 0x20) && (c < 0x100))
                    )
            {
                escaped.append(c);
            }
            else if (c > 0xFF)
            {
                if ((c <= 0xD7FF) ||
                    ((c >= 0xE000) && (c <= 0xFFFD)) ||
                    ((c >= 0x10000) && (c <= 0x10FFFF))
                   )
                {
                    escaped.append("&#x");
                    escaped.append(Integer.toHexString(c));
                    escaped.append(';');
                }
                escapedSomething = true;
            } else {
                // illegal character, stripping.
                escapedSomething = true;
            }
        }

        if (escapedSomething) {
            return escaped.toString();
        } else {
            return text;
        }
    }

    public String getFilterName()
    {
        return "xml";
    }

    public String[] getFilterAliases()
    {
        return new String[]{
            "html",
            "xmlescape",
            "htmlescape",
            "escapexml",
            "escapehtml",
            "xmlesc",
            "htmlesc"
        };
    }

}
