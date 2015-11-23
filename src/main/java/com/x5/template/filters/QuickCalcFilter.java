package com.x5.template.filters;

import com.x5.template.Chunk;

/**
 * QuickCalcFilter provides the qcalc filter, a very cheap, lightweight
 * way to do a single arithmetic operation on a numeric value.
 *
 * Examples:
 *  {$x|qcalc(*2)} {$y|qcalc(+30)}
 *  {% if $x|comp(>20) %}...{% endif %}
 *
 * @author tmcclure
 *
 */
public class QuickCalcFilter extends BasicFilter implements ChunkFilter
{
    public String transformText(Chunk chunk, String text, FilterArgs args)
    {
        String[] parsedArgs = args.getFilterArgs();
        if (parsedArgs == null) return text;

        String calc = parsedArgs[0];
        if (calc == null) return text;

        return applyQuickCalc(text, calc);
    }

    public String getFilterName()
    {
        return "qcalc";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"qc","comp"};
    }

    private static String applyQuickCalc(String text, String calc)
    {
        if (text == null) return null;
        if (calc == null || calc.trim().length() < 2) return text;
        calc = calc.trim();

        try {
            if (text.indexOf(".") > 0 || calc.indexOf(".") > 0) {
                double x = Double.parseDouble(text);
                char op = calc.charAt(0);
                char op2 = calc.charAt(1);

                // handle comparisons
                if ((op == '!' && op2 == '=') || (op == '<' && op2 == '>')) {
                    double y = Double.parseDouble(calc.substring(2));
                    return (x == y) ? null : "TRUE";
                }
                if (op == '>' || op == '<' || op == '=') {
                    boolean orEq = (op2 == '=');
                    double y = Double.parseDouble(calc.substring(orEq ? 2 : 1));
                    if (orEq && x == y) return "TRUE";
                    if (op == '=') return null;
                    if (x > y) {
                        return (op == '>') ? "TRUE" : null;
                    } else {
                        return (op == '<') ? "TRUE" : null;
                    }
                }

                double y = Double.parseDouble(calc.substring(1));

                double z = x;
                if (op == '-') z = x - y;
                if (op == '+') z = x + y;
                if (op == '*') z = x * y;
                if (op == '/') z = x / y;
                if (op == '%') z = x % y;
                if (op == '^') z = Math.pow(x, y);
                return Double.toString(z);

            } else {
                long x = Long.parseLong(text);
                char op = calc.charAt(0);
                char op2 = calc.charAt(1);

                // handle comparisons
                if ((op == '!' && op2 == '=') || (op == '<' && op2 == '>')) {
                    long y = Long.parseLong(calc.substring(2));
                    return (x == y) ? null : "TRUE";
                }
                if (op == '>' || op == '<' || op == '=') {
                    boolean orEq = (op2 == '=');
                    long y = Long.parseLong(calc.substring(orEq ? 2 : 1));
                    if (orEq && x == y) return "TRUE";
                    if (op == '=') return null;
                    if (x > y) {
                        return (op == '>') ? "TRUE" : null;
                    } else {
                        return (op == '<') ? "TRUE" : null;
                    }
                }

                long y = Long.parseLong(calc.substring(1));

                long z = x;
                if (op == '-') z = x - y;
                if (op == '+') z = x + y;
                if (op == '*') z = x * y;
                if (op == '/') z = x / y;
                if (op == '%') z = x % y;
                if (op == '^') {
                    if (y < 0) {
                        // negative exponents will not result in integers
                        return Double.toString(Math.pow(x, y));
                    } else {
                        z = Math.round(Math.pow(x, y));
                    }
                }
                return Long.toString(z);
            }
        } catch (NumberFormatException e) {
            return text;
        }
    }
}
