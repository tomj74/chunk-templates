package com.x5.template;

import java.util.Iterator;
import java.util.List;

public class CondTree
{
    private static final int TEST = -1;
    private static final int NOT = 0;
    private static final int AND = 1;
    private static final int OR = 2;

    private static final String OPEN_PAREN = "(";
    private static final String CLOSE_PAREN = ")";
    private static final String NEGATOR = "!";
    private static final String OP_AND = "&&";
    private static final String OP_OR = "||";

    private int operator = TEST;
    private Conditional test;
    private CondTree leftBranch;
    private CondTree rightBranch;

    public CondTree(int operator)
    {
        this.operator = operator;
    }

    public static CondTree buildBranch(Iterator<String> tokens) throws InvalidExpressionException
    {
        CondTree branch = new CondTree(TEST);
        if (!tokens.hasNext()) {
            branch.test = new Conditional("");
            return branch;
        }

        while (tokens.hasNext()) {
            String token = tokens.next();

            boolean readyForInfix = branch.rightBranch != null
                || ((branch.operator == TEST || branch.operator == NOT) && (branch.test != null || branch.leftBranch != null));

            if (readyForInfix) {
                // everything up to this point is a valid expression.
                // if there is more to parse, an infix operator (AND/OR) is expected.
                if (token.equals(OPEN_PAREN) || token.equals(NEGATOR)) {
                    // expr expr w/no conjunction is not valid
                    // expr !expr w/no conjunction is not valid
                    throw new InvalidExpressionException("Missing infix operator between expressions");
                }
                if (token.equals(OP_OR)) {
                    // push this leaf branch down and make this OR container the new root.
                    CondTree orNode = new CondTree(OR);
                    orNode.leftBranch = branch;
                    if (branch.rightBranch != null) {
                        // descend to the right
                        orNode.rightBranch = CondTree.buildBranch(tokens);
                    }
                    branch = orNode;
                    continue;
                }
                if (token.equals(OP_AND)) {
                    if (branch.operator == OR) {
                        boolean isNegated = false;
                        do {
                            if (!tokens.hasNext()) {
                                throw new InvalidExpressionException("AND operator missing right-hand-side expression");
                            }
                            token = tokens.next();
                            if (token.equals(NEGATOR)) {
                                isNegated = !isNegated;
                            }
                        } while (token.equals(NEGATOR));
                        // order of operations: a || b && c  ==>  a || (b && c)
                        CondTree childBranch = new CondTree(AND);
                        childBranch.leftBranch = branch.rightBranch;
                        branch.rightBranch = childBranch;
                        if (token.equals(OPEN_PAREN)) {
                            childBranch.rightBranch = CondTree.buildBranch(tokens);
                            continue;
                        }
                        if (token.equals(CLOSE_PAREN)) {
                            throw new InvalidExpressionException("AND operator may not be followed by close-paren");
                        }
                        childBranch.rightBranch = new CondTree(isNegated ? NOT : TEST);
                        childBranch.rightBranch.test = new Conditional(token);
                        continue;
                    }
                    if (branch.operator == TEST && branch.test == null) {
                        // simple expr, can be made compound without pushing down.
                        branch.operator = AND;
                        continue;
                    }
                    // compound expression, must push down and create new root AND node.
                    CondTree andNode = new CondTree(AND);
                    andNode.leftBranch = branch;
                    branch = andNode;
                    continue;
                }
            }

            // test != null is handled above, so from here down test == null
            // ie, we're just starting and we have not parsed any expressions yet.
            if ((branch.operator == TEST || branch.operator == NOT) && branch.leftBranch == null) {
                // expression may not begin with infix operators OR/AND
                if (token.equals(OP_OR) || token.equals(OP_AND)) {
                    throw new InvalidExpressionException("Infix operator missing left-hand-side expression");
                }
                if (token.equals(OPEN_PAREN)) {
                    branch.leftBranch = CondTree.buildBranch(tokens);
                    continue;
                }
                if (token.equals(CLOSE_PAREN)) {
                    return branch;
                }
                if (token.equals(NEGATOR)) {
                    branch.operator = (branch.operator == TEST) ? NOT : TEST;
                    continue;
                }
                branch.test = new Conditional(token);
                continue;
            }

            if (branch.leftBranch != null && branch.rightBranch == null) {
                // seeking RHS of OR/AND ...
                boolean isNegated = false;
                while (token.equals(NEGATOR)) {
                    isNegated = !isNegated;
                    if (!tokens.hasNext()) {
                        throw new InvalidExpressionException("Infix operator missing right-hand-side expression");
                    }
                    token = tokens.next();
                }
                if (token.equals(OP_OR) || token.equals(OP_AND) || token.equals(CLOSE_PAREN)) {
                    // not valid: "&& || ..." "&& && ..." "&& )"
                    throw new InvalidExpressionException("Infix operators OR/AND must be followed by expression");
                }
                if (token.equals(OPEN_PAREN)) {
                    CondTree groupedExpr = CondTree.buildBranch(tokens);
                    if (isNegated) {
                        branch.rightBranch = new CondTree(NOT);
                        branch.rightBranch.leftBranch = groupedExpr;
                    } else {
                        branch.rightBranch = groupedExpr;
                    }
                    continue;
                }
                CondTree childNode = new CondTree(isNegated ? NOT : TEST);
                childNode.test = new Conditional(token);
                branch.rightBranch = childNode;
                continue;
            }

            if (token.equals(CLOSE_PAREN)) {
                return branch;
            }
        }

        return branch;
    }

    public String toString()
    {
        String prefix = (operator == NOT) ? "!" : "";
        if (operator == TEST || operator == NOT) {
            if (leftBranch != null) {
                return prefix + "(" + leftBranch.toString() + ")";
            }
            return prefix + (test == null ? "NULL" : test.toString());
        }

        return "(" + leftBranch.toString() + ") " + (operator == AND ? "AND" : "OR")
            + " (" + (rightBranch == null ? "..." : rightBranch.toString()) + ")";
    }

    public boolean isTrue(Chunk context)
    {
        if (operator == TEST) {
            return leftBranch != null ? leftBranch.isTrue(context) : test.isTrue(context);
        }

        if (operator == NOT) {
            return leftBranch != null ? !leftBranch.isTrue(context) : !test.isTrue(context);
        }

        if (operator == AND) {
            // only eval right branch if left branch is true.
            if (!leftBranch.isTrue(context)) {
                return false;
            }

            return rightBranch.isTrue(context);
        }

        if (operator == OR) {
            // only eval right branch if left branch is false.
            if (leftBranch.isTrue(context)) {
                return true;
            }
            return rightBranch.isTrue(context);
        }

        return false;
    }
}
