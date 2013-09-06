package com.x5.template;

import java.io.Writer;

public class SnippetError extends SnippetPart
{
    public SnippetError(String errMsg)
    {
        super(errMsg);
        super.setLiteral(true);
    }
 
    public void render(Writer out, Chunk rules, int depth)
    throws java.io.IOException
    {
        if (rules == null || rules.renderErrorsToOutput()) {
            out.append(snippetText);
        }
    }
 
}
