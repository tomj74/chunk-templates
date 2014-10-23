package com.x5.template;

import java.io.IOException;
import java.io.Writer;
import java.util.StringTokenizer;

import com.x5.template.filters.RegexFilter;

public class SnippetTag extends SnippetPart
{
    protected String tag;

    private String[] path;
    private String filters;
    private String ifNull;
    private boolean applyFiltersIfNull = false;

    public SnippetTag(String text, String tag)
    {
        super(text);
        this.tag = tag;
    }

    public boolean isTag()
    {
        return true;
    }

    public String getTag()
    {
        return this.tag;
    }

    public void render(Writer out, Chunk rules, int depth)
    throws IOException
    {
        if (depthCheckFails(depth,out)) return;

        Object tagValue = null;
        if (path == null) init(tag);

        tagValue = rules.resolveTagValue(this, depth);

        if (tagValue == null) {
            // preserve tag in final output (can be used as template)
            out.append(super.snippetText);
        } else if (tagValue instanceof Snippet) {
            // needs additional processing
            ((Snippet)tagValue).render(out, rules, depth);
        } else if (tagValue instanceof String) {
            Snippet compiled = Snippet.getSnippet((String)tagValue);
            compiled.render(out,rules,depth+1);
        } else {
            rules.explodeToPrinter(out, tagValue, depth+1);
        }
    }

    private void init(String tag)
    {
        String lookupName = tag;

        //strip off the default if provided eg {$tagName:333} means use 333
        // if no specific value is provided.
        //strip filters as well eg {$tagName|s/xx/yy/}
        int colonPos = tag.indexOf(':');
        int pipePos = tag.indexOf('|');
        if (pipePos > -1) pipePos = confirmPipe(tag,pipePos);

        if (colonPos > 0 || pipePos > 0) {
            int firstMod = (colonPos > 0) ? colonPos : pipePos;
            if (pipePos > 0 && pipePos < colonPos) firstMod = pipePos;
            lookupName = tag.substring(0,firstMod);

            String defValue = null;
            String filter = null;
            String order = Filter.FILTER_LAST;

            if (pipePos > 0 && colonPos > 0) {
                // can come in either order.
                String[] tokens = parseTagTokens(tag, pipePos, colonPos);
                filter   = tokens[0];
                defValue = tokens[1];
                order    = tokens[2];
            } else if (colonPos > 0) {
                // everything after the colon is a default value
                defValue = tag.substring(colonPos+1);
            } else {
                // everything after the first pipe is the filter set
                filter = tag.substring(pipePos+1);
            }

            this.ifNull = defValue;
            this.applyFiltersIfNull = order.equals(Filter.FILTER_LAST);
            this.filters = filter;
        }

        // break deep references like bob.hand.thumb into an array
        // of path segments
        this.path = parsePath(lookupName);
    }

    private String[] parsePath(String deepRef)
    {
        if (deepRef.indexOf('.',1) < 0 || deepRef.charAt(0) == '.') {
            // no segments, or command-prefix (and therefore segments should
            // not be handled here).
            return new String[]{deepRef};
        } else {
            StringTokenizer splitter = new StringTokenizer(deepRef,".");
            int segmentCount = splitter.countTokens();

            String[] path = new String[segmentCount];
            int i=0;
            while (splitter.hasMoreTokens()) {
                path[i] = splitter.nextToken();
                i++;
            }

            return path;
        }
    }

    // pipe denotes a request to apply a filter
    // colon denotes a default value
    // they may come in either order {$tag_name:hello there|url} or {$tag_name|url:hello there}
    //
    // In retrospect, I probably should have considered a nice legible syntax like
    // {$tag_name default="hello there" filter="url"}
    private String[] parseTagTokens(String tagName, int pipePos, int colonPos)
    {
        String filter = null;
        String defValue = null;

        String order = Filter.FILTER_LAST;

        if (colonPos < 0) {
            // no colon token, just pipe
            filter = tagName.substring(pipePos+1);
        } else if (pipePos < colonPos) {
            // both tokens, pipe before colon
            //
            // ok, so colon CAN appear inside regex or onmatch() etc
            // these need to be IGNORED!!
            //
            // pipe may NOT appear in default value, so at least we can limit our scan to the final filter
            int finalPipe = Filter.grokFinalFilterPipe(tagName,pipePos);
            int nextColon = tagName.indexOf(":",finalPipe+1);
            if (nextColon < 0) {
                // lucked out, colon was fake-out, embedded in earlier filter
                filter = tagName.substring(pipePos+1);
            } else {
                int startScan = Filter.grokValidColonScanPoint(tagName,finalPipe+1);
                nextColon = tagName.indexOf(":",startScan);
                if (nextColon < 0) {
                    // colon was fake-out
                    filter = tagName.substring(pipePos+1);
                } else {
                    filter = tagName.substring(pipePos+1,nextColon);
                    defValue = tagName.substring(nextColon+1);
                    order = Filter.FILTER_FIRST;
                }
            }
        } else {
            // both tokens, colon before pipe
            filter = tagName.substring(pipePos+1);
            defValue = tagName.substring(colonPos+1,pipePos);
        }

        return new String[]{ filter, defValue, order };
    }

