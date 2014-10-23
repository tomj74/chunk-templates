package com.x5.template;

import java.io.Writer;

public class SnippetComment extends SnippetPart
{
    public SnippetComment(String text)
    {
        super(text);
    }

    /* comments do not render */
    public void render(Writer out, Chunk context, int depth)
    throws java.io.IOException
    {
    }
}
