package com.x5.template.filters;

import com.x5.template.Chunk;

/**
 * QuickCalcFilter provides the qcalc filter, a very cheap, lightweight
 * way to do a single arithetic operation on a numeric value.
 *
 * Examples:
 *  {~x|qcalc(*2)} {~y|qcalc(+30)}
 *
 * @author tmcclure
 *
 */
public class QuickCalcFilter extends BasicFilter implements ChunkFilter
{
    public String transformText(Chunk chunk, String text, String[] args)
    {
        String calc = null;
        if (args != null && args.length > 0) calc = args[0];

        return applyQuickCalc(text,calc);
    }

    public String getFilterName()
    {
        return "qcalc";
    }

    private static String applyQuickCalc(String text, String calc)
    {
        if (text == null) return null;
        if (calc == null) return text;

        // strip calling wrapper ie "calc(-30)" -> "-30"
        /*
        if (calc.startsWith("qcalc(")) {
            calc = calc.substring(6);
            if (calc.endsWith(")")) {
                calc = calc.substring(0,calc.length()-1);
            }*/

        try {
            if (text.indexOf(".") > 0 || calc.indexOf(".") > 0) {
                double x = Double.parseDouble(text);
                char op = calc.charAt(0);
                double y = Double.parseDouble(calc.substring(1));

                //System.err.println("float-op: "+op+" args: "+x+","+y);

                double z = x;
                if (op == '-') z = x - y;
                if (op == '+') z = x + y;
                if (op == '*') z = x * y;
                if (op == '/') z = x / y;
                if (op == '%') z = x % y;
                return Double.toString(z);

            } else {
                long x = Long.parseLong(text);
                char op = calc.charAt(0);
                long y = Long.parseLong(calc.substring(1));

                //System.err.println("int-op: "+op+" args: "+x+","+y);

                long z = x;
                if (op == '-') z = x - y;
                if (op == '+') z = x + y;
                if (op == '*') z = x * y;
                if (op == '/') z = x / y;
                if (op == '%') z = x % y;
                if (op == '^') z = Math.round( Math.pow(x,y) );
                return Long.toString(z);
            }
        } catch (NumberFormatException e) {
            return text;
        }
    }
}
