package com.x5.template;

import java.util.Map;

import com.x5.util.DataCapsuleTable;
import com.x5.util.TableData;

public class Loop implements BlockTagHelper
{
    private TableData data;
    private Chunk chunk;
    private String rowTemplate;
    private String emptyTemplate;
    private Map<String,String> options;

    public static void main(String[] args)
    {
        String loopTest =
            "{~.loop data=\"~mydata\" template=\"#test_row\" no_data=\"#test_empty\"}";
        // test that the parser is not in and endless loop.
        Loop loop = new Loop(loopTest,null);
        System.out.println("row_tpl="+loop.rowTemplate);
        System.out.println("empty_tpl="+loop.emptyTemplate);
    }

    public static String expandLoop(String params, Chunk ch)
    throws BlockTagException
    {
        Loop loop = new Loop(params, ch);
        return loop._cookLoop();
    }

    public Loop(String params, Chunk ch)
    {
        this.chunk = ch;
        parseParams(params);
    }

    private void parseParams(String params)
    {
        if (params == null) return;

        if (params.startsWith(".loop(")) {
            parseFnParams(params);
        } else {
            parseAttributes(params);
        }
    }

    // ^loop(~data[...range...],#rowTemplate,#emptyTemplate)
    private void parseFnParams(String params)
    {
        int endOfParams = params.length();
        if (params.endsWith(")")) endOfParams--;
        params = params.substring(".loop(".length(),endOfParams);
        String[] args = params.split(",");
        if (args != null && args.length >= 2) {
            String dataVar = args[0];
            fetchData(dataVar);

            this.rowTemplate = args[1];
            if (args.length > 2) {
                this.emptyTemplate = args[2];
            } else {
                this.emptyTemplate = null;
            }
        }
    }

    // ^loop data="~data" template="#..." no_data="#..." range="..." per_page="x" page="x"
    private void parseAttributes(String params)
    {
        String dataVar = getAttribute("data", params);
        fetchData(dataVar);
        this.rowTemplate = getAttribute("template", params);
        this.emptyTemplate = getAttribute("no_data", params);

        // okay, this is heinously inefficient, scanning the whole thing every time for each param
        // esp. optional params which probably won't even be there
        String[] optional = new String[]{"range","divider","trim"}; //... what else?
        for (int i=0; i<optional.length; i++) {
            String param = optional[i];
            String val = getAttribute(param, params);
            if (val != null) registerOption(param, val);

            // really?
            if (param.equals("range") && val == null) {
                // alternate range specification via optional params page and per_page
                String perPage = getAttribute("per_page", params);
                String page = getAttribute("page", params);
                if (perPage != null && page != null) {
                    registerOption("range", page + "*" + perPage);
                }
            }
        }
    }

    private void fetchData(String dataVar)
    {
        if (dataVar != null) {
            int rangeMarker = dataVar.indexOf("[");
            if (rangeMarker > 0) {
                int rangeMarker2 = dataVar.indexOf("]",rangeMarker);
                if (rangeMarker2 < 0) rangeMarker2 = dataVar.length();
                String range = dataVar.substring(rangeMarker+1,rangeMarker2);
                dataVar = dataVar.substring(0,rangeMarker);
                registerOption("range",range);
            }
            if (dataVar.charAt(0) == '^') {
                // expand "external" shortcut syntax eg ^wiki becomes ~.wiki
                dataVar = TextFilter.applyRegex(dataVar, "s/^\\^/~./");
            }
            if (dataVar.startsWith("~")) {
                // tag reference (eg, tag assigned to query result table)
                dataVar = dataVar.substring(1);

                if (chunk != null) {
                    Object dataStore = chunk.get(dataVar);
                    if (dataStore instanceof TableData) {
                        this.data = (TableData)dataStore;
                    } else if (dataStore instanceof String) {
                        this.data = InlineTable.parseTable((String)dataStore);
                        registerOption("array_index_tags","FALSE");
                    } else if (dataStore instanceof String[]) {
                    	this.data = new SimpleTable((String[])dataStore);
                    } else if (dataStore instanceof Object[]) {
                    	// assume array of objects that implement DataCapsule
                    	this.data = DataCapsuleTable.extractData((Object[])dataStore);
                        registerOption("array_index_tags","FALSE");
                    }
                }
            } else {
                // template reference
                if (chunk != null) {
                	String tableAsString = chunk.getTemplateSet().fetch(dataVar);
                    this.data = InlineTable.parseTable(tableAsString);
                }
            }
        }
    }

