package com.x5.template;

import java.io.Writer;

public class SnippetPart
{
	protected String snippetText;
	private boolean isLiteral = false;

	public SnippetPart(String text)
	{
		this.snippetText = text;
	}
	
	public String getText()
	{
		return snippetText;
	}
	
    public void setText(String text)
    {
        this.snippetText = text;
    }

    public void setLiteral(boolean isLiteral)
	{
		this.isLiteral = isLiteral;
	}
	
	public boolean isLiteral()
	{
		return this.isLiteral;
	}
	
	public boolean isTag()
	{
	    return false;
	}
	
	public boolean depthCheckFails(int depth, Writer out)
	throws java.io.IOException
	{
	    if (depth >= Chunk.DEPTH_LIMIT) {
            out.append("[**ERR** max template recursions: "+Chunk.DEPTH_LIMIT+"]");
	        return true;
	    } else {
	        return false;
	    }
	}
	
	public void render(Writer out, Chunk rules, int depth)
	throws java.io.IOException
	{
	    if (isLiteral) {
	        out.append(snippetText);
	    } else {
	        // ... ? shouldn't ever get here, pure SnippetPart's are now
	        // static/literal content only.  subclasses of SnippetPart
	        // do their dynamic rendering here by overriding the render method.
	    }
	}
	
	/**
	 * toString() returns the un-interpreted content for this part
	 */
	public String toString()
	{
	    return snippetText;
	}
}
