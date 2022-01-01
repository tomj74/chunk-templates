package com.x5.template;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.filters.FilterArgs;

public class CondLexer
{
    String conditional;
    CondTree parsed;

    public CondLexer(String conditional)
    {
        this.conditional = conditional;
    }

    public CondTree parse() throws InvalidExpressionException
    {
        if (parsed != null) {
            return parsed;
        }

        parsed = CondTree.buildBranch(lexLogicTokens());

        return parsed;
    }

    // pick apart high-level logic tokens && || ! () but treat entire
    // comparison tests as single token.
    private Iterator<String> lexLogicTokens()
    {
        char c, d;
        int len = conditional.length();
        List<String> tokens = new ArrayList<String>();

        for (int i=0; i<len; i++) {
            c = conditional.charAt(i);
            while (Character.isWhitespace(c)) {
                i++;
                if (i == len) {
                    return tokens.iterator();
                }
                c = conditional.charAt(i);
            }
            if (c == '(' || c == ')' || c == '!') {
                tokens.add(conditional.substring(i, i+1));
                continue;
            }
            d = i+1 < len ? conditional.charAt(i+1) : 0;
            if ((c == '&' && d == '&') || (c == '|' && d == '|')) {
                tokens.add(conditional.substring(i, i+2));
                i++;
                continue;
            }
            // not a logic token, must be the start of a comparison/test.
            int j = endOfComparison(conditional, i);
            tokens.add(conditional.substring(i,j));
            i = j-1;
        }

        return tokens.iterator();
    }

    // ) or && or || or [[:whitespace:]]
    private static Pattern END_OF_UNQUOTED_STRING = Pattern.compile("\\)|&&|\\|\\||\\s");

    // seek to end of comparison
    private int endOfComparison(String s, int start) {
        // POSSIBLE TESTS
        // $tag-expr
        // $tag-expr == (or !=) "constant"
        // $tag-expr == (or !=) 'constant'
        // $tag-expr == (or !=) unquoted_const
        // $tag-expr == (or !=) $tag-expr
        // $tag-expr =~ (or !~) /regex/
        char c, d;
        int i = start;
        int exprCount = 0;
        boolean foundOperator = false;
        int len = s.length();
        // 1. find LHS tag-expr
        // 2. seek comparison operator
        // 3. find RHS expr
        while (true) {
            if (i == len) {
                return len;
            }
            c = s.charAt(i);
            while (Character.isWhitespace(c)) {
                i++;
                if (i == len) {
                    return len;
                }
                c = s.charAt(i);
            }
            if (c == '$' || c == '~') {
                i = Conditional.skipModifiers(s, i+1);
                exprCount++;
                if (exprCount == 2) {
                    return i;
                }
                continue;
            }
            if (exprCount > 0 && (c == '&' || c == '|' || c == ')')) {
                return i;
            }
            if (exprCount > 0 && (c == '=' || c == '!')) {
                if (i+1 == len) {
                    return len;
                }
                d = s.charAt(i+1);
                if (d == '=' || d == '~') {
                    foundOperator = true;
                    i += 2;
                    continue;
                }
            }
            if (foundOperator) {
                // seeking RHS, ruled out tag, must be string constant or regex.
                if (c == '"' || c == '\'' || c == '/') {
                    String delim = Character.toString(c);
                    i = FilterArgs.nextUnescapedDelim(delim, s, i+1);
                    return (i > 0) ? i+1 : len;
                }
                // unquoted string :( assume next space/close-paren/&&/|| marks the end
                Matcher matcher = END_OF_UNQUOTED_STRING.matcher(s);
                if (matcher.find(i)) {
                    return matcher.start();
                } else {
                    return len;
                }
            }
            // falling through to here is okay... eg "if (true)" is permitted.
            // advance cursor to avoid infinite loop.
            i++;
        }
    }
}
