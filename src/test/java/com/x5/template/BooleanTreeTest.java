package com.x5.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class BooleanTreeTest
{
    private String input;
    private String expected;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"a", "a"},
            {"a && b", "(a) AND (b)"},
            {"a || b", "(a) OR (b)"},
            {"a && b && c", "((a) AND (b)) AND (c)"},
            {"a || b || c", "((a) OR (b)) OR (c)"},
            {"a && b || c", "((a) AND (b)) OR (c)"},
            {"a || b && c", "(a) OR ((b) AND (c))"},
            {"( a )", "(a)"},
            {"( a || b )", "((a) OR (b))"},
            {"( a && b )", "((a) AND (b))"},
            {"a && ( b || c )", "(a) AND ((b) OR (c))"},
            {"( a || b ) && c", "((a) OR (b)) AND (c)"},
            {"a || b && c || d", "((a) OR ((b) AND (c))) OR (d)"},
            {"( a || b ) && ( c || d )", "((a) OR (b)) AND ((c) OR (d))"},
            {"a || ( b && c ) || d", "((a) OR ((b) AND (c))) OR (d)"},
            {"a && b || c && d", "((a) AND (b)) OR ((c) AND (d))"},
            {"a && b || c && d || e", "((a) AND (b)) OR (((c) AND (d)) OR (e))"},
            {"a || b && c || d && e", "((a) OR ((b) AND (c))) OR ((d) AND (e))"},
            {"! a", "!a"},
            {"! a && b", "(!a) AND (b)"},
            {"a && ! b", "(a) AND (!b)"},
            {"! a || b", "(!a) OR (b)"},
            {"a || ! b", "(a) OR (!b)"},
            {"! ( a && b )", "!((a) AND (b))"},
            {"! ( a || b )", "!((a) OR (b))"},
            {"a || b && ! c", "(a) OR ((b) AND (!c))"},
        });
    }

    @SuppressWarnings("unused")
    public BooleanTreeTest(String input, String expected)
    {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void test() {
        try {
            ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(input.split(" ")));
            CondTree tree = CondTree.buildBranch(tokens.iterator());
            assertEquals(expected, tree.toString());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail(e.getMessage());
        }
    }
}
