package com.x5.template;

public class SnippetPart
{
	private String snippetText;
	private boolean isLiteral = false;

	public SnippetPart(String text)
	{
		this.snippetText = text;
	}
	
	public String getText()
	{
		return snippetText;
	}
	
	public void setLiteral(boolean isLiteral)
	{
		this.isLiteral = isLiteral;
	}
	
	public boolean isLiteral()
	{
		return this.isLiteral;
	}
	
}
