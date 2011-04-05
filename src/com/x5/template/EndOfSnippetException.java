package com.x5.template;

@SuppressWarnings("serial")
public class EndOfSnippetException extends Exception
{
	private String line;
	
	public EndOfSnippetException(String line)
	{
		this.line = line;
	}
	
	public String getRestOfLine()
	{
		return line;
	}
}
