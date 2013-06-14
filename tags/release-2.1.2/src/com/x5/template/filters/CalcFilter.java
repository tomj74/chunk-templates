package com.x5.template.filters;

import com.x5.template.Chunk;

public class CalcFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        if (text == null) return null;
        if (args == null) return text;
        
        return easyCalc(text, args);
    }

    public String getFilterName()
    {
        return "calc";
    }

    private static String easyCalc(String text, String[] args)
    {
        String expr = args.length > 1 ? args[1] : args[0];

        // optional -- format string; only possible when args are quoted
        String fmt = null;
        if (args.length > 2) {
            fmt = args[2];
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
