package com.x5.template.filters;

import com.x5.template.Chunk;

public class CalcFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, FilterArgs args)
    {
        if (text == null) return null;
        if (args.getUnresolvedArgs() == null) return text;

        return easyCalc(text, args, chunk);
    }

    public String getFilterName()
    {
        return "calc";
    }

    private static String easyCalc(String text, FilterArgs arg, Chunk context)
    {
        String[] args = arg.getUnresolvedArgs();
        String expr = args[0];

        String fmt = null;
        if (args.length > 1) {
            fmt = args[args.length-1];
        }

        if (expr.indexOf("$x") < 0) {
            expr = "$_input_" + expr;
        } else {
            expr = expr.replace("$x", "$_input_");
        }
        try {
            String[] varNames = Calc.parseVarNames(expr);
            String[] varValues = grokVarValues(context, varNames, text);
            String jepExpr = expr.replace('$', 'V');
            return Calc.evalExpression(jepExpr, fmt, varNames, varValues);
        } catch (NumberFormatException e) {
            // not a number?  no-op
            return text;
        } catch (NoClassDefFoundError e) {
            return "[ERROR: jeplite jar missing from classpath! calc filter requires jeplite library]";
        }
    }

    private static String[] grokVarValues(Chunk context, String[] varNames, String input)
    {
        String[] varValues = new String[varNames.length];
        for (int i=0; i<varNames.length; i++) {
            if (varNames[i].equals("V_input_")) {
                varValues[i] = input;
            } else {
                Object value = context.get(varNames[i].substring(1));
                if (value instanceof String) {
                    varValues[i] = (String)value;
                }
            }
        }

        return varValues;
    }
}
