package com.x5.template;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Snippet
{
	private List<SnippetPart> parts = null;
	private String simpleText = null;

	private static boolean useCache = isCacheEnabled();
	private static HashMap<String,Snippet> snippetCache = new HashMap<String,Snippet>();
	private static HashMap<String,Long> cacheAge = new HashMap<String,Long>();
    private static long lastGC = 0;
	private static long gcCounter = 0;
	private static final int gcInterval = 500;
	private static final long CAN_GC_AFTER = 1000 * 60;
	
	private Snippet(String template)
	{
	    parseParts(template);
	}
	
	public static Snippet getSnippet(String template)
	{
	    if (Snippet.useCache) {
	        return getSnippetFromCache(template);
	    } else {
	        return new Snippet(template);
	    }
	}
	
    /* premature optimization? */
	private static Snippet getSnippetFromCache(String template)
	{
	    long timestamp = System.currentTimeMillis();
	    
	    if (++gcCounter % gcInterval == 0) {
	        pruneCache(timestamp);
	    }
	    
	    Snippet s = snippetCache.get(template);
	    if (s != null) {
	        cacheAge.put(template, timestamp);
	        return s;
	    } else {
	        s = new Snippet(template);
	        snippetCache.put(template, s);
	        cacheAge.put(template, timestamp);
	        return s;
	    }
	}
	
	private static boolean isCacheEnabled()
	{
	    String useCacheProperty = System.getProperty("chunk.snippetcache");
	    if (useCacheProperty != null) {
	        return true;
	    } else {
	        return false;
	    }
	}
	
	private static void pruneCache(long timestamp)
	{
	    long threshhold = timestamp - CAN_GC_AFTER;
	    if (lastGC > threshhold) return;
	    
	    Iterator<String> i = snippetCache.keySet().iterator();
	    while (i.hasNext()) {
	        String key = i.next();
	        long age = cacheAge.get(key);
	        if (age < threshhold) {
	            i.remove();
	            cacheAge.remove(key);
	        }
	    }
	    lastGC = timestamp;
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

	public List<SnippetPart> getParts()
	{
		return parts;
	}
	
	private static final String MAGIC_CHARS = "~$^./!*=+_";
	
	/**
	 * One pass over the string.  Identify all dynamic tags and slice into
	 * parts - literals will pass directly into output, dynamic SnippetTag parts
	 * will get interpreted.
	 * 
	 * Second step is to nest block tags properly.
	 * 
	 * @param template
	 */
	private void parseParts(String template)
	{
	    // pre-compile template -- slice and dice ALL tags as separate parts.
	    if (template == null) return;

	    // first pass was just to catch literals. reset.
        simpleText = null;

	    // oh yeah -- we're going old-school
	    ///char[] chars = template.toCharArray();
	    char c, c2;
	    // how many regex delims left before tag parser has to wake up again
	    int regexDelimCount = 0;
	    
	    int marker = 0; // beginning of latest static span
	    int tagStart = -1; // beginning of latest tag span
	    int trailingBackslashes = 0; // track escape chars so we can ignore escapes
	    
	    // the parser has to ignore certain chars when in various states
	    boolean insideRegex = false;
	    boolean insideTrToken = false;
        boolean insideComment = false;
        boolean insideLiteral = false;
        
        // magicChar determines what sort of tag are we forming
	    char magicChar = 0;
	    
	    int len = template.length();
	    
	    for (int i=0; i<len; i++) {
	        //c = chars[i];
	        c = template.charAt(i);
	        
	        if (tagStart < 0) {
	            
	            // edge case - can't start a tag on final char of sequence
	            if (i+1 >= len) break;
	            
                //c2 = chars[i+1];
	            c2 = template.charAt(i+1);
	            // collecting static until tag comes along
	            if (c == '{') {
    	            if (MAGIC_CHARS.indexOf(c2) > -1) {
    	                // avoid being tricked by javascript that only smells like a tag.
    	                if (c2 == '$' && isJavascriptHeadFake(i,template)) {
    	                    // not a chunk tag, keep scanning, nothing to see here.
    	                } else {
        	                // FOUND TAG START
        	                tagStart = i;
        	                trailingBackslashes = 0;
        	                i++;
        	                magicChar = c2;
    	                }
    	            }
	            } else if (c == '_' && c2 == '[') {
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
    	                String wholeTag = template.substring(tagStart,i+1);
                        String tokenStr = template.substring(tagStart+2,i);
                        SnippetToken token = new SnippetToken(wholeTag,tokenStr);
                        parts.add(token);
                        // reset scan mode
                        marker = i+1;
                        tagStart = -1;
                        insideTrToken = false;
	                }
	            } else if (c == '}') {
	                if (!insideRegex && trailingBackslashes % 2 == 0) {
	                    if (insideLiteral) {
                            // scanning for end of literal
                            if (isLiteralClose(template,magicChar,tagStart,i)) {
                                String literalText = template.substring(marker,i+1);
                                SnippetPart literal = new SnippetPart(literalText);
                                literal.setLiteral(true);
                                parts.add(literal);
                                // reset...
                                marker = i+1;
                                insideLiteral = false;
                            }
                            tagStart = -1;
	                    } else if (magicChar == '!') {
                            //char c0 = chars[i-1];
                            //char c00 = chars[i-2];
	                        char c0 = template.charAt(i-1);
                            char c00 = template.charAt(i-2);
                            if (c0 == '-' && c00 == '-') {
                                // FOUND COMMENT END
                                // FIXME move this whole block its own separate method
                                
                                // strip comment into non-rendering part
                                if (parts == null) parts = new ArrayList<SnippetPart>();
                                String precedingComment = null;
                                
                                int startOfThisLine = marker;
                                if (marker < tagStart) {
                                    precedingComment = template.substring(marker,tagStart);
                                    
                                    // might need to strip empty line left by stripped comment.
                                    // locate the start of this line by backtracking
                                    int lineBreakPos = precedingComment.lastIndexOf('\n');
                                    if (lineBreakPos > -1) {
                                        startOfThisLine = marker + lineBreakPos + 1;
                                    }
                                    
                                }
                                
                                // If eating comment leaves empty line, eat empty line too.
                                //
                                // ie, IF the span between final linebreak and tag is all whitespace
                                // (or tag is not preceded by anything but whitespace)
                                // *AND* the template following the comment begins with whitespace
                                // and a linebreak (or the template-end), eat the preceding whitespace
                                // and skip past the end of line.
                                if (startOfThisLine == tagStart || template.substring(startOfThisLine,tagStart).trim().length()==0) {
                                    // locate end of this line
                                    int endOfLine = template.indexOf('\n',i+1);
                                    if (endOfLine < 0) {
                                        endOfLine = len;
                                    }
                                    if (template.substring(i+1,endOfLine).trim().length()==0) {
                                        // yep, need to eat empty line and linebreak
                                        if (startOfThisLine < tagStart) {
                                            // strip leading whitespace from preceding static
                                            // and shift that whitespace into the comment text
                                            precedingComment = template.substring(marker,startOfThisLine);
                                            tagStart = startOfThisLine;
                                        }
                                        // skip ahead to end of condemned line
                                        i = (endOfLine == len) ? endOfLine-1 : endOfLine;
                                    }
                                }
                                
                                // preserve static leading up to comment
                                if (precedingComment != null) {
                                    SnippetPart literal = new SnippetPart(precedingComment);
                                    literal.setLiteral(true);
                                    parts.add(literal);
                                }
                                
                                // this grabs the comment tag as well as any surrounding
                                // whitespace that's being eaten (see above)
                                String wholeComment = template.substring(tagStart,i+1);
                                SnippetComment comment = new SnippetComment(wholeComment);
                                parts.add(comment);
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
    	                    SnippetPart tag = extractTag(magicChar,template,marker,tagStart,i);
    	                    if (tag != null) {
    	                        parts.add(tag);
                                // reset scan mode
                                marker = i+1;
                                tagStart = -1;
    	                    } else {
    	                        // uh-oh, literal block
                                insideLiteral = true;
    	                        marker = tagStart;
    	                        tagStart = -1;
    	                    }
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
    	                //char c0 = chars[i-1];
    	                //char c00 = chars[i-2];
	                    char c0 = template.charAt(i-1);
	                    char c00 = template.charAt(i-2);
    	                if (c0 == 's' && c00 == '|') {
    	                    // found {~tag|s/.../.../}
    	                    // need to find two more
    	                    regexDelimCount = 2;
    	                    insideRegex = true;
    	                } else if (c0 == 'm' && (c00 == ',' || c00 == '(')) {
    	                    // found {~tag|...(m/.../,...,m/.../,...)}
    	                    regexDelimCount = 1;
    	                    insideRegex = true;
    	                } else if (c0 == ',' || (c0 == '(' && c00 == 'h')) {
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
	        if (insideComment) {
	            // add marker-to-end as comment
	            SnippetPart finalComment = new SnippetComment(template.substring(marker));
	            parts.add(finalComment);
	        } else if (marker < template.length()) {
	            // add marker-to-end as literal
	            SnippetPart finalLiteral = new SnippetPart(template.substring(marker));
	            finalLiteral.setLiteral(true);
	            parts.add(finalLiteral);
	        }
	        groupBlocks(parts);
	    }
        
	}

	/**
	 * Sniff out invalid tags that match specific common profile.
	 * 
	 * @return
	 */
	private static boolean isJavascriptHeadFake(int i, String template)
	{
	    int len = template.length();
        // verify that this is tag and not function(){$.someJQueryFn()}
        if (i+2 >= len) return true;
        
        char c3 = template.charAt(i+2);
        // {$.XXX} {$(XXX)} and {$ XXX} and {$$xxx} are never valid chunk tags
        if (c3 == '.' || c3 == '(' || c3 == ' ' || c3 == '$') {
            // eep, not a tag!  probably a bit of javascript/jQuery.
            return true;
        }
        // also, don't be fooled by function(){$j(...)}
        // this catches a lot of prototype utility calls.
        if (i+3 < len) {
            char c4 = template.charAt(i+3);
            if (c4 == '(') {
                return true;
            }
        }
        
        // did not detect javascript.  probably a legit tag.
        return false;
	}
	
    public boolean isSimple()
    {
        return simpleText != null;
    }
    
	private boolean isLiteralClose(String template, char magicChar, int tagStart, int i)
	{
        if (magicChar == '^') {
            if (tagStart == i-2) {
                // {^}
                return true;
            }
        } else if (magicChar == '~') {
            if (tagStart == i-3 && template.charAt(i-1) == '.') {
                // {~.}
                return true;
            } else if (tagStart == i-11 && template.substring(tagStart+3,i).equals("/literal")) {
                // {~./literal}
                return true;
            }
        } else if (tagStart == i-9 && magicChar == '/') {
            if (template.substring(tagStart+1,i).equals("/literal")) {
                // {/literal}
                return true;
            }
        }
        return false;
	}
	
    private SnippetPart extractTag(char magicChar, String template,
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
        
        if (magicChar == '~' || magicChar == '$') {
            String gooeyCenter = template.substring(tagStart+2,i);
            SnippetTag tag = new SnippetTag(wholeTag,gooeyCenter);
            return tag;
        } else if (magicChar == '^' || magicChar == '.') {
            String gooeyCenter = template.substring(tagStart+2,i);
            // check for literal block (includes abandoned {^^} shorthand syntax)
            if (gooeyCenter.equals("literal") || gooeyCenter.equals("^")) {
                // null return signals literal-start to caller
                return null;
            }
            // expand ^ to ~.
            SnippetTag tag = new SnippetTag(wholeTag,"."+gooeyCenter);
            return tag;
        } else if (magicChar == '/') {
            String gooeyCenter = template.substring(tagStart+1,i);
            // expand {/ to {^/ to {~./
            SnippetTag tag = new SnippetTag(wholeTag,"."+gooeyCenter);
            return tag;
        } else if (magicChar == '*') {
            // convert macro syntax to internal macro block-tag
            if (wholeTag.length() == 3) {
                // this marks the end of the macro block {*}
                SnippetTag tag = new SnippetTag(wholeTag,"."+MacroTag.MACRO_END_MARKER);
                return tag;
            } else {
                int refEnd = i;
                if (template.charAt(i-1) == '*') refEnd--;
                String macroTemplate = template.substring(tagStart+2,refEnd).trim();
                SnippetTag macroHead = new SnippetTag(wholeTag,"."+MacroTag.MACRO_MARKER+" "+macroTemplate+" original");
                return macroHead;
            }
        } else if (magicChar == '=') {
            if (wholeTag.length() == 3) {
                // this marks the end of a macro def {=}
                SnippetTag tag = new SnippetTag(wholeTag,"=");
                return tag;
            }
        } else if (magicChar == '_') {
            // for example: {_[token %s %s],~with,~args}
            SnippetToken token = SnippetToken.parseTokenWithArgs(wholeTag);
            return token;
        } else if (magicChar == '+') {
            // include shorthand: {+template#ref} or {+(cond)template#ref}
            if (wholeTag.startsWith("{+(")) {
                String includeIfTag = ".includeIf(" + template.substring(tagStart+3,i);
                SnippetTag condInclude = new SnippetTag(wholeTag,includeIfTag);
                return condInclude;
            } else {
                String includeTag = ".include " + template.substring(tagStart+2,i);
                SnippetTag include = new SnippetTag(wholeTag,includeTag);
                return include;
            }
        }
        
        // huh? in other words, we should never reach here.
        // but just in case, let's make sure the wheels don't completely come off.
        SnippetPart wackyTag = new SnippetPart(wholeTag);
        return wackyTag;
    }

    /*
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
	}*/
	
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
	                    
	                    if (blockTag.doSmartTrimAroundBlock()) {
	                        smartTrimBeforeBlockStart(bodyParts,blockTag,i-1);
	                        smartTrimAfterBlockEnd(bodyParts,blockTag,i+1);
	                    }
	                } else {
	                    // unmatched block tag!!  output error notice.
	                    // TODO should create a SnippetErrorPart class
	                    // and offer options wrt, exclude errors
	                    // in final output, etc.
	                    String errMsg = "[ERROR in template! "+helper.getBlockStartMarker()+" block with no matching end marker! ]";
	                    SnippetError errorPart = new SnippetError(errMsg);
	                    bodyParts.add(i+1,errorPart);
	                    i++;
	                }
	            }
	        }
	    }
	}
	
	private boolean smartTrimBeforeBlockStart(List<SnippetPart> parts, SnippetBlockTag blockTag, int prevPartIdx)
	{
	    if (prevPartIdx < 0) return false;
	    
	    // first, ensure that block body begins with (whitespace+)LF
	    // if not, abort -- smart-trim is not warranted.
	    if (blockBodyStartsOnSameLine(blockTag)) {
	        // abort
	        return false;
	    }
    
	    SnippetPart prevPart = parts.get(prevPartIdx);
	    
	    // skip non-rendering comments
	    while (prevPart instanceof SnippetComment) {
	        prevPartIdx--;
	        if (prevPartIdx < 0) return false;
	        prevPart = parts.get(prevPartIdx);
	    }
	    
	    if (!prevPart.isLiteral()) {
	        if (prevPart instanceof SnippetBlockTag) {
	            prevPart = ((SnippetBlockTag)prevPart).getCloseTag();
	        } else {
	            // abort
	            return false;
	        }
	    }
	    
        String text = prevPart.getText();
        if (text.length() == 0) {
            return smartTrimBeforeBlockStart(parts, blockTag, prevPartIdx-1);
        }
        
        // if the block ends with LF + whitespace, trim whitespace
        int i = text.length() - 1;
        char c = text.charAt(i);
        boolean eatWhitespace = false;
        while (Character.isWhitespace(c)) {
            if (c == '\n' || c == '\r') {
                i++;
                eatWhitespace = true;
                break;
            }
            i--;
            if (i<0) {
                i = 0;
                if (prevPartIdx == 0) {
                    // beginning of snippet is also a "new line"
	                eatWhitespace = true;
                } else {
	                // Still just whitespace so far,
	                // no newlines -- should keep scanning
                    // backwards through parts.  recurse.
                    if (smartTrimBeforeBlockStart(parts, blockTag, prevPartIdx-1)) {
                        eatWhitespace = true;
                    } else {
                        return false;
                    }
                }
                break;
            }
            c = text.charAt(i);
        }
        
        if (eatWhitespace) {
            prevPart.setText( text.substring(0,i) );
            // TODO preserve eaten space as non-rendering part?
        }
        
        return eatWhitespace;
	}
	
    private static final Pattern UNIVERSAL_LF = Pattern.compile("\n|\r\n|\r\r");

    private boolean blockBodyStartsOnSameLine(SnippetBlockTag blockTag)
    {
        Snippet blockBody = blockTag.getBody();
        if (blockBody.parts != null) {
            int i = 0;
            while (i < blockBody.parts.size()) {
                SnippetPart firstPart = blockBody.parts.get(i);
                // skip comments... unless comment already ate LF
                if (firstPart instanceof SnippetComment) {
                    String commentText = firstPart.toString();
                    if (commentText.charAt(commentText.length()-1) != '}') {
                        // ate linefeed already, but it was there.
                        // do not abort.
                        return false;
                    }
                    i++;
                    continue;
                }
                
                // must start with (whitespace)+LF or no smart-trim
                if (firstPart.isLiteral()) {
                    String text = firstPart.getText();
                    Matcher m = UNIVERSAL_LF.matcher(text);
                    if (m.find()) {
                        int firstLF = m.start();
                        if (text.substring(0,firstLF).trim().length() == 0) {
                            // trim this too?
                        } else {
                            // abort! no smart-trim.
                            return true;
                        }
                    }
                }
                return false;
            }
        }
        
        return false;
    }
    
    private void smartTrimAfterBlockEnd(List<SnippetPart> parts, SnippetBlockTag blockTag, int nextPartIdx)
	{
	    if (parts.size() <= nextPartIdx) return;
	    SnippetPart nextPart = parts.get(nextPartIdx);
	    
	    // make sure to skip over non-rendering comments
	    while (nextPart instanceof SnippetComment) {
	        String commentText = nextPart.toString();
	        if (commentText.charAt(commentText.length()-1) != '}') {
	            // already ate, bail!  no double-trimming.
	            return;
	        }
	        nextPartIdx++;
	        if (parts.size() <= nextPartIdx) return;
	        nextPart = parts.get(nextPartIdx);
	    }
	    
	    if (nextPart.isLiteral()) {
	        String text = nextPart.getText();
	        // if the block begins with (whitespace+) LF, trim initial line
	        Matcher m = UNIVERSAL_LF.matcher(text);
	        
	        if (m.find()) {
	            int firstLF = m.start();
	            if (text.substring(0,firstLF).trim().length() == 0) {
	                nextPart.setText( text.substring(m.end()) );
	                // shift whitespace into blockTag's end-tag?
	                // or make a non-rendering part?
	                blockTag.getCloseTag().snippetText += text.substring(0,m.end());
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

    public Snippet copy()
    {
        if (simpleText != null) {
            Snippet copy = new Snippet("");
            copy.simpleText = simpleText;
            return copy;
        } else {
            List<SnippetPart> partsCopy = new ArrayList<SnippetPart>();
            partsCopy.addAll(parts);
            Snippet copy = new Snippet(partsCopy);
            return copy;
        }
    }
    
    public static Snippet makeLiteralSnippet(String literal)
    {
        SnippetPart x = new SnippetPart(literal);
        x.setLiteral(true);
        
        List<SnippetPart> listOfOne = new ArrayList<SnippetPart>();
        listOfOne.add(x);
        
        return new Snippet(listOfOne);
    }

    /**
     * Let's say your whole snippet is just {$x}
     * In certain contexts, it's nice to resolve to the value of
     * x instead of the chunk-evaluation of x into a string.
     */
    boolean isSimplePointer()
    {
        if (parts != null && parts.size() == 1) {
            SnippetPart onlyPart = parts.get(0);
            if (onlyPart instanceof SnippetTag) {
                return true;
            }
        }
        return false;
    }

    String getPointer()
    {
        if (isSimplePointer()) {
            SnippetPart onlyPart = parts.get(0);
            SnippetTag tag = (SnippetTag)onlyPart;
            return tag.getTag();
        }
        return null;
    }
    
    static Snippet consolidateSnippets(Vector<Snippet> template)
    {
        if (template == null) return null;
        if (template.size() == 1) return template.get(0);
        // can't just slap all parts together,
        // since block tag might start in one snippet and end in another.
        // so, first FLATTEN all the pieces, then run through groupBlocks
        List<SnippetPart> merged = new ArrayList<SnippetPart>();
        for (int i=0; i<template.size(); i++) {
            Snippet s = template.get(i);
            merged.addAll(s.ungroupBlocks());
        }
        
        Snippet voltron = new Snippet(merged);
        voltron.groupBlocks(voltron.parts);
        
        return voltron;
    }
    
    private List<SnippetPart> ungroupBlocks()
    {
        if (parts == null) {
            if (simpleText.length() < 1) {
                return null;
            } else {
                List<SnippetPart> onePart = new ArrayList<SnippetPart>();
                SnippetPart simplePart = new SnippetPart(simpleText);
                simplePart.setLiteral(true);
                onePart.add(simplePart);
                return onePart;
            }
        } else {
            // best case: no blocks
            boolean noBlocks = true;
            for (SnippetPart part : parts) {
                if (part instanceof SnippetBlockTag || part instanceof SnippetError) {
                    noBlocks = false;
                    break;
                }
            }
            if (noBlocks) {
                return parts;
            }
            // otherwise...
            List<SnippetPart> flat = new ArrayList<SnippetPart>();
            for (SnippetPart part : parts) {
                if (part instanceof SnippetBlockTag) {
                    // flatten block tag...
                    //  how to detect un-closed block tag?
                    SnippetBlockTag block = (SnippetBlockTag)part;
                    flat.add(block.getOpenTag());
                    Snippet body = block.getBody();
                    flat.addAll(body.ungroupBlocks());
                    flat.add(block.getCloseTag());
                } else if (part instanceof SnippetError) {
                    // should qualify this somehow but for now, just
                    // lose the error.
                } else {
                    flat.add(part);
                }
            }
            return flat;
        }
    }
    
}
