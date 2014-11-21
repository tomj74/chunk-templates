package com.x5.template;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.filters.RegexFilter;

public class IfTag extends BlockTag
{
    // chosenPath simplest case: 0 is "then", -1 is "else"
    // else-if case: 0 is "then", 1 is first else-if, 2 is 2nd else-if, -1 is "else"
    private String primaryCond;

    private Snippet body;
    private boolean doTrim = true;

    private Map<String,String> options;

    public IfTag(String params, Snippet body)
    {
        parseParams(params);
        initBody(body);
    }

    public IfTag()
    {
    }

    public String getBlockStartMarker()
    {
        return "if";
    }

    public String getBlockEndMarker()
    {
        return "/if";
    }

    private void initBody(Snippet body)
    {
        // FIXME pre-scan body to identify all ^else and ^elseIf blocks
        this.body = body;
    }

    private void parseParams(String params)
    {
        String cond = parseCond(params);
        primaryCond = cond;
        /*
        if (context != null) {
            if (isTrueExpr(primaryCond, context)) {
                chosenPath = 0;
            } else {
                chosenPath = -1;
            }
        }*/
        options = parseAttributes(params);
        if (options == null) return;

        //thenTemplate = options.get("then");
        //elseTemplate = options.get("else");
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
            //int closeParenPos = params.indexOf(")",openParenPos+1);
            int closeParenPos = params.lastIndexOf(")");
            if (quotedCondPos < 0 && closeParenPos > openParenPos) {
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

    private boolean isTrueExpr(String test, Chunk context)
    {
        if (test == null) return false;
        test = test.trim();
        if (test.length() == 0) return false;

        char firstChar = test.charAt(0);
        if (firstChar == '!' || firstChar == '~' || firstChar == '$') {
            test = test.substring(1);
        }
        // eat one more in the !~tag case
        if (firstChar == '!' && (test.charAt(0) == '~' || test.charAt(0) == '$')) {
            test = test.substring(1);
        }

        if (test.indexOf('=') < 0 && test.indexOf("!~") < 0) {
            // simplest case: no comparison, just a non-null (or null) test
            Object tagValue = context.get(test);
            if (firstChar == '~' || firstChar == '$') {
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

                if (tagB.charAt(0) == '~' || tagB.charAt(0) == '$') {
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
                        // FIXME should scan for unescaped end-quote in the middle of the string
                        match = tagB.substring(1, tagB.length()-1);
                        // quoted strings may have escaped characters.
                        // unescape.
                        match = unescape(match);
                    } else if (tagB.charAt(0) == '\'' && tagB.charAt(match.length()-1) == '\'') {
                        // FIXME should scan for unescaped end-quote in the middle of the string
                        match = tagB.substring(1, tagB.length()-1);
                        // unescape
                        match = unescape(match);
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

        Object tagValue = context.get(var);

        boolean isMatch = isMatch(tagValue == null ? null : tagValue.toString(), regex);

        if (neg) {
            return isMatch ? false : true;
        } else {
            return isMatch ? true : false;
        }
    }

    private String unescape(String x)
    {
        // this method does more or less what we want
        return RegexFilter.parseRegexEscapes(x);
    }

    private boolean isMatch(String text, String regex)
    {
        if (text == null || regex == null) return false;
        regex = regex.trim();

        int cursor = 0;
        if (regex.charAt(cursor) == 'm') cursor++;
        if (regex.charAt(cursor) == '/') cursor++;
        int regexEnd = RegexFilter.nextRegexDelim(regex,cursor);
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

        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        return m.find();
    }

    public boolean doSmartTrimAroundBlock()
    {
        return true;
    }

    private String trimLeft(String x)
    {
        if (x == null) return null;
        int i = 0;
        char c = x.charAt(i);
        while (c == '\n' || c == ' ' || c == '\r' || c == '\t') {
            i++;
            if (i == x.length()) break;
            c = x.charAt(i);
        }
        if (i == 0) return x;
        return x.substring(i);
    }

    private String trimRight(String x)
    {
        if (x == null) return null;
        int i = x.length()-1;
        char c = x.charAt(i);
        while (c == '\n' || c == ' ' || c == '\r' || c == '\t') {
            i--;
            if (i == -1) break;
            c = x.charAt(i);
        }
        i++;
        if (i >= x.length()) return x;
        return x.substring(0,i);
    }

    private boolean isTrimAll()
    {
        String trimOpt = (options != null) ? (String)options.get("trim") : null;
        if (trimOpt != null && (trimOpt.equals("all") || trimOpt.equals("true"))) {
            return true;
        } else {
            return false;
        }
    }

    private String smartTrim(String x)
    {
        return smartTrim(x, false);
    }

    private static final Pattern UNIVERSAL_LF = Pattern.compile("\n|\r\n|\r\r");

    private String smartTrim(String x, boolean ignoreAll)
    {
        if (!ignoreAll && isTrimAll()) {
            // trim="all" disables smartTrim.
            return x.trim();
        }

        // if the block begins with (whitespace+) LF, trim initial line
        // otherwise, apply standard/complete trim.
        Matcher m = UNIVERSAL_LF.matcher(x);

        if (m.find()) {
            int firstLF = m.start();
            if (x.substring(0,firstLF).trim().length() == 0) {
                return x.substring(m.end());
            }
        }

        return x;
    }

    private int nextElseTag(List<SnippetPart> bodyParts, int startAt)
    {
        for (int i=startAt; i<bodyParts.size(); i++) {
            SnippetPart part = bodyParts.get(i);
            if (part instanceof SnippetTag) {
                SnippetTag tag = (SnippetTag)part;
                if (tag.getTag().startsWith(".else")) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void renderBlock(Writer out, Chunk context, String origin, int depth)
    throws IOException
    {

        List<SnippetPart> bodyParts = body.getParts();
        int nextElseTag = nextElseTag(bodyParts,0);

        //int path = 0;

        if (isTrueExpr(primaryCond, context)) {
            //chosenPath = 0;
            if (nextElseTag < 0) nextElseTag = bodyParts.size();
            renderChosenParts(out, context, origin, depth, bodyParts, 0, nextElseTag);
        } else {
            // locate next {^else} or {^elseIf} tag, or output nothing
            while (nextElseTag > -1) {
                //path++;
                String elseTag = ((SnippetTag)bodyParts.get(nextElseTag)).getTag();
                if (elseTag.equals(".else")) {
                    //chosenPath = path;
                    renderChosenParts(out, context, origin, depth, bodyParts, nextElseTag+1, bodyParts.size());
                    break;
                } else {
                    String elseIfCond = parseCond(elseTag);
                    if (isTrueExpr(elseIfCond, context)) {
                        //chosenPath = path;
                        int nextBoundary = nextElseTag(bodyParts,nextElseTag+1);
                        if (nextBoundary == -1) nextBoundary = bodyParts.size();
                        renderChosenParts(out, context, origin, depth, bodyParts, nextElseTag+1, nextBoundary);
                        break;
                    }
                    nextElseTag = nextElseTag(bodyParts,nextElseTag+1);
                }
            }
        }
    }

    public void renderChosenParts(Writer out, Chunk context, String origin, int depth,
            List<SnippetPart> parts, int a, int b)
    throws IOException
    {
        if (!doTrim) {
            for (int i=a; i<b; i++) {
                SnippetPart part = parts.get(i);
                part.render(out, context, origin, depth);
            }
        } else if (b > a) {
            if (isTrimAll()) {
                // skip leading comments for a proper trim
                while (parts.get(a) instanceof SnippetComment && a < b-1) {
                    a++;
                }
                // trim front and back
                if (a + 1 == b) {
                    // only one part, easy to trim
                    SnippetPart onlyPart = parts.get(a);
                    if (onlyPart.isLiteral()) {
                        String trimmed = onlyPart.getText().trim();
                        out.append(trimmed);
                    } else {
                        onlyPart.render(out, context, origin, depth);
                    }
                } else {
                    // output first part (left-trimmed)
                    SnippetPart partA = parts.get(a);
                    if (partA.isLiteral()) {
                        String trimmed = trimLeft(partA.getText());
                        out.append(trimmed);
                    }
                    // output middle (untouched)
                    for (int i=a+1; i<b-1; i++) {
                        SnippetPart part = parts.get(i);
                        part.render(out, context, origin, depth);
                    }
                    // output last part (right-trimmed)
                    SnippetPart partB = parts.get(b-1);
                    if (partB.isLiteral()) {
                        String trimmed = trimRight(partB.getText());
                        out.append(trimmed);
                    }
                }
            } else {
                // trim only first blank line
                // output first part (smart-trimmed)
                SnippetPart partA = parts.get(a);
                if (partA.isLiteral()) {
                    String smartTrimmed = smartTrim(partA.getText());
                    out.append(smartTrimmed);
                } else {
                    partA.render(out, context, origin, depth);
                }
                // output rest (untouched)
                for (int i=a+1; i<b; i++) {
                    SnippetPart part = parts.get(i);
                    part.render(out, context, origin, depth);
                }
            }
        }
    }
}