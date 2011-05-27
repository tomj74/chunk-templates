package com.x5.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IfTag extends BlockTag
{
    // chosenPath simplest case: 0 is "then", -1 is "else"
    // else-if case: 0 is "then", 1 is first else-if, 2 is 2nd else-if, -1 is "else"
    private int chosenPath;
    private String primaryCond;
    
    private String thenTemplate;
    private String elseTemplate;
    
    //private String[] altConds;
    //private String[] altTemplates;
    
    private Chunk context;
    private boolean doTrim = true;
    
    private Map<String,String> options;
    
    public IfTag(String params, Chunk context)
    {
        this.context = context;
        parseParams(params);
    }
    
    public String getBlockStartMarker()
    {
        return "if";
    }
    
    public String getBlockEndMarker()
    {
        return "/if";
    }
    
    private void parseParams(String params)
    {
        String cond = parseCond(params);
        primaryCond = cond;
        if (isTrueExpr(primaryCond)) {
            chosenPath = 0;
        } else {
            chosenPath = -1;
        }
        options = parseAttributes(params);
        if (options == null) return;
        
        thenTemplate = options.get("then");
        elseTemplate = options.get("else");
        String trimOpt = options.get("trim");
        if (trimOpt != null) {
            if (trimOpt.equalsIgnoreCase("false") || trimOpt.equalsIgnoreCase("none")) {
                doTrim = false;
            }
        }
    }
    
    private String parseCond(String params)
    {
        if (params == null) return null;
        
        // look for conditional test. first, try parens (...)
        int openParenPos = params.indexOf("(");
        int quotedCondPos = params.indexOf(" cond=\"");
        
        if (openParenPos > -1) {
            int closeParenPos = params.indexOf(")",openParenPos+1);
            if (quotedCondPos < 0) {
                String test = params.substring(openParenPos+1,closeParenPos);
                return test;
            }
        }
        
        if (quotedCondPos > -1) {
            quotedCondPos += " cond='".length();
            int closeQuotePos = params.indexOf("\"",quotedCondPos);
            if (closeQuotePos < 0) {
                return params.substring(quotedCondPos);
            } else {
                return params.substring(quotedCondPos,closeQuotePos);
            }
        }
        
        return null;
    }
    
    private Map<String,String> parseAttributes(String params)
    {
        // find and save all xyz="abc" style attributes
        Pattern p = Pattern.compile(" ([a-zA-Z0-9_-]+)=(\"([^\"]*)\"|'([^\']*)')");
        Matcher m = p.matcher(params);
        HashMap<String,String> opts = null;
        while (m.find()) {
            m.group(0); // need to do this for subsequent number to be correct?
            String paramName = m.group(1);
            String paramValue = m.group(3);
            if (opts == null) opts = new HashMap<String,String>();
            opts.put(paramName, paramValue);
        }
        return opts;
    }
    
    public static String evalIf(String params, Chunk context)
    throws BlockTagException
    {
        IfTag obj = new IfTag(params, context);
        return obj._eval();
    }
    
    private String _eval()
    throws BlockTagException
    {
        if (thenTemplate == null) {
            throw new BlockTagException(this);
        }
        
        if (isTrueExpr(primaryCond)) {
            return snippetOrValue(thenTemplate);
        } else {
            return snippetOrValue(elseTemplate);
        }
    }
    
    private boolean isTrueExpr(String test)
    {
        if (test == null) return false;
        test = test.trim();
        if (test.length() == 0) return false;
        
        char firstChar = test.charAt(0);
        if (firstChar == '!' || firstChar == '~') test = test.substring(1);
        // eat one more in the !~tag case
        if (firstChar == '!' && test.charAt(0) == '~') test = test.substring(1);
        
        if (test.indexOf('=') < 0 && test.indexOf("!~") < 0) {
            // simplest case: no comparison, just a non-null (or null) test
            Object tagValue = context.get(test);
            if (firstChar == '~') {
                return (tagValue != null) ? true : false;
            } else if (firstChar == '!') {
                return (tagValue == null) ? true : false;
            } else {
                // should this be an error?
                return (test.equalsIgnoreCase("true")) ? true : false;
            }
        }

        // now handle straight equality/inequality
        // (~asdf == ~xyz) and (~asdf != ~xyz)
        boolean isNeg = false;
        if (test.indexOf("==") > 0 || (isNeg = test.indexOf("!=") > 0)) {
            String[] parts = test.split("!=|==");
            if (parts.length == 2) {
                String tagA = parts[0].trim();
                String tagB = parts[1].trim();

                // get A
                Object tagValue = context.get(tagA);
                String tagValueA = tagValue == null ? "" : tagValue.toString();
                
                if (tagB.charAt(0) == '~') {
                    // equality (or inequality) of two variables (tags)
                    // resolve both tags and compare
                    
                    // get B
                    tagValue = context.get(tagB.substring(1));
                    String tagValueB = tagValue == null ? "" : tagValue.toString();
                    
                    if (isNeg) {
                        return (tagValueA.equals(tagValueB)) ? false : true;
                    } else {
                        return (tagValueA.equals(tagValueB)) ? true : false;
                    }
                } else {
                    // equality (or inequality) of one variable (tag)
                    // compared to a constant string
                    String match = tagB;
                    // allow tagB to be quoted?  if so, strip quotes here
                    if (tagB.charAt(0) == '"' && tagB.charAt(match.length()-1) == '"') {
                        match = tagB.substring(1, tagB.length()-1);
                    }
                    if (isNeg) {
                        return (tagValueA.equals(match)) ? false : true;
                    } else {
                        return (tagValueA.equals(match)) ? true : false;
                    }
                }
            }
        }

        // handle pattern match
        String[] parts = test.split("=~");
        boolean neg = false;
        if (parts.length != 2) {
            parts = test.split("!~");
            neg = true;
            if (parts.length != 2) {
                return false; // or error?
            }
        }
        
        String var = parts[0].trim();
        String regex = parts[1].trim();
        
        Object tagValue = null;
        try { tagValue = context.resolveTagValue(var); } catch(BlockTagException e) {}

        boolean isMatch = isMatch(tagValue == null ? null : tagValue.toString(), regex);
        
        if (neg) {
            return isMatch ? false : true;
        } else {
            return isMatch ? true : false;
        }
    }
    
    private boolean isMatch(String text, String regex)
    {
        if (text == null || regex == null) return false;
        regex = regex.trim();
        
        int cursor = 0;
        if (regex.charAt(cursor) == 'm') cursor++;
        if (regex.charAt(cursor) == '/') cursor++;
        int regexEnd = TextFilter.nextRegexDelim(regex,cursor);
        if (regexEnd < 0) return false; // fatal, unmatched regex boundary

        String pattern = regex.substring(cursor,regexEnd);

        // check for modifiers after regex end
        boolean ignoreCase = false;
        boolean multiLine = false;
        boolean dotAll = false;

        for (int i=regex.length()-1; i>regexEnd; i--) {
            char option = regex.charAt(i);
            if (option == 'i') ignoreCase = true;
            if (option == 'm') multiLine = true;
            if (option == 's') dotAll = true; // dot matches newlines too
        }

        if (multiLine) pattern = "(?m)" + pattern;
        if (ignoreCase) pattern = "(?i)" + pattern;
        if (dotAll) pattern = "(?s)" + pattern;

        return TextFilter.matches(text,pattern);
    }
    
    private String snippetOrValue(String result)
    {
        ContentSource theme = context.getTemplateSet();
        if (theme.provides(result)) {
            return theme.fetch(result);
        } else {
            return result;
        }
    }
    
    public String cookBlock(String blockBody)
    {
        // locate correct block from then, else, elseIf blocks
        // trim unless trim="false"
        // return chosen block
        Pattern p = Pattern.compile("\\{\\~\\.else[^\\}]*}"); // FIXME what about curly braces inside a regex?
        Matcher m = p.matcher(blockBody);

        String nestedIf = context.tagStart+".if";
        int nestedIfPos = blockBody.indexOf(nestedIf);
        int nestedBlockEnd = -1;
        
        int marker = 0;
        
        ElseScan:
        while (m.find()) {
            // this else might be from a nested if -- in that case, ignore
            while (nestedIfPos > -1 && nestedIfPos < m.start()) {
                if (nestedBlockEnd < nestedIfPos) {
                    int[] endSpan = findMatchingBlockEnd(context,blockBody,nestedIfPos+nestedIf.length(),this);
                    nestedBlockEnd = endSpan == null ? -1 : endSpan[1];
                }
                if (m.start() < nestedBlockEnd) {
                    // nested else, ignore
                    continue ElseScan;
                } else {
                    // we are past a nested if-- check if we're inside another one
                    if (nestedBlockEnd > 0) {
                        nestedIfPos = blockBody.indexOf(nestedIf,nestedBlockEnd);
                    }
                }
            }
            
            if (marker == 0) {
                thenTemplate = blockBody.substring(0,m.start());
                if (doTrim) thenTemplate = smartTrim(thenTemplate);
                if (chosenPath == 0) {
                    return thenTemplate;
                }
            }
            
            String elseTag = blockBody.substring(m.start(),m.end());
            String cond = parseCond(elseTag);
            if (cond == null) {
                // simple if-else, else wins
                elseTemplate = blockBody.substring(m.end());
                if (doTrim) elseTemplate = smartTrim(elseTemplate);
                return elseTemplate;
            } else {
                marker = m.end();
                if (isTrueExpr(cond)) {
                    int altBlockEnd = blockBody.length();
                    if (m.find()) {
                        altBlockEnd = m.start();
                    }
                    String altBlock = blockBody.substring(marker,altBlockEnd);
                    if (doTrim) altBlock = smartTrim(altBlock);
                    return altBlock;
                }
            }
        }
        
        if (chosenPath == 0 && marker == 0) {
            thenTemplate = blockBody;
            if (doTrim) thenTemplate = smartTrim(thenTemplate);
            return thenTemplate;
        }
        
        return "";
    }
    
    private String smartTrim(String x)
    {
        String trimOpt = options == null ? null : options.get("trim");
        if (trimOpt != null) {
            if (trimOpt.equalsIgnoreCase("all") || trimOpt.equalsIgnoreCase("true")) {
                return x.trim();
            }
        }

        // if the block begins with (whitespace+) LF, trim initial line
        // otherwise, apply no trim.
        Pattern p = Pattern.compile("\n|\r\n|\r\r");
        Matcher m = p.matcher(x);
        
        if (m.find()) {
            int firstLF = m.start();
            if (x.substring(0,firstLF).trim().length() == 0) {
                return x.substring(m.end());
            }
        }
        
        return x;
    }
}