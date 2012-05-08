package com.x5.template;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Map;

import com.x5.template.filters.BasicFilter;
import com.x5.template.filters.ChunkFilter;
import com.x5.template.filters.RegexFilter;

/* TextFilter provides a library of text filtering functions
   to support in-template presentation transformations. */

public class TextFilter
{
    public static String FILTER_FIRST = "FILTER_FIRST";
    public static String FILTER_LAST  = "FILTER_LAST";
    
    private static Map<String,ChunkFilter> filters = registerStockFilters();

    private static Map<String,ChunkFilter> registerStockFilters()
    {
        if (filters == null) {
            filters = BasicFilter.getStockFilters();
        }
        return filters;
    }
    
    public static String applyTextFilter(Chunk context, String filter, String text)
    {
        if (filter == null) return text;

        // filters might be daisy-chained
        int pipePos = findNextFilter(filter);
        if (pipePos >= 0) {
            String firstFilter = filter.substring(0,pipePos);
            String nextFilters = filter.substring(pipePos+1);
            text = applyTextFilter(context, firstFilter, text);
            return applyTextFilter(context, nextFilters, text);
        }

        /*
        if (text == null) {
            if (filter.startsWith("onmatch")) {
                // onmatch is special wrt null text
                // onmatch can transform null into an empty string
                // or even a nomatch string
            } else {
                return null;
            }
        }*/
        
        String filterName = filter;
        String[] filterArgs = null;
        
        int parenPos = filter.indexOf('(');
        int slashPos = filter.indexOf('/');
        if (slashPos > -1 && (parenPos < 0 || parenPos > slashPos)) {
            // regex special case
            filterName = filter.substring(0,slashPos);
            filterArgs = new String[]{filter.substring(slashPos)};
        } else if (parenPos > -1) {
            // standard args(x,y,z) case
            filterName = filter.substring(0,parenPos);
            filterArgs = parseArgs(filter.substring(parenPos+1));
        }
        
        // if custom filter is registered with this name, it takes precedence
        Map<String,ChunkFilter> customFilters = null;
        
        ChunkFactory userTheme = context.getChunkFactory();
        if (userTheme != null) customFilters = userTheme.getFilters();
        
        if (customFilters != null) {
            ChunkFilter userFilter = customFilters.get(filterName);
            if (userFilter != null) {
                try {
                    return userFilter.transformText(context, text, filterArgs);
                } catch (Exception e) {
                    // poorly behaved contrib code.  don't buy the farm, just
                    // complain to stderr and move along.
                    e.printStackTrace(System.err);
                    return text;
                }
            }
        }

        // provide a few basic filters without making a whole class for each one.
        
        if (filter.equals("trim")) {
            // trim leading and trailing whitespace
            return text == null ? null : text.trim(); //text.replaceAll("^\\s+","").replaceAll("\\s+$","");
        } else if (filter.equals("qs") || filter.equals("quoted") || filter.equals("quotedstring")) {
            // qs is a quoted string - escape " and ' with backslashes
            if (text != null) {
                text = Chunk.findAndReplace(text,"\"","\\\"");
                text = Chunk.findAndReplace(text,"'","\\'");
            }
            return text;
        } else if (filter.equals("uc")) {
            // uppercase
            return text == null ? null : text.toUpperCase();
        } else if (filter.equals("lc")) {
            // lowercase
            return text == null ? null : text.toLowerCase();
        }
        
        // try to find a matching factory-standard stock filter for this job.
        ChunkFilter stockFilter = filters.get(filterName);
        if (stockFilter == null) {
            return text;
        } else {
            return stockFilter.transformText(context, text, filterArgs);
        }
    }
    
    private static String[] parseArgs(String filter)
    {
        return parseArgs(filter, true);
    }
        
