package com.x5.template;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.filters.FilterArgs;
import com.x5.template.filters.RegexFilter;

public class IfTag extends BlockTag
{
    private String primaryCond;
    private Map<String,IfTest> condTests = new HashMap<String,IfTest>();

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
        primaryCond = parseCond(params);
        condTests.put(primaryCond, new IfTest(primaryCond));

        options = parseAttributes(params);
        if (options == null) return;

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
        int exprPos = params.indexOf("f") + 1; // if or elseIf
        int openParenPos = params.indexOf("(", exprPos);

        if (openParenPos > -1 && params.substring(exprPos,openParenPos).trim().length() == 0) {
            int closeParenPos = params.lastIndexOf(")");
            if (closeParenPos > openParenPos) {
                String test = params.substring(openParenPos+1,closeParenPos);
                return test;
            }
        }

        // no parens?  allow conditional to not be encased in parens, a la python/go.
        return params.substring(exprPos);
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
            String paramValue = m.group(3);
            if (opts == null) opts = new HashMap<String,String>();
            opts.put(paramName, paramValue);
        }
        return opts;
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
        IfTest compiledTest = condTests.get(test);
        if (compiledTest == null) {
            compiledTest = new IfTest(test);
            condTests.put(test, compiledTest);
        }

        return compiledTest.isTrue(context);
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
                    String elseIfCond = parseCond(elseTag);
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

    private static class IfTest
    {
        private static final int EXISTENCE = 0;
        private static final int COMPARE_CONSTANT = 1;
        private static final int COMPARE_REGEX = 2;
        private static final int COMPARE_TAGEXPR = 3;

        private int testType = 0;
        private boolean isNeg = false;

        // when comparing against a constant, this is the constant
        // when comparing against a regex, this is the regex
        private String compareTo = null;

        // if left side or right side are tag expressions, pre-init them and store here
        private SnippetTag leftSide = null;
        private SnippetTag rightSide = null;

        public IfTest(String test)
        {
            init(test);
        }

        private void init(String test)
        {
            if (test == null) return;
            test = test.trim();
            if (test.length() == 0) return;

            char firstChar = test.charAt(0);
            if (firstChar == '$' || firstChar == '!' || firstChar == '~') {
                test = test.substring(1);
            }
            // eat one more in the !~tag case
            if (firstChar == '!' && (test.charAt(0) == '$' || test.charAt(0) == '~')) {
                test = test.substring(1);
            }

            // find the end of the LHS
            int scanStart = skipModifiers(test);

            if (test.indexOf('=', scanStart) < 0 && test.indexOf("!~", scanStart) < 0) {
                this.testType = IfTest.EXISTENCE;
                // simplest case: no comparison, just a non-null (or null) test
                if (firstChar == '$' || firstChar == '~') {
                    this.leftSide = SnippetTag.parseTag(test);
                } else if (firstChar == '!') {
                    this.leftSide = SnippetTag.parseTag(test);
                    this.isNeg = true;
                } else {
                    this.isNeg = !test.equalsIgnoreCase("true");
                }
                return;
            }

            // now handle straight equality/inequality
            // ($asdf == $xyz) and ($asdf != $xyz)
            int eqPos = test.indexOf("==", scanStart);
            int ineqPos = test.indexOf("!=", scanStart);
            if (eqPos > 0 || ineqPos > 0) {
                this.isNeg = eqPos < 0;
                String tagA = test.substring(0, isNeg ? ineqPos : eqPos).trim();
                String tagB = test.substring((isNeg ? ineqPos : eqPos) + 2).trim();

                this.leftSide = SnippetTag.parseTag(tagA);

                if (tagB.charAt(0) == '$' || tagB.charAt(0) == '~') {
                    this.rightSide = SnippetTag.parseTag(tagB.substring(1));
                    this.testType = IfTest.COMPARE_TAGEXPR;
                } else {
                    this.testType = IfTest.COMPARE_CONSTANT;
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
                    this.compareTo = match;
                }
                return;
            }

            // handle pattern match
            int regexOpPos = test.indexOf("=~", scanStart);
            int negRegexOpPos = test.indexOf("!~", scanStart);
            if (regexOpPos < 0 && negRegexOpPos < 0) {
                isNeg = true;
                return;
            }
            this.isNeg = regexOpPos < 0;
            regexOpPos = isNeg ? negRegexOpPos : regexOpPos;

            String var = test.substring(0, regexOpPos).trim();
            String regex = test.substring(regexOpPos + 2).trim();
            this.leftSide = SnippetTag.parseTag(var);
            this.testType = IfTest.COMPARE_REGEX;
            this.compareTo = regex;
        }

        private int skipModifiers(String test)
        {
            char[] chars = test.toCharArray();
            int i=0;
            for (; i<chars.length; i++) {
                char c = chars[i];
                if (Character.isJavaIdentifierPart(c)) continue;
                if (c == '|' || c == ':' || c == '.') continue;
                if (c == '(') {
                    i = FilterArgs.nextUnescapedDelim(")", test, i+1);
                    continue;
                }
                if (c == '/') {
                    i = RegexFilter.nextRegexDelim(test, i+1);
                    continue;
                }
                // end of the road
                break;
            }
            return i;
        }

        private String unescape(String x)
        {
            // this method does more or less what we want
            return RegexFilter.parseRegexEscapes(x);
        }

        public boolean isTrue(Chunk context)
        {
            Object leftSideValue;

            switch (testType) {
            case EXISTENCE:
                if (leftSide == null) {
                    return !isNeg;
                }
                leftSideValue = context.resolveTagValue(leftSide, 1);
                return (leftSideValue == null) ? isNeg : !isNeg;
            case COMPARE_CONSTANT:
                leftSideValue = context.resolveTagValue(leftSide, 1);
                if (leftSideValue == null) {
                    return (compareTo == null) ? !isNeg : isNeg;
                }
                return leftSideValue.toString().equals(compareTo == null ? "" : compareTo) ? !isNeg : isNeg;
            case COMPARE_TAGEXPR:
                leftSideValue = context.resolveTagValue(leftSide, 1);
                Object rightSideValue = context.resolveTagValue(rightSide, 1);
                if (leftSideValue == null && rightSideValue == null) {
                    return !isNeg;
                }
                if (leftSideValue == null || rightSideValue == null) {
                    return isNeg;
                }
                String leftStr = leftSideValue.toString();
                String rightStr = rightSideValue.toString();
                return leftStr.equals(rightStr) ? !isNeg : isNeg;
            case COMPARE_REGEX:
                leftSideValue = context.resolveTagValue(leftSide, 1);
                String testStr = leftSideValue == null ? null : leftSideValue.toString();
                return isMatch(testStr, this.compareTo) ? !isNeg : isNeg;
            default:
                return false;
            }
        }

        private boolean isMatch(String text, String regex)
        {
            if (text == null || regex == null) return false;
            regex = regex.trim();

            Pattern p = compilePattern(regex);
            if (p == null) return false;
            Matcher m = p.matcher(text);
            return m.find();
        }

        private static Map<String,Pattern> compiledRegex = new HashMap<String,Pattern>();

        private Pattern compilePattern(String regex)
        {
            if (compiledRegex.containsKey(regex)) {
                return compiledRegex.get(regex);
            }

            int cursor = 0;
            if (regex.charAt(cursor) == 'm') cursor++;
            if (regex.charAt(cursor) == '/') cursor++;
            int regexEnd = RegexFilter.nextRegexDelim(regex,cursor);
            if (regexEnd < 0) return null; // fatal, unmatched regex boundary

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
            compiledRegex.put(regex, p);

            return p;
        }

    }
}
