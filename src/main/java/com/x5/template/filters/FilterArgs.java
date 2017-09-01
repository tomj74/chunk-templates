package com.x5.template.filters;

import java.util.StringTokenizer;

import com.x5.template.Chunk;
import com.x5.template.Filter;
import com.x5.template.TemplateSet;

public class FilterArgs
{
    private String rawInvocation;
    private String rawArgs;
    private String[] deepRefPath;

    private String filterName;
    private String[] filterArgs;

    public FilterArgs(String filterInvocation)
    {
        this.rawInvocation = filterInvocation;
        this.init();
    }

    public String getFilterName()
    {
        return this.filterName;
    }

    /**
     * Returns literal filter arguments from original template with no interpolation.
     * If you really want this, use {@link #getUnresolvedArgs()} instead.
     *
     * @deprecated use {@link #getFilterArgs(Chunk)} instead.
     */
    @Deprecated
    public String[] getFilterArgs()
    {
        return this.filterArgs;
    }

    /**
     * When getFilterArgs is provided with a context, backticks will be resolved
     * and args that look like tag values are resolved. The old method signature
     * is deprecated, please always call this version of getFilterArgs.
     *
     * @param context
     * @return resolved arguments as strings
     */
    public String[] getFilterArgs(Chunk context)
    {
        Object[] resolvedObjs = getArgsAsObjects(context);
        if (resolvedObjs == null) {
            return null;
        }

        String[] resolved = new String[resolvedObjs.length];
        for (int i=0; i<resolved.length; i++) {
            Object val = resolvedObjs[i];
            if (val != null) {
                resolved[i] = val.toString();
            }
        }

        return resolved;
    }

    /**
     * When passing tag values as filter args, you may want to manipulate the
     * original object within the filter logic, ie and not its string representation.
     *
     * eg: {$name|myFilter($x,$y.attr,$z)}
     *
     * @param context
     * @return context-resolved arguments as bare objects
     */
    public Object[] getArgsAsObjects(Chunk context) {
        if (filterArgs == null) {
            return null;
        }

        Object[] resolvedArgs = new Object[filterArgs.length];

        for (int i=0; i<resolvedArgs.length; i++) {
            String resolvedStr = Filter.resolveBackticks(context, filterArgs[i]);
            Object resolved = resolvedStr;
            if (resolvedStr.length() > 2) {
                char magicChar = resolvedStr.charAt(0);
                if (magicChar == '$' || magicChar == '~') {
                    resolved = context.get(resolvedStr.substring(1));
                }
            }
            resolvedArgs[i] = resolved;
        }

        return resolvedArgs;
    }

    public String getUnparsedFilter()
    {
        return this.rawInvocation;
    }

    public String getUnparsedArgs()
    {
        return this.rawArgs;
    }

    public String[] getUnresolvedArgs()
    {
        return this.filterArgs;
    }

    public String[] getDeepRefPath()
    {
        return this.deepRefPath;
    }

    private void init()
    {
        filterName = rawInvocation;

        int parenPos = rawInvocation.indexOf('(');
        int slashPos = rawInvocation.indexOf('/');
        if (slashPos > -1 && (parenPos < 0 || parenPos > slashPos)) {
            // regex special case
            filterName = rawInvocation.substring(0, slashPos);
            rawArgs = rawInvocation.substring(slashPos);
            filterArgs = new String[]{rawArgs};
        } else if (parenPos > -1) {
            // standard args(x,y,z) case
            filterName = rawInvocation.substring(0, parenPos);
            int closeParenPos = rawInvocation.lastIndexOf(")");
            if (closeParenPos > parenPos) {
                rawArgs = rawInvocation.substring(parenPos+1, closeParenPos);
                filterArgs = parseArgs(rawArgs);
                // check for deep ref after close-paren
                if (closeParenPos + 2 < rawInvocation.length()) {
                    int deepRefPos = rawInvocation.indexOf(".", closeParenPos+1);
                    deepRefPath = parseDeepRef(rawInvocation.substring(deepRefPos+1));
                }
            }
        }
    }

    private static String[] parseDeepRef(String deepRef)
    {
        StringTokenizer tokens = new StringTokenizer(deepRef, ".");
        int n = tokens.countTokens();
        String[] segments = new String[n];
        for (int i=0; i<n; i++) {
            segments[i] = tokens.nextToken();
        }
        return segments;
    }

    private static String[] parseArgs(String parenthetical)
    {
        return parseArgs(parenthetical, true);
    }

