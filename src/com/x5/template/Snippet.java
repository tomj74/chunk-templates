package com.x5.template;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Snippet
{
	private List<SnippetPart> parts = null;
	private String simpleText = null;
	
	public Snippet(String template)
	{
	    parseParts(template);
	    if (parts == null) {
	        // no literals
	        parseParts2(template);
	    } else {
	        // reconcile parts (THIS IS DUMB AND UNREADABLE CODE FIXME REFACTOR)
	        for (int i=0; i<parts.size(); i++) {
	            SnippetPart part = parts.get(i);
	            if (!part.isLiteral()) {
	                Snippet s2 = new Snippet(part.getText());
	                if (s2.parts == null) {
	                    part.setLiteral(true);
	                } else {
	                    parts.remove(i);
	                    parts.addAll(i,s2.parts);
	                    i += s2.parts.size() - 1;
	                }
	            }
	        }
	    }
	}
	
	public Snippet(List<SnippetPart> bodyParts)
    {
	    if (bodyParts == null || bodyParts.size() == 0) {
	        simpleText = "";
	    } else {
	        this.parts = bodyParts;
	    }
    }
	
	public Snippet(List<SnippetPart> bodyParts, int from, int to)
	{
	    if (bodyParts == null || bodyParts.size() == 0) {
	        simpleText = "";
	    } else {
	        ArrayList<SnippetPart> subParts = new ArrayList<SnippetPart>();
	        for (int i=from; i<to; i++) {
	            subParts.add(bodyParts.get(i));
	        }
	        this.parts = subParts;
	    }
	}

    public boolean isSimple()
	{
		return (simpleText != null);
	}
	
	public String getSimpleText()
	{
		return simpleText;
	}
	
	public List<SnippetPart> getParts()
	{
		return parts;
	}
	
	private static final String MAGIC_CHARS = "~^/*=+_";
	
	/**
	 * Second pass over the string.  Identify all dynamic tags and slice into
	 * parts - literals will pass directly into output, dynamic SnippetTag parts
	 * will get interpreted.
	 * 
	 * Third step is to nest block tags properly.
	 * 
	 * @param template
	 */
	private void parseParts2(String template)
	{
	    // pre-compile template -- slice and dice ALL tags as separate parts.
	    if (template == null) return;

	    // first pass was just to catch literals. reset.
        simpleText = null;

	    // oh yeah -- we're going old-school
	    char[] chars = template.toCharArray();
	    char c, c2, regexPrefix;
	    // how many regex delims left before tag parser has to wake up again
	    int regexDelimCount = 0;
	    
	    int marker = 0; // beginning of latest static span
	    int tagStart = -1; // beginning of latest tag span
	    int trailingBackslashes = 0; // track escape chars so we can ignore escapes
	    
	    // the parser has to ignore certain chars when in various states
	    boolean insideRegex = false;
	    boolean insideTrToken = false;
        boolean insideComment = false;
        
        // magicChar determines what sort of tag are we forming
	    char magicChar = 0;
	    
	    for (int i=0; i<chars.length; i++) {
	        c = chars[i];
	        
	        if (tagStart < 0) {
	            
	            // edge case - can't start a tag on final char of sequence
	            if (i+1 >= chars.length) break;
	            
	            // collecting static until tag comes along
	            if (c == '{') {
    	            c2 = chars[i+1];
    	            if (MAGIC_CHARS.indexOf(c2) > -1) {
    	                // FOUND TAG START
    	                tagStart = i;
    	                trailingBackslashes = 0;
    	                i++;
    	                magicChar = c2;
    	            }
	            } else if (c == '_' && chars[i+1] == '[') {
    	            // localization token!
    	            tagStart = i;
    	            insideTrToken = true;
    	            i++;
	            }
	            
	        } else {
	            
	            // tagStart is positive value -- scan for tag-end
	            
	            if (insideTrToken && c == ']') {
	                if (trailingBackslashes % 2 == 0) {
    	                // FOUND TOKEN END
    	                if (parts == null) parts = new ArrayList<SnippetPart>();
    	                if (marker < tagStart) {
    	                    SnippetPart literal = new SnippetPart(template.substring(marker,tagStart));
    	                    literal.setLiteral(true);
    	                    parts.add(literal);
    	                }
                        String tokenStr = template.substring(tagStart+2,i);
                        SnippetToken token = new SnippetToken(tokenStr);
                        parts.add(token);
                        // reset scan mode
                        marker = i+1;
                        tagStart = -1;
	                }
	            } else if (c == '}') {
	                if (!insideRegex && trailingBackslashes % 2 == 0) {
	                    if (magicChar == '!') {
                            char c0 = chars[i-1];
                            char c00 = chars[i-2];
                            if (c0 == '-' && c00 == '-') {
                                // FOUND COMMENT END
                                // discard comment and reset scan mode
                                tagStart = -1;
                                marker = i+1;
                                insideComment = false;
                            } else {
                                // this curly brace is not the end of the comment
                                // keep scanning
                                insideComment = true;
                            }
	                    } else {
	                        //////////////////////////////////////////////////////////
                            // FOUND TAG END, extract and add to sequence along with
	                        // preceding static content, if any.
                            //////////////////////////////////////////////////////////
    	                    extractTag(magicChar,template,chars,marker,tagStart,i);
    	                    
                            // reset scan mode
                            tagStart = -1;
                            marker = i+1;
	                    }
	                }
	            } else if (c == '/' && trailingBackslashes % 2 == 0) {
	                // MIGHT BE INSIDE REGEX...
	                // ignore curly braces until we get past this regex span
	                if (regexDelimCount > 0) {
	                    regexDelimCount--;
	                    if (regexDelimCount < 1) {
	                        // found END of this regex
	                        insideRegex = false;
	                    }
	                } else {
    	                char c0 = chars[i-1];
    	                char c00 = chars[i-2];
    	                if (c0 == 's' && c00 == '|') {
    	                    // found {~tag|s/.../.../}
    	                    // need to find two more
    	                    regexDelimCount = 2;
    	                    insideRegex = true;
    	                } else if (c0 == 'm' && (c00 == ',' || c00 == '(')) {
    	                    // found {~tag|...(m/.../,...,m/.../,...)}
    	                    regexDelimCount = 1;
    	                    insideRegex = true;
    	                } else if (c0 == ',' || c00 == '(') {
                            // found {~tag|...(/regex/,...,/regex/,...)}
    	                    regexDelimCount = 1;
    	                    insideRegex = true;
    	                }
	                }
	            } else if (c == '\\') {
                    trailingBackslashes++;
                } else if (trailingBackslashes > 0) {
                    trailingBackslashes = 0;
                }
	        }
	    }
	    
	    if (parts == null) {
	        // no parts? avoid overhead of ArrayList
	        simpleText = template;
	    } else {
	        if (!insideComment && marker < template.length()) {
	            SnippetPart finalLiteral = new SnippetPart(template.substring(marker));
	            finalLiteral.setLiteral(true);
	            parts.add(finalLiteral);
	        }
	        groupBlocks(parts);
	    }
        
	}
	
    private void extractTag(char magicChar, String template, char[] chars,
            int marker, int tagStart, int i)
    {
        // FOUND TAG END
        // extract and add to part sequence
        if (parts == null) parts = new ArrayList<SnippetPart>();
        
        // any static content leading up to tag?  capture static part.
        if (marker < tagStart) {
            SnippetPart literal = new SnippetPart(template.substring(marker,tagStart));
            literal.setLiteral(true);
            parts.add(literal);
        }
        
        // and now focus on the tag...
        String wholeTag = template.substring(tagStart,i+1);
        if (magicChar == '~') {
            String gooeyCenter = template.substring(tagStart+2,i);
            SnippetTag tag = new SnippetTag(wholeTag,gooeyCenter);
            parts.add(tag);
        } else if (magicChar == '^') {
            String gooeyCenter = template.substring(tagStart+2,i);
            // expand ^ to ~.
            SnippetTag tag = new SnippetTag(wholeTag,"."+gooeyCenter);
            parts.add(tag);
        } else if (magicChar == '/') {
            String gooeyCenter = template.substring(tagStart+1,i);
            // expand {/ to {^/ to {~./
            SnippetTag tag = new SnippetTag(wholeTag,"."+gooeyCenter);
            parts.add(tag);
        } else if (magicChar == '*') {
            // convert macro syntax to internal macro block-tag
            if (wholeTag.length() == 3) {
                // this marks the end of the macro block {*}
                SnippetTag tag = new SnippetTag(wholeTag,"."+MacroTag.MACRO_END_MARKER);
                parts.add(tag);
            } else {
                int refEnd = i;
                if (chars[i-1] == '*') refEnd--;
                String macroTemplate = template.substring(tagStart+2,refEnd).trim();
                SnippetTag macroHead = new SnippetTag(wholeTag,"."+MacroTag.MACRO_MARKER+" "+macroTemplate);
                parts.add(macroHead);
            }
        } else if (magicChar == '=') {
            if (wholeTag.length() == 3) {
                // this marks the end of a macro def {=}
                SnippetTag tag = new SnippetTag(wholeTag,"=");
                parts.add(tag);
            }
        } else if (magicChar == '_') {
            // for example: {_[token %s %s],~with,~args}
            SnippetToken token = SnippetToken.parseTokenWithArgs(wholeTag);
            parts.add(token);
        } else if (magicChar == '+') {
            // include shorthand: {+template#ref} or {+(cond)template#ref}
            if (wholeTag.startsWith("{+(")) {
                String includeIfTag = ".includeIf(" + template.substring(tagStart+3,i);
                SnippetTag condInclude = new SnippetTag(wholeTag,includeIfTag);
                parts.add(condInclude);
            } else {
                String includeTag = ".include " + template.substring(tagStart+2,i);
                SnippetTag include = new SnippetTag(wholeTag,includeTag);
                parts.add(include);
            }
        } else {
            // huh?
            SnippetPart wackyTag = new SnippetPart(wholeTag);
            parts.add(wackyTag);
        }
    }

    private int eatRestOfLineAfterBlockEndTag(char[] chars, int startAt)
	{
	    int cutPoint = startAt;
	    for (int i=startAt; i<chars.length; i++) {
	        char c = chars[i];
	        if (c == ' ' || c == '\t') {
	            // keep eating
	            cutPoint = i+1;
	        } else if (c == '\n') {
	            // done eating (unix)
	            cutPoint = i+1;
	            break;
	        } else if (c == '\r') {
	            // done eating (MAC/Win)
	            cutPoint = i+2;
	            if (cutPoint > chars.length) cutPoint--;
	            break;
	        } else {
	            // content on this line!  abandon meal!
	            cutPoint = startAt;
	            break;
	        }
	    }
	    return cutPoint;
	}

	/**
	 * First pass over string, pull out {^literal}...{/literal} blocks.
	 * 
	 * It might be worth combining this step with the second step which
	 * basically pre-compiles the whole template into a linked list of
	 * SnippetPart objects.
	 * 
	 * The current strategy still walks the string twice, first here
	 * and then again in parseParts2().
	 * 
	 * @param template
	 */
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
			
			int endMarkerLen = TemplateSet.LITERAL_END.length();
			
			int litEnd = template.indexOf(TemplateSet.LITERAL_END,litPos+TemplateSet.LITERAL_SHORTHAND.length());
			// {^} ends a literal block, OR {/literal} -- whichever comes first.
            int litEndLong = template.indexOf(TemplateSet.LITERAL_END_LONGHAND,litPos+TemplateSet.LITERAL_SHORTHAND.length());
            if (litEndLong > -1 && (litEnd < 0 || litEndLong < litEnd)) {
                litEnd = litEndLong;
                endMarkerLen = TemplateSet.LITERAL_END_LONGHAND.length();
            }
            
			if (litEnd < 0) {
				String text = template.substring(litPos);
				SnippetPart literal = new SnippetPart(text);
				literal.setLiteral(true);
				parts.add(literal);
				// eat the whole rest of the enchilada.  burp.
				marker = template.length();
				break;
			} else {
				marker = litEnd + endMarkerLen;
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
	
	private void groupBlocks(List<SnippetPart> bodyParts)
	{
	    for (int i=0; i<bodyParts.size(); i++) {
	        SnippetPart part = bodyParts.get(i);
	        if (part.isTag()) {
	            SnippetTag tag = (SnippetTag)part;
	            BlockTag helper = tag.getBlockTagType();
	            if (helper != null) {
	                int j = BlockTag.findMatchingBlockEnd(helper, bodyParts, i+1);
	                if (j > i) {
	                    // remove these parts and place them all together
	                    // in a new SnippetBlockTag
	                    SnippetTag endTag = (SnippetTag)bodyParts.remove(j);
	                    
	                    ArrayList<SnippetPart> subBodyParts = new ArrayList<SnippetPart>();
	                    for (int x=i+1; x<j; x++) subBodyParts.add(bodyParts.get(x));
	                    for (int x=j-1; x>=i; x--) bodyParts.remove(x);
	                    
	                    // recurse
	                    groupBlocks(subBodyParts);
	                    
                        SnippetBlockTag blockTag = new SnippetBlockTag(tag,subBodyParts,endTag);
	                    bodyParts.add(i,blockTag);
	                    
	                    smartTrimAfterBlockEnd(bodyParts,i+1);
	                } else {
	                    // unmatched block tag!!  output error notice.
	                    // TODO should create a SnippetErrorPart class
	                    // and offer options wrt, exclude errors
	                    // in final output, etc.
	                    String errMsg = "[ERROR in template! "+helper.getBlockStartMarker()+" block with no matching end marker! ]";
	                    SnippetPart errorPart = new SnippetPart(errMsg);
	                    errorPart.setLiteral(true);
	                    bodyParts.add(i+1,errorPart);
	                    i++;
	                }
	            }
	        }
	    }
	}
	
    private static final Pattern UNIVERSAL_LF = Pattern.compile("\n|\r\n|\r\r");

    private void smartTrimAfterBlockEnd(List<SnippetPart> parts, int nextPartIdx)
	{
	    if (parts.size() <= nextPartIdx) return;
	    SnippetPart nextPart = parts.get(nextPartIdx);
	    if (nextPart.isLiteral()) {
	        String text = nextPart.getText();
	        // if the block begins with (whitespace+) LF, trim initial line
	        Matcher m = UNIVERSAL_LF.matcher(text);
	        
	        if (m.find()) {
	            int firstLF = m.start();
	            if (text.substring(0,firstLF).trim().length() == 0) {
	                nextPart.setText( text.substring(m.end()) );
	            }
	        }
	    }
	}
	
	public String toString()
	{
		if (simpleText != null) return simpleText;
		if (parts == null) return null;
		
		// reassemble parts back into original template
		StringBuilder sb = new StringBuilder();
		for (SnippetPart part : parts) {
			sb.append(part.toString());
		}
		return sb.toString();
	}
	
	public void render(Writer out, Chunk rules, int depth)
	throws java.io.IOException
	{
	    if (simpleText != null) {
	        out.append(simpleText);
	    } else if (parts != null) {
    	    for (SnippetPart part : parts) {
    	        part.render(out, rules, depth+1);
    	    }
	    }
	}
}
