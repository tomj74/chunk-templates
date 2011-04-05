package com.x5.template;

import java.util.ArrayList;

public class Snippet
{
	private ArrayList<SnippetPart> parts = null;
	private String simpleText = null;
	
	public Snippet(String template)
	{
		parseParts(template);
	}
	
	public boolean isSimple()
	{
		return (simpleText != null);
	}
	
	public String getSimpleText()
	{
		return simpleText;
	}
	
	public ArrayList<SnippetPart> getParts()
	{
		return parts;
	}
	
	private void parseParts(String template)
	{
		// cut literal blocks out of template
		if (template == null) return;
		
		int litPos = TemplateSet.findLiteralMarker(template);

		if (litPos < 0) {
			// no literals? avoid overhead of ArrayList
			simpleText = template;
			return;
		}
		
		parts = new ArrayList<SnippetPart>();
		
		int marker = 0;
		
		while (litPos > -1) {
			String beforeLiteral = template.substring(marker,litPos);
			parts.add(new SnippetPart(beforeLiteral));
			
			int litEnd = template.indexOf(TemplateSet.LITERAL_END,litPos+TemplateSet.LITERAL_SHORTHAND.length());
			if (litEnd < 0) {
				String text = template.substring(litPos);
				SnippetPart literal = new SnippetPart(text);
				literal.setLiteral(true);
				parts.add(literal);
				break;
			} else {
				marker = litEnd+TemplateSet.LITERAL_END.length();
				String text = template.substring(litPos,marker);
				SnippetPart literal = new SnippetPart(text);
				literal.setLiteral(true);
				parts.add(literal);
				litPos = TemplateSet.findLiteralMarker(template,marker);
			}
		}
		
		if (marker < template.length()) {
			String tailText = template.substring(marker);
			parts.add(new SnippetPart(tailText));
		}
	}
	
	public String toString()
	{
		if (simpleText != null) return simpleText;
		if (parts == null) return null;
		
		StringBuilder sb = new StringBuilder();
		for (SnippetPart part : parts) {
			sb.append(part.getText());
		}
		return sb.toString();
	}
}