    private static String[] parseArgs(String filter, boolean splitOnComma)
    {
        int quote1 = filter.indexOf("\"");
        int quote2;
        boolean isQuoted = true;
        // quote must be preceded only by whitespace...
        if (quote1 < 0 || filter.substring(0,quote1).trim().length() > 0) {
            quote1 = -1;
            quote2 = filter.length();

            isQuoted = false;
        } else {
            quote2 = filter.indexOf("\"",quote1+1);
            if (quote2 < 0) quote2 = filter.length();
        }

        String arg0 = filter.substring(quote1+1,quote2);

        String arg1 = null;
        if (isQuoted) {
            int quote3 = filter.indexOf("\"",quote2+1);
            if (quote3 > 0) {
                int quote4 = filter.indexOf("\"",quote3+1);
                if (quote4 > 0) {
                    arg1 = filter.substring(quote3+1,quote4);
                }
            }
        }

        if (arg1 != null) {
            // some bizarre special case a la ("xyz","abc")
            // with a guarantee of no escaped double quotes
            // tracked this down, being used by calc("expr","%2f")
            // to pass optional 2nd format str arg.
            // probably (?) isn't breaking anything.
            return new String[]{arg0,arg1};
        } else if (isQuoted || !splitOnComma || arg0.indexOf(",") < 0) {
            return new String[]{arg0};
        } else {
            return parseCommaDelimitedArgs(arg0);
        }
    }

    private static String[] parseCommaDelimitedArgs(String argStr)
    {
        String[] args = new String[15];
        int argX = 0;

        int marker = 0;

        while (argX < args.length) {
            int commaPos = nextArgDelim(argStr,marker);
            if (commaPos < 0) break;

            int quotePos = nextUnescapedDelim("\"", argStr, marker);
            if (quotePos > -1 && quotePos < commaPos) {
                // arg must start with quote to be considered quoted
                if (argStr.substring(marker,quotePos).trim().length() == 0) {
                    int endQuotePos = nextUnescapedDelim("\"",argStr,quotePos+1);
                    if (endQuotePos > 0) {
                        String arg = argStr.substring(quotePos+1,endQuotePos);
                        args[argX] = arg;
                        argX++;
                        commaPos = nextArgDelim(argStr,endQuotePos+1);
                        if (commaPos > 0) {
                            marker = commaPos + 1;
                        } else {
                            marker = argStr.length();
                        }
                        continue;
                    }
                }
            }
            int regexPos = RegexFilter.nextRegexDelim(argStr, marker);
            if (regexPos > -1 && regexPos < commaPos) {
                // arg must start with / or m/ to be considered regex
                String regexOpen = argStr.substring(marker,regexPos).trim();
                if (regexOpen.length() == 0 || regexOpen.equals("m")) {
                    int endRegexPos = RegexFilter.nextRegexDelim(argStr, regexPos+1);
                    if (endRegexPos > 0) {
                        commaPos = nextArgDelim(argStr,endRegexPos+1);
                        int endArgPos = commaPos;
                        if (commaPos < 0) {
                            // no more args
                            endArgPos = argStr.length();
                            marker = endArgPos;
                        } else {
                            marker = commaPos+1;
                        }
                        String arg = argStr.substring(regexPos,endArgPos);
                        args[argX] = arg;
                        argX++;
                        continue;
                    }
                }
            }

            String arg = argStr.substring(marker,commaPos);
            args[argX] = arg;
            argX++;
            marker = commaPos+1;
            commaPos = nextArgDelim(argStr,marker);
        }
        if (argX == args.length) return args; // maxed out

        // got here? no more commas...
        int closeParenPos = nextUnescapedDelim(")",argStr,marker);
        int finalArgEnd = argStr.length();
        if (closeParenPos > 0) {
            finalArgEnd = closeParenPos;
        }
        String finalArg = argStr.substring(marker,finalArgEnd);
        args[argX] = finalArg;
        argX++;

        // check for nomatch(...) args
        if (argX+1 < args.length && closeParenPos > 0 && closeParenPos + 1 < argStr.length()) {
            int nextParen = argStr.indexOf('(',closeParenPos+1);
            if (nextParen > 0) {
                String appendixTag = argStr.substring(closeParenPos+1,nextParen);
                args[argX] = "|"+appendixTag+"|";
                argX++;
                int endPos = argStr.length();
                if (argStr.endsWith(")")) endPos--;
                String appendixArg = argStr.substring(nextParen+1,endPos);
                args[argX] = appendixArg;
                argX++;
            }
        }

        String[] truncated = new String[argX];
        System.arraycopy(args, 0, truncated, 0, argX);
        return truncated;
    }

    /**
     * magicBraces wraps unbraced tag-specials $tag ^command +include in {$braces}
     * to trigger proper re-processing later on.  magicBraces leaves unprefixed
     * values intact.
     */
    public static String magicBraces(Chunk context, String output)
    {
        return magicBraces(context, output, null);
    }