    /**
     * Don't interpret pipes from an includeIf(...) regex as filter markers
     */
    private int confirmPipe(String tagName, int pipePos)
    {
        String skipToken = "includeIf(";
        String closeToken = ")";
        int doesntCountParen = tagName.indexOf(skipToken);
        if (doesntCountParen < 0) {
            // also have to check for expanded {+(...)} syntax
            skipToken = "include.(";
            doesntCountParen = tagName.indexOf(skipToken);
            if (doesntCountParen < 0) {
                // also skip filters inside a backtick group
                skipToken = "`";
                closeToken = "`";
                doesntCountParen = tagName.indexOf(skipToken);
            }
        }
        if (doesntCountParen < 0) return pipePos;
        // skip to the end-paren and search from there
        int scanFrom = doesntCountParen + skipToken.length();
        int nextParen = tagName.indexOf(closeToken,scanFrom);
        if (closeToken.equals("`")) {
            // skip pipe if it is before close-tick
            if (pipePos > nextParen) return pipePos;
        } else {
            // for the pipe not to count, has to be in /reg|ex/
            int nextSlash = tagName.indexOf("/",scanFrom);
            if (nextSlash < 0 || nextParen < 0) return pipePos;
            if (nextParen < nextSlash) return pipePos;
            // okay, we found a regex. find the end of the regex.
            int regexEnd = RegexFilter.nextRegexDelim(tagName,nextSlash+1);
            nextParen = tagName.indexOf(")",regexEnd+1);
            if (nextParen < 0 || nextParen < pipePos) return pipePos;
        }
        return tagName.indexOf("|",nextParen+1);
    }

    public String[] getPath()
    {
        return path;
    }

    public String getDefaultValue()
    {
        if (ifNull == null || ifNull.length() == 0) return ifNull;

        // check for magic-brace triggers in default value
        char firstChar = ifNull.charAt(0);
        if (firstChar == '~' || firstChar == '$' || firstChar == '+' || firstChar == '^' || firstChar == '.') {
            if (filters == null) {
                return '{'+ifNull+'}';
            } else if (applyFiltersIfNull) {
                return '{'+ifNull+'|'+filters+'}';
            } else {
                return '{'+ifNull+'}';
            }
        }

        // leading magic $~.^+ might be backslash-escaped to nullify
        if (ifNull.charAt(0)=='\\') {
            return ifNull.substring(1);
        }

        return ifNull;
    }

    public String getFilters()
    {
        return filters;
    }

    public boolean applyFiltersFirst()
    {
        return !applyFiltersIfNull;
    }

    static SnippetTag parseTag(String tag)
    {
        SnippetTag parsedTag = new SnippetTag(tag,tag);
        parsedTag.init(tag);
        return parsedTag;
    }

    private static final BlockTag[] BLOCK_TAGS
    = new BlockTag[]{new LoopTag(),
                     new IfTag(),
                     new LocaleTag(),
                     new MacroTag()
                    };

    private static final String[] BLOCK_TAG_TOKENS = extractTagTokens(BLOCK_TAGS);

    private static String[] extractTagTokens(BlockTag[] blockTags)
    {
        String[] tokens = new String[blockTags.length];
        for (int i=0; i<blockTags.length; i++) {
            // This will miss old-style (deprecated?) {^loop(...)} invocation (do we care?)
            tokens[i] = "."+blockTags[i].getBlockStartMarker();
        }
        return tokens;
    }

    public BlockTag getBlockTagType()
    {
        for (int i=0; i<BLOCK_TAG_TOKENS.length; i++) {
            if (tag.startsWith(BLOCK_TAG_TOKENS[i])) {
                if (BLOCK_TAGS[i].hasBody(tag)) {
                    return BLOCK_TAGS[i];
                } else {
                    return null;
                }
            }
        }
        return null;
    }


}
