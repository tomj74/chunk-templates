package com.x5.template;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.x5.template.filters.ChunkFilter;
import com.x5.template.filters.BasicFilter;
import com.x5.template.filters.FilterArgs;
import com.x5.template.filters.RegexFilter;
import com.x5.util.TableData;

/* TextFilter provides a library of text filtering functions
   to support in-template presentation transformations. */

public class Filter
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

    private FilterArgs filterArgs;
    private ChunkFilter filter;
    private boolean isSafe = false;

    public static Object applyFilter(Chunk context, String filter, Object input)
    {
        if (filter == null) return input;

        return applyFilter(context, parseFilterChain(filter), input);
    }

    public static Object applyFilter(Chunk context, Filter[] filters, Object input)
    {
        if (filters == null) return input;

        Object filtered = input;
        for (int i=0; i<filters.length; i++) {
            filtered = filters[i].apply(context, filtered);
        }

        return filtered;
    }

    public static Filter[] parseFilterChain(String filter)
    {
        if (filter == null) return null;

        String[] filters = splitFilters(filter);
        Filter[] parsedFilters = new Filter[filters.length];
        for (int i=0; i<filters.length; i++) {
            parsedFilters[i] = new Filter(filters[i]);
        }

        return parsedFilters;
    }

    public Filter(String filter)
    {
        this.filterArgs = new FilterArgs(filter);
    }

    public Object apply(Chunk context, Object input)
    {
        if (filter != null) {
            if (isSafe) {
                return filter.applyFilter(context, input, filterArgs);
            }
            try {
                return filter.applyFilter(context, input, filterArgs);
            } catch (Exception e) {
                // poorly behaved contrib code.  don't buy the farm, just
                // complain to stderr and move along.
                e.printStackTrace(System.err);
                return input;
            }
        }

        String filterName = filterArgs.getFilterName();

        // if custom filter is registered with this name, it takes precedence
        Map<String,ChunkFilter> customFilters = null;

        ChunkFactory userTheme = context.getChunkFactory();
        if (userTheme != null) customFilters = userTheme.getFilters();

        if (customFilters != null) {
            ChunkFilter userFilter = customFilters.get(filterName);
            if (userFilter != null) {
                filter = userFilter;
                return apply(context, input);
            }
        }

        String rawFilter = filterArgs.getUnparsedFilter();
        if (rawFilter.equals("type")) {
            return typeFilter(context, input);
        }

        String text = inputAsText(input);
        if (text != null) {
            // provide a few core filters without making a whole class for each one.
            if (rawFilter.equals("trim")) {
                // trim leading and trailing whitespace
                return text.trim(); //text.replaceAll("^\\s+","").replaceAll("\\s+$","");
            } else if (rawFilter.startsWith("join(")) {
                TableData array = InlineTable.parseTable(text);
                if (array != null) {
                    return joinInlineTable(array, filterArgs);
                }
            } else if (rawFilter.startsWith("get(")) {
                TableData array = InlineTable.parseTable(text);
                if (array != null) {
                    return accessArrayIndex(array, filterArgs);
                }
            }
        }

        // try to find a matching factory-standard stock filter for this job.
        ChunkFilter stockFilter = filters.get(filterName);
        if (stockFilter != null) {
            filter = stockFilter;
            isSafe = true;
            return apply(context, input);
        } else {
            return input;
        }
    }

    private String inputAsText(Object input)
    {
        if (input instanceof String) {
            return (String)input;
        }
        if (input instanceof Snippet) {
            return ((Snippet)input).toSimpleString();
        }

        return null;
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
                int commaPos = FilterArgs.nextUnescapedDelim(",",filter,slashPos+1);
                if (commaPos < 0) break;
                int moreArgs = FilterArgs.nextUnescapedDelim(",",filter,commaPos+1);
                if (moreArgs < 0) {
                    int closeParen = FilterArgs.nextUnescapedDelim(")",filter,commaPos+1);
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
                int closeParen = FilterArgs.nextUnescapedDelim(")",filter,openParen+1);
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

    public static String accessArrayIndex(TableData table, FilterArgs getFilter)
    {
        return accessArrayIndex(extractListFromTable(table), getFilter);
    }

    public static String accessArrayIndex(String[] array, FilterArgs getFilter)
    {
        if (array == null) return "";
        return accessArrayIndex(Arrays.asList(array), getFilter);
    }

    public static String accessArrayIndex(List<String> list, FilterArgs getFilter)
    {
        if (list == null) return "";
        String[] args = getFilter.getFilterArgs();
        if (args != null) {
            String idx = args[0];
            try {
                int x = Integer.parseInt(idx);
                if (x < 0) {
                    // eg, -1 returns the last item in the array
                    // -2 return the 2nd-to-last, etc.
                    x = list.size() + x;
                }
                if (x >= 0 && x < list.size()) {
                    return list.get(x);
                }
            } catch (NumberFormatException e) {
            }
        }
        return "";
    }

    private static List<String> extractListFromTable(TableData table)
    {
        if (table == null) return null;

        // create a string array from the first column of the table.
        List<String> list = new ArrayList<String>();

        while (table.hasNext()) {
            table.nextRecord();
            String[] record = table.getRow();
            list.add(record[0]);
        }

        return list;
    }

    public static String joinInlineTable(TableData table, FilterArgs joinFilter)
    {
        return joinStringList(extractListFromTable(table), joinFilter);
    }

    public static String joinStringArray(String[] array, FilterArgs joinFilter)
    {
        if (array == null) return "";
        if (array.length == 1) return array[0];

        return joinStringList(Arrays.asList(array), joinFilter);
    }

    public static String joinStringList(List<String> list, FilterArgs joinFilter)
    {
        if (list == null) return "";
        if (list.size() == 1) return list.get(0);

        // to do: handle quoted divider case
        String divider = joinFilter.getUnparsedArgs();

        StringBuilder x = new StringBuilder();
        int i = 0;
        for (String s : list) {
            if (i>0 && divider != null) x.append(divider);
            if (s != null) x.append(s);
            i++;
        }
        return x.toString();
    }

    public static String typeFilter(Chunk context, Object tagValue)
    {
        return _typeFilter(context, tagValue, 0);
    }

    private static String _typeFilter(Chunk context, Object tagValue, int depth)
    {
        if (depth > 7) {
            return "CIRCULAR_POINTER";
        }

        if (tagValue == null) {
            return "NULL";
        } else if (tagValue instanceof String) {
            // make sure it's not an inline table
            if (isInlineTable((String)tagValue)) {
                return "LIST";
            } else {
                return "STRING";
            }
        } else if (tagValue instanceof Snippet) {
            if (isInlineTable(tagValue.toString())) {
                return "LIST";
            } else {
                Snippet snippet = (Snippet)tagValue;
                if (snippet.isSimplePointer()) {
                    String tagRef = snippet.getPointer();
                    // recurse (but, not forever)
                    return _typeFilter(context, context.get(tagRef), depth+1);
                } else {
                    return "STRING";
                }
            }
        } else if (tagValue instanceof Chunk) {
            // Chunk is a Map, but not really, but it's not just a string either
            // so let's intercept it here and give it its own label
            return "CHUNK";
        } else if (tagValue instanceof String[] || tagValue instanceof List
                || tagValue instanceof Object[] || tagValue instanceof com.x5.util.TableData) {
            return "LIST";
        } else if (tagValue instanceof Map || tagValue instanceof com.x5.util.DataCapsule) {
            return "OBJECT";
        }

        return "UNKNOWN";
    }

    private static boolean isInlineTable(String value)
    {
        // make sure it's not an inline table
        TableData inlineTable = InlineTable.parseTable(value);
        if (inlineTable != null) {
            return true;
        } else {
            return false;
        }
    }

}
