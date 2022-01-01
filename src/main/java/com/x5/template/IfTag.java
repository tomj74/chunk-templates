package com.x5.template;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IfTag extends BlockTag
{
    private String primaryCond;
    private Map<String,CondLexer> condTests = new HashMap<String,CondLexer>();

    private Snippet body;
    private boolean doTrim = true;

    private Map<String,String> options;

    public IfTag(String params, Snippet body)
    {
        parseParams(params);
        this.body = body;
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

    private void parseParams(String params)
    {
        String baseParam = baseParameter(params);
        condTests.put(primaryCond, new CondLexer(baseParam));

        options = parseAttributes(params);
        if (options == null) {
            return;
        }

        String trimOpt = options.get("trim");
        if (trimOpt != null) {
            if (trimOpt.equalsIgnoreCase("false") || trimOpt.equalsIgnoreCase("none")) {
                doTrim = false;
            }
        }
    }

    private String stripCasing(String params)
    {
        if (params == null) return null;

        // skip over directive, strip parens if encased.
        int exprPos = params.indexOf("f") + 1; // if or elseIf
        String test = params.substring(exprPos).trim();
        if (test.charAt(0) == '(' && test.charAt(test.length()-1) == ')') {
            // DON'T strip outer parens if there are ANY inner parens
            // in fact, maybe never strip outer parens anymore,
            // ie now that the cond expr parser can handle parens?
            int innerParenPos = test.indexOf(')', 1);
            if (innerParenPos == test.length()-1) {
                test = test.substring(1, test.length() - 1);
            }
        }

        return test;
    }

    private static final Pattern paramPattern = Pattern.compile(" ([a-zA-Z0-9_-]+)=(\"([^\"]*)\"|'([^\']*)')");

    private Map<String,String> parseAttributes(String params)
    {
        // find and save all xyz="abc" style attributes
        Matcher m = paramPattern.matcher(params);
        HashMap<String,String> opts = null;
        while (m.find()) {
            m.group(0); // need to do this for subsequent number to be correct?
            String paramName = m.group(1);
            String dubQuotedValue = m.group(3);
            String singleQuotedValue = m.group(4);
            String paramValue = dubQuotedValue != null ? dubQuotedValue : singleQuotedValue;
            if (opts == null) opts = new HashMap<String,String>();
            opts.put(paramName, paramValue);
        }
        return opts;
    }

    // strip away options, directive, paren casing.
    private String baseParameter(String params)
    {
        StringBuffer stripped = null;

        // find and remove all xyz="abc" style attributes
        Matcher m = paramPattern.matcher(params);
        while (m.find()) {
            if (stripped == null) stripped = new StringBuffer();
            m.appendReplacement(stripped, "");
        }

        if (stripped != null) {
            m.appendTail(stripped);
            params = stripped.toString();
        }

        return stripCasing(params);
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

    private boolean isTrueExpr(String test, Chunk context)
    {
        CondLexer compiledTest = condTests.get(test);
        if (compiledTest == null) {
            compiledTest = new CondLexer(test);
            condTests.put(test, compiledTest);
        }

        try {
            return compiledTest.parse().isTrue(context);
        } catch (InvalidExpressionException e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    public void renderBlock(Writer out, Chunk context, String origin, int depth)
    throws IOException
    {

        List<SnippetPart> bodyParts = body.getParts();
        int nextElseTag = nextElseTag(bodyParts,0);

        if (isTrueExpr(primaryCond, context)) {
            if (nextElseTag < 0) nextElseTag = bodyParts.size();
            renderChosenParts(out, context, origin, depth, bodyParts, 0, nextElseTag);
        } else {
            // locate next else or elseIf tag, or output nothing
            while (nextElseTag > -1) {
                String elseTag = ((SnippetTag)bodyParts.get(nextElseTag)).getTag();
                if (elseTag.equals(".else")) {
                    renderChosenParts(out, context, origin, depth, bodyParts, nextElseTag+1, bodyParts.size());
                    break;
                } else {
                    String elseIfCond = stripCasing(elseTag);
                    if (isTrueExpr(elseIfCond, context)) {
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
