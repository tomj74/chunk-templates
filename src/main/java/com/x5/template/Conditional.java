package com.x5.template;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.filters.FilterArgs;
import com.x5.template.filters.RegexFilter;

public class Conditional
{
    private static final int EXISTENCE = 0;
    private static final int COMPARE_CONSTANT = 1;
    private static final int COMPARE_REGEX = 2;
    private static final int COMPARE_TAGEXPR = 3;

    private String rawTest;
    private int testType = EXISTENCE;
    private boolean isNeg = false;

    // when comparing against a constant, this is the constant
    // when comparing against a regex, this is the regex
    private String compareTo = null;

    // if left side or right side are tag expressions, pre-init them and store here
    private SnippetTag leftSide = null;
    private SnippetTag rightSide = null;

    public Conditional(String test)
    {
        rawTest = test;
        init(test);
    }

    public String toString()
    {
        return rawTest;
    }

    private void init(String test)
    {
        if (test == null || test.trim().length() == 0) {
            this.testType = EXISTENCE;
            this.isNeg = true;
            return;
        }

        test = test.trim();

        char firstChar = test.charAt(0);
        if (firstChar == '$' || firstChar == '!' || firstChar == '~') {
            test = test.substring(1);
            if (test.length() == 0) {
                this.testType = EXISTENCE;
                this.isNeg = true;
                return;
            }
        }
        // eat one more in the !~tag case
        if (firstChar == '!' && (test.charAt(0) == '$' || test.charAt(0) == '~')) {
            test = test.substring(1);
        }

        // find the end of the LHS
        int scanStart = skipModifiers(test, 0);

        if (test.indexOf('=', scanStart) < 0 && test.indexOf("!~", scanStart) < 0) {
            this.testType = EXISTENCE;
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
                this.testType = COMPARE_TAGEXPR;
            } else {
                this.testType = COMPARE_CONSTANT;
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
        this.testType = COMPARE_REGEX;
        this.compareTo = regex;
    }

    public static int skipModifiers(String test, int start)
    {
        char[] chars = test.toCharArray();
        int i = start;
        for (; i<chars.length; i++) {
            char c = chars[i];
            if (Character.isJavaIdentifierPart(c)) continue;
            if (c == '|' || c == ':' || c == '.') continue;
            if (c == '(') {
                i = FilterArgs.nextUnescapedDelim(")", test, i+1);
                if (i < 0) {
                    // unmatched paren!
                    return chars.length;
                }
                continue;
            }
            if (c == '/') {
                i = RegexFilter.nextRegexDelim(test, i+1);
                if (i < 0) {
                    // unmatched regex delim!
                    return chars.length;
                }
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