    public static String magicBraces(Chunk context, String output, FilterArgs args)
    {
        if (output == null || output.length() == 0) return output;

        char firstChar = output.charAt(0);
        if (firstChar == '~' || firstChar == '$') {
            String[] deepRef = (args == null) ? null : args.getDeepRefPath();
            if (deepRef != null) {
                for (int i=0; i<deepRef.length; i++) {
                    output += "." + deepRef[i];
                }
            }
            return context != null ? context.makeTag(output.substring(1)) : "{" + output + "}";
        } else if (firstChar == '^' || firstChar == '.') {
            if (context != null) {
                return context.makeTag('.' + output.substring(1));
            }
            // turn .cmd into {.cmd}
            return TemplateSet.PROTOCOL_SHORTHAND + output.substring(1) + TemplateSet.DEFAULT_TAG_END;
        } else if (firstChar == '+') {
            return "{" + output + "}";
        } else {
            return output;
        }
    }

    public static int nextArgDelim(String arglist, int searchFrom)
    {
        return nextUnescapedDelim(",",arglist,searchFrom);
    }

    public static int nextUnescapedDelim(String delim, String regex, int searchFrom)
    {
        int delimPos = regex.indexOf(delim, searchFrom);

        boolean isProvenDelimeter = false;
        while (!isProvenDelimeter) {
            // count number of backslashes that precede this forward slash
            int bsCount = 0;
            while (delimPos-(1+bsCount) >= searchFrom && regex.charAt(delimPos - (1+bsCount)) == '\\') {
                bsCount++;
            }
            // if odd number of backslashes precede this delimiter char, it's escaped
            // if even number precede, it's not escaped, it's the true delimiter
            // (because it's preceded by either no backslash or an escaped backslash)
            if (bsCount % 2 == 0) {
                isProvenDelimeter = true;
            } else {
                // keep looking for real delimiter
                delimPos = regex.indexOf(delim, delimPos+1);
                // if the regex is not legal (missing delimiters??), bail out
                if (delimPos < 0) return -1;
            }
        }
        return delimPos;
    }

    public static int grokValidColonScanPoint(String wholeTag, int startHere)
    {
        // presumably we are starting at the final filter.
        // so, we need to ignore colons that appear within function args
        // eg:
        // s/...:.../...:.../
        // sprintf(...:...)
        // ondefined(...:...)
        // onmatch(/:/,:,/:/,:)
        // onmatch(/:/,:,/:/,:)nomatch(:)
        // onmatch(/(asdf|as:df)/,:)
        if (wholeTag.charAt(startHere) == 's' && wholeTag.charAt(startHere+1) == '/') {
            int regexMid = RegexFilter.nextRegexDelim(wholeTag,startHere+2);
            int regexEnd = RegexFilter.nextRegexDelim(wholeTag,regexMid+1);
            return regexEnd+1;
        }
        // this assumes no whitespace in the filters
        if (wholeTag.length() > startHere+7 && wholeTag.substring(startHere,startHere+7).equals("onmatch")) {
            // tricky, you have to traverse each regex and consider the contents as blind spots (anything goes)
            boolean skippedArgs = false;
            startHere += 8;
            while (!skippedArgs) {
                int slashPos = wholeTag.indexOf("/",startHere);
                if (slashPos < 0) break;
                slashPos = RegexFilter.nextRegexDelim(wholeTag,slashPos+1);
                if (slashPos < 0) break;
                int commaPos = nextUnescapedDelim(",",wholeTag,slashPos+1);
                if (commaPos < 0) break;
                int moreArgs = nextUnescapedDelim(",",wholeTag,commaPos+1);
                if (moreArgs < 0) {
                    int closeParen = nextUnescapedDelim(")",wholeTag,commaPos+1);
                    if (closeParen < 0) break;
                    // else
                    if (wholeTag.length() > closeParen+8 && wholeTag.substring(closeParen+1,closeParen+8).equals("nomatch")) {
                        startHere = closeParen+1;
                        skippedArgs = true;
                        // drop out and continue on to exclude nomatch(...) args
                    } else {
                        // this is the end of the onmatch(/regex/,output,/regex/,output)
                        // there is no onmatch(...)nomatch(...) suffix
                        return closeParen+1;
                    }
                } else {
                    startHere = moreArgs+1;
                }
            }
        }

        // got here?  just one set of parens left to skip, maybe less!

        int openParen = wholeTag.indexOf("(",startHere);
        if (openParen < 0) return startHere;

        int closeParen = nextUnescapedDelim(")",wholeTag,openParen+1);
        if (closeParen < 0) return startHere;

        // if it has args and it's not an onmatch, then this close-paren is the end of the last filter
        return closeParen+1;
    }

}