    private static String[] parseArgs(String filter, boolean splitOnComma)
    {
        int quote1 = filter.indexOf("\"");
        int quote2;
        boolean isQuoted = true;
        // quote must be preceded only by whitespace...
        if (quote1 < 0 || filter.substring(0,quote1).trim().length() > 0) {
            quote1 = -1;
            quote2 = filter.lastIndexOf(")");
            if (quote2 < 0) return null;
                
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
            return new String[]{filter,arg0,arg1};
        } else if (isQuoted || !splitOnComma || arg0.indexOf(",") < 0) {
            return new String[]{arg0};
        } else {
            return parseCommaDelimitedArgs(arg0);
        }
    }
    
    private static String[] parseCommaDelimitedArgs(String argStr)
    {
        String[] args = new String[15];
        // always pass pre-split args string in position 0
        args[0] = argStr;
        int argX = 1;
        
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

    public static String magicBraces(Chunk context, String output)
    {
        if (output == null || output.length() == 0) return output;
        
        char firstChar = output.charAt(0);
        if (firstChar == '~' || firstChar == '+') {
        	if (context == null || context.isConforming()) {
        		return "{"+output+"}";
        	} else {
        		String tagOpen = context.makeTag("XXX");
        		tagOpen = RegexFilter.applyRegex(tagOpen, "s/XXX.*//");
        		
        		String tag = context.makeTag(output);
        		
        		tag = Chunk.findAndReplace(tag, tagOpen+'~', tagOpen);
        		tag = Chunk.findAndReplace(tag, tagOpen+'+', TemplateSet.INCLUDE_SHORTHAND);
        		
        		return tag;
        	}
        } else if (firstChar == '^') {
        	if (context == null) {
        		// internally, {^xyz} is just an alias for {~.xyz}
        		return TemplateSet.DEFAULT_TAG_START+'.'+output.substring(1)+TemplateSet.DEFAULT_TAG_END;
        	} else {
        		return context.makeTag('.'+output.substring(1));
        	}
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


    public static String[] splitFilters(String filter)
    {
        int startOfNext = findNextFilter(filter);
        if (startOfNext < 0) {
            return new String[]{filter};
        }

        java.util.ArrayList<String> filterList = new java.util.ArrayList<String>();
        while (startOfNext >= 0) {
            filterList.add(filter.substring(0,startOfNext));
            filter = filter.substring(startOfNext+1);
            startOfNext = findNextFilter(filter);
        }
        filterList.add(filter);
        String[] filters = new String[filterList.size()];
        return filterList.toArray(filters);
    }

    private static int findNextFilter(String filter)
    {
        int pipePos = filter.indexOf('|');

        if (pipePos >= 0 && filter.startsWith("s/")) {
            // tricky case -- skip pipes that appear inside regular expressions
            int regexEnd = RegexFilter.nextRegexDelim(filter,2);
            if (regexEnd < 0) return pipePos;

            // ok, we have reached the middle delimeter, now find the the closer
            regexEnd = RegexFilter.nextRegexDelim(filter,regexEnd+1);
            if (regexEnd < 0) return pipePos;

            if (regexEnd < pipePos) {
                return pipePos;
            } else {
                return filter.indexOf("|",regexEnd+1);
            }
        } else if (pipePos >= 0 && filter.startsWith("onmatch")) {
            // also tricky, find regexes/args and skip pipes inside them
            // there might be several regex matches in one onmatch
            boolean skippedArgs = false;
            int cursor = 8;
            while (!skippedArgs) {
                int slashPos = filter.indexOf("/",cursor);
                if (slashPos < 0) break;
                slashPos = RegexFilter.nextRegexDelim(filter,slashPos+1);
                if (slashPos < 0) break;
                int commaPos = nextUnescapedDelim(",",filter,slashPos+1);
                if (commaPos < 0) break;
                int moreArgs = nextUnescapedDelim(",",filter,commaPos+1);
                if (moreArgs < 0) {
                    int closeParen = nextUnescapedDelim(")",filter,commaPos+1);
                    if (closeParen < 0) break;
                    // else
                    if (filter.length() > closeParen+8 && filter.substring(closeParen+1,closeParen+8).equals("nomatch")) {
                        cursor = closeParen+1;
                        skippedArgs = true;
                        // drop out and continue on to exclude nomatch(...) args
                    } else {
                        // this is the end of the onmatch(/regex/,output,/regex/,output)
                        // there is no onmatch(...)nomatch(...) suffix
                        pipePos = filter.indexOf("|",closeParen+1);
                        return pipePos;
                    }
                } else {
                    cursor = moreArgs+1;
                }
            }
            // reached here? find end of nomatch(...) clause
            int openParen = filter.indexOf("(",cursor);
            if (openParen > 0) {
                int closeParen = nextUnescapedDelim(")",filter,openParen+1);
                if (closeParen > 0) {
                    pipePos = filter.indexOf("|",closeParen+1);
                    return pipePos;
                }
            }
            // reached here? something unexpected happened
            pipePos = filter.indexOf("|",cursor);
            return pipePos;
        } else {
            return pipePos;
        }
    }

    /*
    public static boolean matches(String text, Pattern pattern)
    {
        // lamest syntax ever...
        Matcher m = pattern.matcher(text);
        return m.find();
    }

    public static boolean matches(String text, String pattern)
    {
        // lamest syntax ever...
        Matcher m = Pattern.compile(pattern).matcher(text);
        return m.find();
    }*/

    // this is really just /includeIf([!~].*).[^)]*$/
    // with some groupers to parse out the variable pieces
    private static final Pattern parsePattern =
        Pattern.compile("includeIf\\(([\\!\\~])(.*)\\)\\.?([^\\)]*)$");
    // this is really just /include.([!~].*)[^)]*$/
    // with some groupers to parse out the variable pieces
    private static final Pattern parsePatternAlt =
        Pattern.compile("include\\.\\(([\\!\\~])(.*)\\)([^\\)]*)$");

    public static String translateIncludeIf(String tag, String open, String close, Map<String,Object> tagTable)
    {
        // {~.includeIf(~asdf).tpl_name}
        // is equiv to {~asdf|ondefined(+tpl_name):}
        // ...and...
        // {~.includeIf(~asdf =~ /xyz/).tpl_name}
        // is equiv to {~asdf|onmatch(/xyz/,+tpl_name)}
        // ...and...
        // {~.includeIf(~asdf == xyz).tpl_name}
        // is equiv to {~asdf|onmatch(/^xyz$/,+tpl_name)}
        // ...and...
        // {~.includeIf(~asdf == ~xyz).tpl_name}
        // is equiv to {~xyz|onmatch(/^__value of ~asdf__$/,+tpl_name)}
        // ...so...
        // just translate and return

        Matcher parseMatcher = parsePattern.matcher(tag);

        if (!parseMatcher.find()) {
            // all is not lost, just yet --
            // will also accept include.(~xyz)asdf since this is how +(~xyz)asdf expands
            parseMatcher = parsePatternAlt.matcher(tag);
            if (!parseMatcher.find()) {
                // ok, now all is lost...
                return "[includeIf bad syntax: "+tag+"]";
            }
        }
        // group zero is the primary toplevel match.
        // paren'd matches start at 1 but only if you first call it with zero.
        parseMatcher.group(0);
        String negater = parseMatcher.group(1);
        String test = parseMatcher.group(2);
        String includeTemplate = parseMatcher.group(3);
        //if (includeTemplate.equals(".")) includeTemplate = parseMatcher.group(4);
        includeTemplate = includeTemplate.replaceAll("[\\|:].*$", "");
        
        if (test.indexOf('=') < 0 && test.indexOf("!~") < 0) {
            // simplest case: no comparison, just a non-null test
            if (negater.charAt(0) == '~') {
                return open + test + "|ondefined(+" + includeTemplate + "):" + close;
            } else {
                return open + test + "|ondefined():+" + includeTemplate + close;
            }
        }

        // now handle straight equality/inequality
        // (~asdf == ~xyz) and (~asdf != ~xyz)
        boolean isNeg = false;
        if (test.indexOf("==") > 0 || (isNeg = test.indexOf("!=") > 0)) {
            String[] parts = test.split("!=|==");
            if (parts.length == 2) {
                String tagA = parts[0].trim();
                String tagB = parts[1].trim();
                String xlation;
                if (tagB.charAt(0) == '~') {
                    // equality (or inequality) of two variables (tags)
                    // resolve right-hand tag reference to a string value
                    String tagValue = null;
                    
                    // might be a snippet...
                    Object tagValueObj = tagTable.get(tagA);
                    if (tagValueObj != null) tagValue = tagValueObj.toString();
                    
                    if (tagValue == null) tagValue = "";
                    if (isNeg) {
                        xlation = open + tagB.substring(1)
                            + "|onmatch(/^" + RegexFilter.escapeRegex(tagValue) + "$/,)nomatch(+"
                            + includeTemplate + ")" + close;
                    } else {
                        xlation = open + tagB.substring(1)
                            + "|onmatch(/^" + RegexFilter.escapeRegex(tagValue) + "$/,+"
                            + includeTemplate + ")nomatch()" + close;
                    }
                } else {
                    // equality (or inequality) of one variable (tag)
                    // compared to a constant string
                    String match = tagB;
                    // allow tagB to be quoted?  if so, strip quotes here
                    if (tagB.charAt(0) == '"' && tagB.charAt(match.length()-1) == '"') {
                    	match = tagB.substring(1, tagB.length()-1);
                    }
                    if (isNeg) {
                        // include the template if the value does not match
                        xlation = open + tagA + "|onmatch(/^" + RegexFilter.escapeRegex(match) + "$/,)nomatch(+"
                            + includeTemplate + ")" + close;
                    } else {
                        // include the template if the value matches
                        xlation = open + tagA + "|onmatch(/^" + RegexFilter.escapeRegex(match) + "$/,+"
                            + includeTemplate + ")nomatch()" + close;
                    }
                }
                return xlation;
            } else {
                return "[includeIf bad syntax: "+tag+"]";
            }
        }

        // handle pattern match
        String[] parts = test.split("=~");
        boolean neg = false;
        if (parts.length != 2) {
            parts = test.split("!~");
            neg = true;
            if (parts.length != 2) {
                return "[includeIf bad syntax: "+tag+"]";
            }
        }
        String var = parts[0].trim();
        String match = parts[1].trim();
        String xlation;
        if (neg) {
            xlation = open + var + "|onmatch(" + match + ",)nomatch(+"
                + includeTemplate + ")" + close;
        } else {
            xlation = open + var + "|onmatch(" + match + ",+"
                + includeTemplate + ")nomatch()" + close;
        }
        //        System.err.println(xlation);
        return xlation;
    }

    public static int grokFinalFilterPipe(String wholeTag, int startHere)
    {
        int cursor = startHere;
        String filter = wholeTag.substring(cursor+1);
        int startOfNext = findNextFilter(filter);
        while (startOfNext >= 0) {
            cursor++;
            cursor+= startOfNext;
            startOfNext = findNextFilter(filter.substring(startOfNext+1));
        }
        return cursor;
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
    
    public static String joinStringArray(String[] array, String joinFilter)
    {
    	if (array == null) return "";
    	if (array.length == 1) return array[0];
    	
    	String divider = null;
    	// the only arg is the divider
    	int parenPos = joinFilter.indexOf('(');
    	if (parenPos > 0) {
        	String[] args = parseArgs(joinFilter.substring(parenPos+1),false);
        	divider = args[0];
    	}
    	
    	StringBuilder x = new StringBuilder();
    	for (int i=0; i<array.length; i++) {
    		if (i>0 && divider != null) x.append(divider);
    		if (array[i] != null) x.append(array[i]);
    	}
    	return x.toString();
    }
    

}
