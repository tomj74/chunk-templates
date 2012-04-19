package com.x5.template.filters;

import org.cheffo.jeplite.JEP;
import org.cheffo.jeplite.ParseException;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.ArrayList;

/***
 * Calc supports an experimental tag filter and template function that allows
 * for arbitrary math to be performed at direct request of the
 * template.
 *
 * Examples:
 *
 * {~width|calc(*2)}
 * {^calc("sqrt($x^2 + $y^2)",~width,~height)} {!-- hypotenuse --}
 * {^calc("sin(pi/4)")|sprintf(%.02f)} {!-- sqrt(2)/2 I think? --}
 */

public class Calc
{
    public static void main(String[] args)
    {
        String expr = "2 + 2 * x";
        String[] varnames = new String[]{"x"};
        String[] varvalues = new String[]{"20"};
        System.out.println(evalExpression(expr,"0",varnames,varvalues));
        System.out.println(evalExpression("sin(pi)","0.00",null,null));

        expr = "(2 + 2 * x^x) % 2";
        System.out.println(evalExpression(expr,null,varnames,varvalues));

        java.util.HashMap<String,Object> map = new java.util.HashMap<String,Object>();
        map.put("a","20");
        map.put("b","30");

        String calcString = "\"$x * 3 + $y * 40\",\"%.2f\",~a,~b";
        System.out.println(evalCalc(calcString, map));
        
        calcString = "\"$x * $x\",\"%.2f\",~a,~b";
        System.out.println(evalCalc(calcString, map));
    }

    public static String evalCalc(String calc, Map<String,Object> vars)
    {
        int quote1 = calc.indexOf("\"");
        if (quote1 < 0) return null;
        int quote2 = calc.indexOf("\"",quote1+1);
        if (quote2 < 0) return null;

        String expr = calc.substring(quote1+1,quote2);

        int argStart = quote2+1;

        // optional -- format string
        String fmt = null;
        int quote3 = calc.indexOf("\"",quote2+1);
        if (quote3 > 0) {
            int quote4 = calc.indexOf("\"",quote3+1);
            if (quote4 > 0) {
                fmt = calc.substring(quote3+1,quote4);
                argStart = quote4+1;
            }
        }

        String[] varNames = parseVarNames(expr);
        String[] varValues = null;
        String jepExpr = expr;
        if (varNames != null) {
            jepExpr = jepExpr.replaceAll("\\$","V");

            int comma = calc.indexOf(",",argStart);
            if (comma > 0) {
                argStart = comma+1;
            }

            int closeParen = calc.indexOf(")",argStart);
            if (closeParen < 0) closeParen = calc.length();
            varValues = grokVarValues(calc.substring(argStart,closeParen),vars);
        }

        /*
        System.err.println(expr);
        System.err.println(fmt);
        if (varNames != null) {
            for (int i=0; i<varNames.length; i++) {
                System.err.println(varNames[i]+" = "+varValues[i]);
            }
        }*/

        try {
            return evalExpression(jepExpr, fmt, varNames, varValues);
        } catch (NumberFormatException e) {
            StringBuilder input = new StringBuilder();
            if (varValues != null) {
                for (int i=0; i<varValues.length; i++) {
                    if (i>0) input.append(",");
                    input.append(varValues[i]);
                }
            }
            return "[error evaluating expression - '"+expr+"' - input ("+input+") must be numeric]";
        }
    }

    private static String[] grokVarValues(String list, Map<String,Object> vars)
    {
        String[] tokens = list.split(",");
        if (tokens == null) return null;

        String[] values = new String[tokens.length];

        for (int i=0; i<tokens.length; i++) {
            // look up value in vars
            String key = tokens[i];
            key = key.trim();
            if (key.startsWith("~")) key = key.substring(1);
            //System.err.println("scanning map for "+key);
            Object value = vars.get(key);
            if (value instanceof String) {
            	values[i] = (String)value;
            }
        }
        return values;
    }

    private static String[] parseVarNames(String expr)
    {
        String[] varNames = null;
        ArrayList<String> names = null;

        int varMarker = expr.indexOf("$");

        while (varMarker > -1) {
            int endOfName = varMarker+1;
            // scan to end of name
            char c = expr.charAt(endOfName);
            while (isLegalNameChar(c) && ++endOfName < expr.length()) {
                c = expr.charAt(endOfName);
            }
            if (names == null) names = new ArrayList<String>();

            String name = "V"+expr.substring(varMarker+1,endOfName);
            if (!names.contains(name)) {
                names.add(name);
            }

            // scan for next variable reference
            varMarker = expr.indexOf("$",endOfName);
        }

        if (names == null || names.size() == 0) return null;
        varNames = new String[names.size()];
        return names.toArray(varNames);
    }

    private static boolean isLegalNameChar(char c)
    {
        if (c >= 'a' && c <= 'z') return true;
        if (c >= 'A' && c <= 'Z') return true;
        if (c == '_') return true;
        if (c >= '0' && c <= '9') return true;

        // otherwise...
        return false;
    }

    public static String evalExpression(String expr, String fmt, String[] varNames, String[] varValues)
    {
        JEP jep = new JEP();
        jep.addStandardConstants();
        jep.addStandardFunctions();

        if (varNames != null && varValues != null && varNames.length <= varValues.length) {
            for (int i=0; i<varNames.length; i++) {
                String var = varNames[i];
                if (var == null) continue;

                String val = varValues[i];
                double v = 0.0d;
                if (val != null) v = Double.parseDouble(val);

                jep.addVariable(var,v);
            }
        }

        // evaluate the expression and format the result
        try {

            jep.parseExpression(expr);
            double result = jep.getValue();

            if (fmt == null) return Double.toString(result);

            // format, bails w/no format if problematic
            try {
                if (fmt.startsWith("%")) {
                    return String.format(fmt, result);
                } else {
                    NumberFormat formatter = new DecimalFormat(fmt);
                    return formatter.format(result);
                }
            } catch (NumberFormatException e) {
                return Double.toString(result);
            } catch (java.util.IllegalFormatException e) {
                return Double.toString(result);
            }

        } catch (ParseException e) {
            e.printStackTrace(System.err);
            return e.getMessage();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return e.getMessage();
        }
    }
}