    private void registerOption(String param, String value)
    {
        if (options == null) options = new java.util.HashMap<String,String>();
        options.put(param,value);
    }

    private String _cookLoop()
    throws BlockTagException
    {
    	if (rowTemplate == null) throw new BlockTagException("loop",this);
        return Loop.cookLoop(data, chunk, rowTemplate, emptyTemplate, options, false);
    }

    public static String cookLoop(TableData data, Chunk context,
    		String rowTemplate, String emptyTemplate,
    		Map<String,String> opt, boolean isBlock)
    {
        if (data == null || !data.hasNext()) {
            if (emptyTemplate == null) {
                return "[Loop Error: Empty Table - please specify no_data template parameter in ^loop tag]";
            } else if (emptyTemplate.length() == 0) {
            	return "";
            } else {
                if (isBlock) {
                    return emptyTemplate;
                } else {
                    return context.getTemplateSet().fetch(emptyTemplate);
                }
            }
        }
        
        String dividerTemplate = null;
        boolean createArrayTags = true;
        
        if (opt != null) {
        	if (opt.containsKey("divider")) {
	        	dividerTemplate = (String)opt.get("divider");
	        	ContentSource templates = context.getTemplateSet();
	        	if (templates.provides(dividerTemplate)) {
	        		dividerTemplate = templates.fetch(dividerTemplate);
	        	}
        	}
        	if (opt.containsKey("array_index_tags")) {
        		createArrayTags = false;
        	}
        }

        ChunkFactory factory = context.getChunkFactory();

        String[] columnLabels = data.getColumnLabels();

        StringBuilder rows = new StringBuilder();
        Chunk rowX;
        if (isBlock) {
            rowX = factory.makeChunk();
            rowX.append(rowTemplate);
        } else {
            rowX = factory.makeChunk(rowTemplate);
        }
        int counter = 0;
        while (data.hasNext()) {
            rowX.set("0",counter);
            rowX.set("1",counter+1);
            
            if (dividerTemplate != null && counter > 0) {
            	rows.append( dividerTemplate );
            }

            Map<String,String> record = data.nextRecord();

            // loop backwards -- in case any headers are identical,
            // this ensures the first such named column will be used
            for (int i=columnLabels.length-1; i>-1; i--) {
                String field = columnLabels[i];
                String value = record.get(field);
                rowX.setOrDelete(field, value);
                if (createArrayTags) {
	                rowX.setOrDelete("DATA["+i+"]",value);
                }
            }

            // make sure chunk tags are resolved in context
            rows.append( rowX.toString(context) );

            counter++;
        }

        return rows.toString();
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

    public String cookBlock(String blockBody)
    {
        // split body up into row template and optional empty template
        //  (delimited by {^on_empty} )
        // trim both, unless requested not to.
//        return null;
        boolean isBlock = true;
        
        boolean doTrim = true;
        String trimOpt = (options == null) ? null : options.get("trim");
        if (trimOpt != null && trimOpt.equalsIgnoreCase("false")) {
            doTrim = false;
        }
        
        String delim = "{~.on_empty}";
        
        int delimPos = blockBody.indexOf(delim);
        if (delimPos > -1) {
            String template = blockBody.substring(0,delimPos);
            String onEmpty = blockBody.substring(delimPos+delim.length());
            this.rowTemplate = doTrim ? template.trim() : template;
            this.emptyTemplate = doTrim ? onEmpty.trim() : onEmpty;
        } else {
            this.rowTemplate = doTrim ? blockBody.trim() : blockBody;
        }
        
        return Loop.cookLoop(data, chunk, rowTemplate, emptyTemplate, options, isBlock);
    }
    
    public String getBlockEndMarker()
    {
        return "/loop";
    }

}
