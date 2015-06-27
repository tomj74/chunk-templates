package com.x5.template.filters;

import com.x5.template.Chunk;

public class CalcFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, FilterArgs args)
    {
        if (text == null) return null;
        if (args.getFilterArgs() == null) return text;

        return easyCalc(text, args);
    }

    public String getFilterName()
    {
        return "calc";
    }

    private static String easyCalc(String text, FilterArgs arg)
    {
        String[] args = arg.getFilterArgs();
        String expr = args[0];

        // optional -- format string; only possible when args are quoted
        String fmt = null;
        if (args.length > 1) {
            fmt = args[1];
        }

        if (expr.indexOf("x") < 0) expr = "x"+expr;
        expr = expr.replace("\\$","");
        try {
            return Calc.evalExpression(expr,fmt,new String[]{"x"},new String[]{text});
        } catch (NumberFormatException e) {
            // not a number?  no-op
            return text;
        } catch (NoClassDefFoundError e) {
            return "[ERROR: jeplite jar missing from classpath! calc filter requires jeplite library]";
        }
    }
}
