package com.x5.template;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.filters.RegexFilter;
import com.x5.util.DataCapsuleTable;
import com.x5.util.TableData;

public class LoopTag extends BlockTag
{
    private Chunk chunk;
    private String rowTemplate;
    private String emptyTemplate;
    private Map<String,Object> options;

    private Snippet emptySnippet = null;
    private Snippet dividerSnippet = null;
    private Snippet rowSnippet = null;

    // for speed
    private Chunk rowX;

    private static final String FIRST_MARKER = "first";
    private static final String LAST_MARKER = "last";
    private static final String PLACE_TAG = "place";

    public static void main(String[] args)
    {
        String loopTest =
            "{~.loop data=\"~mydata\" template=\"#test_row\" no_data=\"#test_empty\"}";
        // test that the parser is not in and endless loop.
        LoopTag loop = new LoopTag();
        loop.parseParams(loopTest);
        System.out.println("row_tpl="+loop.rowTemplate);
        System.out.println("empty_tpl="+loop.emptyTemplate);
    }

    public static String expandLoop(String params, Chunk ch, String origin, int depth)
    {
        LoopTag loop = new LoopTag(params, ch, origin);
        StringWriter out = new StringWriter();
        try {
            loop.renderBlock(out, ch, origin, depth);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return out.toString();
    }

    public LoopTag()
    {
    }

    public LoopTag(String params, Chunk ch, String origin)
    {
        this.chunk = ch;
        parseParams(params);

        // this constructor is only called when the loop tag has no body
        // eg {% loop data="$mydata" template="#test_row" no_data="#test_empty" divider="<hr/>" %}
        initWithoutBlock(origin);
    }

    private void initWithoutBlock(String origin)
    {
        // set up snippets
        if (this.chunk == null) return;

        ContentSource snippetRepo = chunk.getTemplateSet();
        if (snippetRepo == null) return;

        if (this.rowTemplate != null) {
            String templateRef = qualifyTemplateRef(origin, rowTemplate);
            this.rowSnippet = snippetRepo.getSnippet(templateRef);
        }
        if (this.emptyTemplate != null) {
            String templateRef = qualifyTemplateRef(origin, emptyTemplate);
            this.emptySnippet = snippetRepo.getSnippet(templateRef);
        }
    }

    public LoopTag(String params, Snippet body)
    {
        parseParams(params);
        initBody(body);
    }

    private void parseParams(String params)
    {
        if (params == null) return;

        if (params.startsWith(".loop(")) {
            parseFnParams(params);
        } else if (params.matches("\\.loop [^\" ]+ .*")) {
            parseEZParams(params);
        } else {
            parseAttributes(params);
        }
    }

    // {^loop in ~data}...{^/loop} (or {^loop in ~data as x}...)
    private void parseEZParams(String paramString)
    {
        String[] params = paramString.split(" +");

        String dataVar = params[2];

        this.options = Attributes.parse(paramString);
        if (options == null) options = new HashMap<String,Object>();
        options.put("data",dataVar);

        // counter is an alias for counter_tag
        if (options.containsKey("counter")) {
            options.put("counter_tag", options.get("counter"));
        }

        if (params.length > 3) {
            if (params[3].equals("as")) {
                String loopVarPrefix = params[4];
                if (loopVarPrefix != null) {
                    if (loopVarPrefix.startsWith("~") || loopVarPrefix.startsWith("$")) {
                        loopVarPrefix = loopVarPrefix.substring(1);
                    }
                    if (loopVarPrefix.contains(":")) {
                        String[] labels = loopVarPrefix.split(":");
                        String valuePrefix = labels[1];
                        if (valuePrefix.startsWith("~") || valuePrefix.startsWith("$")) {
                            valuePrefix = valuePrefix.substring(1);
                        }
                        options.put("keyname",labels[0]);
                        options.put("valname",valuePrefix);
                    }
                    options.put("name",loopVarPrefix);
                }
            }
        }
    }

    // ^loop(~data[...range...],#rowTemplate,#emptyTemplate)
    private void parseFnParams(String params)
    {
        int endOfParams = params.length();
        if (params.endsWith(")")) endOfParams--;
        params = params.substring(".loop(".length(), endOfParams);
        String[] args = params.split(",");
        if (args != null && args.length >= 2) {
            String dataVar = args[0];
            if (options == null) options = new HashMap<String,Object>();
            options.put("data", dataVar);

            this.rowTemplate = args[1];
            if (args.length > 2) {
                this.emptyTemplate = args[2];
            } else {
                this.emptyTemplate = null;
            }
        }
    }

    // {% loop data="~data" template="#..." no_data="#..." range="..." per_page="x" page="x" %}
    private void parseAttributes(String params)
    {
        Map<String,Object> opts = Attributes.parse(params);

        if (opts == null) return;

        // counter is an alias for counter_tag
        if (opts.containsKey("counter")) {
            opts.put("counter_tag", opts.get("counter"));
        }

        this.options = opts;

        this.rowTemplate = (String)opts.get("template");
        this.emptyTemplate = (String)opts.get("no_data");
    }

    @SuppressWarnings("rawtypes")
    private TableData fetchData(String dataVar, String origin)
    {
        TableData data = null;

        if (dataVar != null) {
            int rangeMarker = dataVar.indexOf("[");
            if (rangeMarker > 0) {
                int rangeMarker2 = dataVar.indexOf("]",rangeMarker);
                if (rangeMarker2 < 0) rangeMarker2 = dataVar.length();
                String range = dataVar.substring(rangeMarker+1,rangeMarker2);
                dataVar = dataVar.substring(0,rangeMarker);
                registerOption("range",range);
            }
            char c0 = dataVar.charAt(0);
            boolean isDirective = false;
            if (c0 == '^' || c0 == '.') {
                // expand "external" shortcut syntax eg ^wiki becomes ~.wiki
                dataVar = RegexFilter.applyRegex(dataVar, "s/^[\\^\\.]/~./");
                isDirective = true;
            }
            if (isDirective || c0 == '~' || c0 == '$') {
                // tag reference (eg, tag assigned to query result table)
                dataVar = dataVar.substring(1);

                if (chunk != null) {
                    Object dataStore = chunk.get(dataVar);

                    // if nec, follow pointers until data is reached
                    int depth = 0;
                    while (dataStore != null && depth < 10) {
                        if (dataStore instanceof TableData) {
                            data = (TableData)dataStore;
                        } else if (dataStore instanceof String) {
                            data = InlineTable.parseTable((String)dataStore);
                        } else if (dataStore instanceof Snippet) {
                            // simple strings are now encased in Snippet obj's
                            Snippet snippetData = (Snippet)dataStore;
                            if (snippetData.isSimplePointer()) {
                                dataStore = chunk.get(snippetData.getPointer());
                                depth++;
                                continue;
                            } else {
                                data = InlineTable.parseTable(snippetData.toString());
                            }
                        } else if (dataStore instanceof String[]) {
                            data = new SimpleTable((String[])dataStore);
                        } else if (dataStore instanceof List) {
                            // is it a list of strings? or a list of kindred objects?
                            List list = (List)dataStore;
                            if (list.size() > 0) {
                                Object a = list.get(0);
                                if (a instanceof String) {
                                    data = new SimpleTable(list);
                                } else if (a instanceof Map) {
                                    data = new TableOfMaps(list);
                                } else {
                                    // last-ditch effort to extract data, treat as POJOs
                                    data = TableOfMaps.boxObjectList((List)dataStore);
                                }
                            }
                        } else if (dataStore instanceof Object[]) {
                            // assume array of objects that implement DataCapsule
                            data = DataCapsuleTable.extractData((Object[])dataStore);
                            if (data == null) {
                                // last-ditch effort to extract data, treat as POJOs
                                data = TableOfMaps.boxObjectArray((Object[])dataStore);
                            }
                        } else if (dataStore instanceof Map) {
                            Map object = (Map)dataStore;
                            data = new ObjectTable(object);
                        }

                        // only loop if following pointer
                        break;
                    }
                }
            } else {
                // template reference
                if (chunk != null) {
                    dataVar = qualifyTemplateRef(origin, dataVar);
                    String tableAsString = chunk.getTemplateSet().fetch(dataVar);
                    if (tableAsString != null) {
                        data = InlineTable.parseTable(tableAsString);
                    }
                }
            }
        }

        return data;
    }

    private void registerOption(String param, String value)
    {
        if (options == null) options = new java.util.HashMap<String,Object>();
        options.put(param,value);
    }

    public void cookLoopToPrinter(Writer out, Chunk context, String origin,
            boolean isBlock, int depth, TableData data)
    throws IOException
    {
        if (data == null || !data.hasNext()) {
            if (emptySnippet == null) {
                String errMsg = "[Loop error: Empty Table - please "
                    + (isBlock ? "supply onEmpty section in loop block]"
                               : "specify no_data template parameter in loop tag]");

                if (context == null || context.renderErrorsToOutput()) {
                    out.append(errMsg);
                }
                if (context != null) context.logError(errMsg);
            } else {
                emptySnippet.render(out, context, depth);
            }
            return;
        }

        Snippet dividerSnippet = null;
        boolean createArrayTags = false;
        boolean counterTags = false;
        int counterOffset = 0;
        int counterStep = 1;
        String counterTag = null;
        String firstRunTag = null;
        String lastRunTag = null;
        String placeTag = null;
        String objectKeyLabel = null;
        String objectValueLabel = null;

        if (options != null) {
            if (options.containsKey("dividerSnippet")) {
                dividerSnippet = (Snippet)options.get("dividerSnippet");
            } else if (options.containsKey("divider")) {
                String dividerTemplate = (String)options.get("divider");
                dividerTemplate = qualifyTemplateRef(origin, dividerTemplate);
                ContentSource templates = context.getTemplateSet();
                if (templates != null && templates.provides(dividerTemplate)) {
                    dividerSnippet = templates.getSnippet(dividerTemplate);
                } else {
                    dividerSnippet = Snippet.getSnippet(dividerTemplate);
                }
                options.put("dividerSnippet", dividerSnippet);
            }
            if (options.containsKey("array_tags")) {
                createArrayTags = true;
            }
            if (options.containsKey("counter_tags")) {
                counterTags = true;
            }
            if (options.containsKey("counter_tag")) {
                counterTag = (String)options.get("counter_tag");
                counterTag = eatTagSymbol(counterTag);
                if (counterTag.indexOf(",") > 0) {
                    String[] counterArgs = counterTag.split(",");
                    counterTag = counterArgs[0];
                    try {
                        counterOffset = Integer.parseInt(counterArgs[1]);
                    } catch (NumberFormatException e) {}
                    // optional third arg is step size
                    if (counterArgs.length > 2) {
                        try {
                            counterStep = Integer.parseInt(counterArgs[2]);
                        } catch (NumberFormatException e) {}
                    }
                }
            }
            if (options.containsKey("first_last")) {
                String tagNames = (String)options.get("first_last");
                if (tagNames.indexOf(",") > 0) {
                    String[] userFirstLast = tagNames.split(",");
                    firstRunTag = eatTagSymbol(userFirstLast[0]);
                    lastRunTag  = eatTagSymbol(userFirstLast[1]);
                    if (userFirstLast.length > 2) {
                        placeTag = eatTagSymbol(userFirstLast[2]);
                    }
                }
                if (firstRunTag == null || firstRunTag.length() == 0) {
                    firstRunTag = FIRST_MARKER;
                }
                if (lastRunTag == null || lastRunTag.length() == 0) {
                    lastRunTag = LAST_MARKER;
                }
                if (placeTag == null || placeTag.length() == 0) {
                    placeTag = PLACE_TAG;
                }
            }
            if (options.containsKey("valname")) {
                objectValueLabel = (String)options.get("valname");
                if (options.containsKey("keyname")) {
                    objectKeyLabel = (String)options.get("keyname");
                }
            }
        }

        ChunkFactory factory = context.getChunkFactory();

        if (this.rowX == null) {
            this.rowX = (factory == null) ? new Chunk() : factory.makeChunk();
            this.rowX.append( rowSnippet );
        }
        // make sure cached rowX chunk matches context locale
        rowX.setLocale(context.getLocale());

        String prefix = null;
        if (options != null && options.containsKey("name")) {
            String name = (String)options.get("name");
            prefix = name; //NON_LEGAL.matcher(name).replaceAll("");
        }

        // if looping over object, should provide names for key:value
        if (objectValueLabel == null && data instanceof ObjectTable) {
            // default to $attr:$[name] or $attr:$value
            objectKeyLabel = "attr";
            objectValueLabel = prefix == null ? prefix : "value";
        }

        String[] columnLabels = data.getColumnLabels();

        if (createArrayTags && columnLabels == null) {
            createArrayTags = false;
        }

        // set up all these auto-generated tags before entering the loop.
        String[] prefixedLabels = null;
        String[] prefixedIndices = null;
        String[] anonIndices = null;
        if (prefix != null && columnLabels != null) {
            prefixedLabels = new String[columnLabels.length];
            for (int i=columnLabels.length-1; i>-1; i--) {
                prefixedLabels[i] = prefix + "." + columnLabels[i];
            }
            if (createArrayTags) {
                prefixedIndices = new String[columnLabels.length];
                for (int i=0; i<prefixedIndices.length; i++) {
                    prefixedIndices[i] = prefix + "["+i+"]";
                }
            }
        }
        if (createArrayTags) {
            anonIndices = new String[columnLabels.length];
            for (int i=0; i<anonIndices.length; i++) {
                anonIndices[i] = "DATA["+i+"]";
            }
        }

        int counter = 0;
        while (data.hasNext()) {
            if (counterTags) {
                rowX.set("0",counter);
                rowX.set("1",counter+1);
            }
            if (counterTag != null) {
                rowX.set(counterTag, counterOffset + counter * counterStep);
            }

            if (dividerSnippet != null && counter > 0) {
                dividerSnippet.render(out, context, depth);
            }

            Map<String,Object> record = data.nextRecord();

            if (objectValueLabel != null) {
                if (objectKeyLabel != null) {
                    rowX.setOrDelete(objectKeyLabel, record.get(ObjectTable.KEY));
                }
                rowX.setOrDelete(objectValueLabel, record.get(ObjectTable.VALUE));
            } else {
                if (prefix != null) {
                    rowX.set(prefix, record);
                    if (createArrayTags) {
                        for (int i=columnLabels.length-1; i>-1; i--) {
                            String field = columnLabels[i];
                            Object value = record.get(field);
                            rowX.setOrDelete(prefixedIndices[i], value);
                        }
                    }
                } else {
                    if (columnLabels == null) {
                        for (String key : record.keySet()) {
                            Object value = record.get(key);

                            String fieldName = key;
                            rowX.setOrDelete(fieldName, value);
                        }
                    } else {
                        // loop backwards -- in case any headers are identical,
                        // this ensures the first such named column will be used
                        for (int i=columnLabels.length-1; i>-1; i--) {
                            String field = columnLabels[i];
                            Object value = record.get(field);
                            // prefix with eg x. if prefix supplied
                            String fieldName = field;
                            rowX.setOrDelete(fieldName, value);
                            if (createArrayTags) {
                                rowX.setOrDelete(anonIndices[i], value);
                            }
                        }
                    }
                }
            }

            // for anonymous one-column tables (aka a string array)
            // allow loop in $array as x to use {$x} for the value --
            // otherwise template has to have {$x[0]} or {$x.anonymous}
            // which is silly.
            if (prefix != null && columnLabels != null) {
                if (columnLabels.length == 1 && columnLabels[0].equals(SimpleTable.ANON_ARRAY_LABEL)) {
                    rowX.setOrDelete(prefix, record.get(SimpleTable.ANON_ARRAY_LABEL));
                }
            }

            // if directed, set $is_first and $is_last tags at appropriate times
            if (firstRunTag != null) {
                if (counter == 0) {
                    rowX.set(firstRunTag, "TRUE");
                    rowX.set(placeTag, firstRunTag);
                    if (prefix != null) {
                        rowX.set(prefix + "." + firstRunTag, "TRUE");
                        rowX.set(prefix + "." + placeTag, firstRunTag);
                    }
                } else if (counter == 1) {
                    rowX.unset(firstRunTag);
                    rowX.set(placeTag, "");
                    if (prefix != null) {
                        rowX.unset(prefix + "." + firstRunTag);
                        rowX.set(prefix + "." + placeTag, "");
                    }
                }
            }
            if (lastRunTag != null) {
                if (!data.hasNext()) {
                    String place = counter == 0 ? (firstRunTag + " " + lastRunTag) : lastRunTag;
                    rowX.set(lastRunTag, "TRUE");
                    rowX.set(placeTag, place);
                    if (prefix != null) {
                        rowX.set(prefix + "." + lastRunTag, "TRUE");
                        rowX.set(prefix + "." + placeTag, place);
                    }
                }
            }

            // make sure chunk tags are resolved in context
            rowX.render(out,context);

            counter++;
        }
        // no side effects!
        data.reset();
        rowX.resetTags();

        //return rows.toString();
    }

    private String eatTagSymbol(String tag)
    {
        if (tag == null) return null;

        char c0 = (tag.length() > 0) ? tag.charAt(0) : 0;
        if (c0 == '$' || c0 == '~') {
            return tag.substring(1);
        }

        return tag;
    }

    public boolean hasBody(String openingTag)
    {
        // loop has a body if there is no template="xxx" param
        if (openingTag != null && openingTag.indexOf("template=") < 0) {
            return true;
        } else {
            return false;
        }
    }

    public static String getAttribute(String attr, String toScan)
    {
        if (toScan == null) return null;

        // locate attributes
        int spacePos = toScan.indexOf(' ');

        // no attributes? (no spaces before >)
        if (spacePos < 0) return null;

        // pull out just the attribute definitions
        String attrs = toScan.substring(spacePos+1);

        // find our attribute
        int attrPos = attrs.indexOf(attr);
        if (attrPos < 0) return null;

        // find the equals sign
        int eqPos = attrs.indexOf('=',attrPos + attr.length());

        // find the opening quote
        int begQuotePos = attrs.indexOf('"',eqPos);
        if (begQuotePos < 0) return null;

        // find the closing quote
        int endQuotePos = begQuotePos+1;
        do {
            endQuotePos = attrs.indexOf('"',endQuotePos);
            if (endQuotePos < 0) return null;
            // FIXME this could get tripped up by escaped slash followed by unescaped quote
            if (attrs.charAt(endQuotePos-1) == '\\') {
                // escaped quote, doesn't count -- keep seeking
                endQuotePos++;
            }
        } while (endQuotePos < attrs.length() && attrs.charAt(endQuotePos) != '"');

        if (endQuotePos < attrs.length()) {
            return attrs.substring(begQuotePos+1,endQuotePos);
        } else {
            // never found closing quote
            return null;
        }
    }

    public String getBlockStartMarker()
    {
        return "loop";
    }

    public String getBlockEndMarker()
    {
        return "/loop";
    }

    public boolean doSmartTrimAroundBlock()
    {
        return true;
    }

    private void smartTrim(List<SnippetPart> subParts)
    {
        smartTrimSnippetParts(subParts, isTrimAll());
    }

    public static void smartTrimSnippetParts(List<SnippetPart> subParts, boolean isTrimAll)
    {
        if (subParts != null && subParts.size() > 0) {
            SnippetPart firstPart = subParts.get(0);
            if (firstPart.isLiteral()) {
                String trimmed = isTrimAll ? trimLeft(firstPart.getText())
                        : smartTrimString(firstPart.getText(), true, false);
                firstPart.setText(trimmed);
            }
            if (isTrimAll) {
                SnippetPart lastPart = subParts.get(subParts.size()-1);
                if (lastPart.isLiteral()) {
                    String trimmed = trimRight(lastPart.getText());
                    lastPart.setText(trimmed);
                }
            }
        }
    }

    private static String trimLeft(String x)
    {
        if (x == null) return null;
        int i = 0;
        char c = x.charAt(i);
        while (c == '\n' || c == ' ' || c == '\r' || c == '\t') {
            i++;
            if (i == x.length()) break;
            c = x.charAt(i);
        }
        if (i == 0) return x;
        return x.substring(i);
    }

    private static String trimRight(String x)
    {
        if (x == null) return null;
        int i = x.length()-1;
        char c = x.charAt(i);
        while (c == '\n' || c == ' ' || c == '\r' || c == '\t') {
            i--;
            if (i == -1) break;
            c = x.charAt(i);
        }
        i++;
        if (i >= x.length()) return x;
        return x.substring(0,i);
    }

    private boolean isTrimAll()
    {
        String trimOpt = (options != null) ? (String)options.get("trim") : null;
        if (trimOpt != null && trimOpt.equals("all")) {
            return true;
        } else {
            return false;
        }
    }

    private static final Pattern UNIVERSAL_LF = Pattern.compile("\n|\r\n|\r\r");

    private static String smartTrimString(String x, boolean ignoreAll, boolean isTrimAll)
    {
        if (!ignoreAll && isTrimAll) {
            // trim="all" disables smartTrim.
            return x.trim();
        }

        // if the block begins with (whitespace+) LF, trim initial line
        // otherwise, apply standard/complete trim.
        Matcher m = UNIVERSAL_LF.matcher(x);

        if (m.find()) {
            int firstLF = m.start();
            if (x.substring(0,firstLF).trim().length() == 0) {
                return x.substring(m.end());
            }
        }

        return ignoreAll ? x : x.trim();

        // if there were any line break chars at the end, add just one back.
        /*
        Pattern p = Pattern.compile(".*[ \\t]*(\\r\\n|\\n|\\r\\r)[ \\t]*$");
        Matcher m = p.matcher(x);
        if (m.find()) {
            m.group(0);
            String eol = m.group(1);
            return trimmed + eol;
        } else {
            return trimmed;
        }*/
    }

    private void initBody(Snippet body)
    {
        // the snippet parts should already be properly nested,
        // so any ^onEmpty and ^divider tags at this level should
        // be for this loop.  locate and separate.

        List<SnippetPart> bodyParts = body.getParts();

        int eMarker = -1, dMarker = -1;
        int eMarkerEnd = bodyParts.size(), dMarkerEnd = bodyParts.size();

        for (int i=bodyParts.size()-1; i>=0; i--) {
            SnippetPart part = bodyParts.get(i);
            if (part.isTag()) {
                SnippetTag tag = (SnippetTag)part;
                String tagText = tag.getTag();
                if (tagText.equals(".onEmpty")) {
                    eMarker = i;
                } else if (tagText.equals(".divider")) {
                    dMarker = i;
                } else if (tagText.equals("./divider")) {
                    dMarkerEnd = i;
                } else if (tagText.equals("./onEmpty")) {
                    eMarkerEnd = i;
                }
            }
        }

        boolean doTrim = true;
        String trimOpt = (options == null) ? null : (String)options.get("trim");
        if (trimOpt != null && trimOpt.equalsIgnoreCase("false")) {
            doTrim = false;
        }

        int bodyEnd = -1;

        if (eMarker > -1 && dMarker > -1) {
            if (eMarker > dMarker) {
                bodyEnd = dMarker;
                dMarkerEnd = Math.min(eMarker, dMarkerEnd);
            } else {
                bodyEnd = eMarker;
                eMarkerEnd = Math.min(dMarker, eMarkerEnd);
            }
            emptySnippet = extractParts(bodyParts,eMarker+1,eMarkerEnd,doTrim);
            dividerSnippet = extractParts(bodyParts,dMarker+1,dMarkerEnd,doTrim);
        } else if (eMarker > -1) {
            bodyEnd = eMarker;
            emptySnippet = extractParts(bodyParts,eMarker+1,eMarkerEnd,doTrim);
            dividerSnippet = null;
        } else if (dMarker > -1) {
            bodyEnd = dMarker;
            emptySnippet = null;
            dividerSnippet = extractParts(bodyParts,dMarker+1,dMarkerEnd,doTrim);
        } else {
            emptySnippet = null;
            dividerSnippet = null;
        }

        if (bodyEnd > -1) {
            for (int i=bodyParts.size()-1; i>=bodyEnd; i--) {
                bodyParts.remove(i);
            }
        }

        if (doTrim) smartTrim(bodyParts);

        this.rowSnippet = body;
    }

    private Snippet extractParts(List<SnippetPart> parts, int a, int b, boolean doTrim)
    {
        List<SnippetPart> subParts = new ArrayList<SnippetPart>();
        for (int i=a; i<b; i++) {
            subParts.add(parts.get(i));
        }

        if (doTrim) smartTrim(subParts);

        return new Snippet(subParts);
    }

    @Override
    public void renderBlock(Writer out, Chunk context, String origin, int depth)
        throws IOException
    {
        if (dividerSnippet != null && !options.containsKey("dividerSnippet")) {
            options.put("dividerSnippet", dividerSnippet);
        }

        this.chunk = context;
        TableData data = null;

        if (options != null) {
            data = fetchData((String)options.get("data"), origin);
        }

        cookLoopToPrinter(out, context, origin, true, depth, data);
    }

}
