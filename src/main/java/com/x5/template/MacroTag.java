package com.x5.template;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.x5.util.LiteXml;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
import static net.minidev.json.parser.ContainerFactory.FACTORY_ORDERED;
import static net.minidev.json.parser.JSONParser.MODE_RFC4627;


public class MacroTag extends BlockTag
{
    public static final String MACRO_MARKER = "exec";
    public static final String MACRO_END_MARKER = "/exec";

    private static final int ARG_START = MACRO_MARKER.length()+2;

    // macro can take args in many formats
    private static final String FMT_XML         = "xml";
    private static final String FMT_JSON_LAX    = "json";
    private static final String FMT_JSON_STRICT = "json-strict";
    private static final String FMT_ORIGINAL    = "original";

    private String templateRef;
    private Snippet template;
    private Map<String,Object> macroDefs;
    private String dataFormat = FMT_ORIGINAL;

    private List<String> inputErrs = null;

    public MacroTag()
    {
    }

    public MacroTag(String tagName, Snippet body)
    {
        if (tagName.length() > ARG_START) {
            templateRef = tagName.substring(ARG_START).trim();

            // check for nonstandard requested format
            int spacePos = templateRef.indexOf(' ');
            if (spacePos > 0) {
                dataFormat = templateRef.substring(spacePos+1).toLowerCase();
                if (dataFormat.charAt(0) == '@') dataFormat = dataFormat.substring(1);
                templateRef = templateRef.substring(0, spacePos);
            }

            // @inline should trigger a search for inline body within exec block
            // 1st arg can also be inline exec args format eg @json
            if (templateRef.charAt(0) == '@') {
                if (!templateRef.startsWith("@inline") && spacePos < 0) {
                    dataFormat = templateRef.substring(1).toLowerCase();
                }
                templateRef = null;
            }
        }

        // operate on clone to preserve body of parent snippet,
        // otherwise we break toString() since we are about to
        // do some surgery on this snippet's parts list.
        Snippet bodyDouble = body.copy();

        if (templateRef == null) {
            parseInlineTemplate(bodyDouble);
        }
        parseDefs(bodyDouble);
    }

    private void parseInlineTemplate(Snippet body)
    {
        List<SnippetPart> parts = body.getParts();
        int bodyEnd = parts.size();
        int eatUntil = bodyEnd;
        for (int i=bodyEnd-1; i>=0; i--) {
            SnippetPart part = parts.get(i);
            if (part.isTag()) {
                SnippetTag tag = (SnippetTag)part;
                if (tag.getTag().equals("./body")) {
                    bodyEnd = i;
                    eatUntil = i+1;
                } else if (tag.getTag().startsWith(".data")) {
                    bodyEnd = i;
                    eatUntil = i;
                } else if (tag.getTag().equals(".body")) {
                    // everything after this marker is the template
                    Snippet inlineSnippet = new Snippet(parts, i+1, bodyEnd);
                    inlineSnippet.setOrigin(body.getOrigin());

                    // trim leading whitespace up to first line break
                    List<SnippetPart> inlineParts = inlineSnippet.getParts();
                    LoopTag.smartTrimSnippetParts(inlineParts, false);

                    this.template = inlineSnippet;

                    // strip inline template away, no need to parse for args
                    for (int j=eatUntil-1; j>=i; j--) {
                        parts.remove(j);
                    }
                    return;
                }
            }
        }
    }

    private void parseDefs(Snippet body)
    {
        body = stripCasing(body);

        if (dataFormat.equals(FMT_ORIGINAL)) {
            parseDefsOriginal(body);
        } else if (dataFormat.equals(FMT_JSON_STRICT)) {
            parseDefsJsonStrict(body);
        } else if (dataFormat.equals(FMT_JSON_LAX)) {
            parseDefsJsonLax(body);
        } else if (dataFormat.equals(FMT_XML)) {
            parseDefsXML(body);
        }
    }

    private Snippet stripCasing(Snippet body)
    {
        List<SnippetPart> parts = body.getParts();
        if (parts == null) return body;

        int dataStart = -1;
        int dataEnd = parts.size();
        for (int i=0; i<dataEnd; i++) {
            SnippetPart part = parts.get(i);
            if (part.isTag()) {
                SnippetTag tag = (SnippetTag)part;
                String tagMeat = tag.getTag();
                if (tagMeat.startsWith(".data")) {
                    parseDataFormat(tagMeat);
                    dataStart = i;
                } else if (tagMeat.equals("./data")) {
                    dataEnd = i;
                }
            }
        }

        // never found {% data %} tag?
        if (dataStart == -1) return body;

        Snippet dataSnippet = new Snippet(parts, dataStart+1, dataEnd);
        if (this.templateRef == null && this.template == null) {
            // no {% body %} block, so inline template is
            // everything minus the {% data %} block
            List<SnippetPart> preData = parts.subList(0, dataStart);
            LoopTag.smartTrimSnippetParts(preData, false);
            if (dataEnd < parts.size()) {
                List<SnippetPart> postData = parts.subList(dataEnd+1, parts.size());
                LoopTag.smartTrimSnippetParts(postData, false);
                parts.remove(dataEnd);
            }
            for (int i=dataEnd-1; i>=dataStart; i--) {
                parts.remove(i);
            }
            this.template = body;
        }
        return dataSnippet;
    }

    private void parseDataFormat(String dataTag)
    {
        if (dataTag.length() < 6) return;

        String params = dataTag.substring(5);
        String format = params;

        Map<String,Object> opts = Attributes.parse(params);
        if (opts != null && opts.containsKey("format")) {
            format = (String)opts.get("format");
        }

        format = format.trim();
        if (format.startsWith("@")) format = format.substring(1);

        this.dataFormat = format;
    }

    @SuppressWarnings("unchecked")
    private void parseDefsJsonLax(Snippet body)
    {
        body.setOrigin(null); // don't render ORIGIN comment
        String json = body.toString();

        // check for json-smart jar, if not present then output a helpful
        // message to stderr.
        try {
            Class.forName("net.minidev.json.JSONValue");
            // it exists on the classpath
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
                parseDefsJacksonJsonLax(json);
                return;
            } catch (ClassNotFoundException e2) {
                logInputError("Error: template uses json-formatted args in exec, but json-smart or jackson is not in the classpath!");
                return;
            }
        }

        Object parsedValue = JSONValue.parseKeepingOrder(json);
        if (parsedValue instanceof Map) {
            Map<String,Object> defs = (Map<String,Object>)parsedValue;
            importJSONDefs(defs);
        } else if (parsedValue instanceof JSONArray || parsedValue instanceof List) {
            logInputError("Error processing template: exec expected JSON object, not JSON array.");
        } else if (parsedValue instanceof String && parsedValue.toString().trim().length() > 0) {
            logInputError("Error processing template: exec expected JSON object.");
        }
    }

    @SuppressWarnings("unchecked")
    private void parseDefsJsonStrict(Snippet body)
    {
        try {
            body.setOrigin(null); // don't render ORIGIN comment
            String json = body.toString();

            // check for json-smart or jackson, if not present then output a helpful
            // message to stderr.
            try {
                Class.forName("net.minidev.json.JSONValue");
                // it exists on the classpath
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
                    parseDefsJacksonJsonStrict(json);
                    return;
                } catch (ClassNotFoundException e2) {
                    logInputError("Error: template uses json-formatted args in exec, but json-smart or jackson are not in the classpath!");
                    return;
                }
            }

            Object parsedValue = parseStrictJsonKeepingOrder(json);
            if (parsedValue instanceof Map) {
                Map<String,Object> defs = (Map<String,Object>)parsedValue;
                importJSONDefs(defs);
            } else if (parsedValue instanceof JSONArray || parsedValue instanceof List) {
                logInputError("Error processing template: exec expected JSON object, not JSON array.");
            } else if (parsedValue instanceof String && parsedValue.toString().trim().length() > 0) {
                logInputError("Error processing template: exec expected JSON object.");
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private void parseDefsJacksonJsonLax(String json) {
        try {
            Map<String,Object> defs = JacksonJsonParser.parseJsonLax(json);
            importJSONDefs(defs);
        } catch (JsonParseException e) {
            logInputError(e.getMessage());
        }
    }

    private void parseDefsJacksonJsonStrict(String json) {
        try {
            Map<String,Object> defs = JacksonJsonParser.parseJsonStrict(json);
            importJSONDefs(defs);
        } catch (JsonParseException e) {
            logInputError(e.getMessage());
        }
    }

    private void logInputError(String errMsg)
    {
        if (inputErrs == null) inputErrs = new ArrayList<String>();

        inputErrs.add(errMsg);
    }

    private Object parseStrictJsonKeepingOrder(String json)
    throws net.minidev.json.parser.ParseException
    {
        // This wasn't one of the stock parse options, but, not too
        // hard to cobble together.
        return new JSONParser(MODE_RFC4627).parse(json, FACTORY_ORDERED);
    }

    private void importJSONDefs(Map<String,Object> defs)
    {
        this.macroDefs = defs;
    }

    private void parseDefsXML(Snippet body)
    {
        body.setOrigin(null); // don't render ORIGIN comment
        LiteXml xml = new LiteXml(body.toString());
        this.macroDefs = parseXMLObject(xml);
    }

    private Map<String,Object> parseXMLObject(LiteXml xml)
    {
        LiteXml[] rules = xml.getChildNodes();
        if (rules == null) {
            return null;
        } else {
            Map<String,Object> tags = new HashMap<String,Object>();

            for (LiteXml rule : rules) {
                String tagName = rule.getNodeType();

                // check for nested object
                LiteXml[] children = rule.getChildNodes();

                if (children == null) {
                    tags.put(tagName, rule.getNodeValue());
                } else {
                    tags.put(tagName, parseXMLObject(rule));
                }
                Map<String,String> attrs = rule.getAttributes();
                if (attrs != null) {
                    for (String key : attrs.keySet()) {
                        tags.put(tagName+"@"+key, attrs.get(key));
                    }
                }
            }

            return tags;
        }
    }

    private void parseDefsOriginal(Snippet body)
    {
        List<SnippetPart> parts = body.getParts();

        if (parts == null) return;

        for (int i=0; i<parts.size(); i++) {
            // seek until a tag definition {$tag_def=} is found
            SnippetPart part = parts.get(i);
            if (part.isTag()) {
                String tagText = ((SnippetTag)part).getTag();
                if (tagText.trim().endsWith("=")) {
                    int j = findMatchingDefEnd(parts,i+1);
                    Snippet def = new Snippet(parts,i+1,j);
                    def.setOrigin(body.getOrigin());
                    String varName = tagText.substring(0,tagText.length()-1);
                    saveDef(varName,def);
                    // skip to next def
                    i = j;

                    if (j < parts.size()) {
                        SnippetPart partJ = parts.get(j);
                        if (partJ.getText().equals("{=}")) {
                            // skip over endDef tag
                            i = j+1;
                        }
                    }
                } else {
                    // some vars are defined simply, like so {$name=Bob} or {$name = Bob}
                    String[] simpleDef = getSimpleDef(tagText);
                    if (simpleDef != null) {
                        saveDef(simpleDef[0],simpleDef[1],body.getOrigin());
                    }
                }
            }
        }
    }

    private int findMatchingDefEnd(List<SnippetPart> parts, int startAt)
    {
        // find next {=} defEnd tag or next defBegin-style tag
        // which is NOT inside a nested macroDef.

        // actually any nested macrodef tags shouldn't be at this level
        // since Snippet.groupBlocks helpfully tucks them away.

        // default is to eat entire rest of block
        int allTheRest = parts.size();

        for (int i=startAt; i<allTheRest; i++) {
            SnippetPart part = parts.get(i);
            if (part.isTag()) {
                String tagText = ((SnippetTag)part).getTag();
                int eqPos = tagText.indexOf('=');
                if (eqPos < 0) continue;
                if (tagText.length() == 1) return i; // found explicit def-cap {=}

                // we're good as long as ".|:(" do not appear to the left of the =
                char[] tagChars = tagText.toCharArray();
                char c = '=';
                for (int x=0; x<eqPos; x++) {
                    c = tagChars[x];
                    if (c == '.' || c == '|' || c == ':' || c == '(') {
                        // signal failure
                        c = 0;
                        break;
                    }
                }
                if (c == 0) {
                    continue; // fail, keep looking
                } else {
                    // made it to the = safely!  this is the next assignment, close def off
                    return i;
                }
            }
        }

        return allTheRest;
    }

    private String[] getSimpleDef(String tagText)
    {
        int eqPos = tagText.indexOf('=');
        if (eqPos > -1) {
            String varName = tagText.substring(0,eqPos).trim();
            String varValue = tagText.substring(eqPos+1);
            // trim this: {~name = Bob } but not this {~name= Bob}
            if (varValue.charAt(0) == ' ') {
                if (tagText.charAt(eqPos-1) == ' ') {
                    varValue = varValue.trim();
                }
            }
            String[] assignment = new String[]{varName,varValue};
            return assignment;
        } else {
            return null;
        }
    }

    private void saveDef(String tag, String def, String origin)
    {
        if (tag == null || def == null) return;
        Snippet simple = Snippet.getSnippet(def, origin);
        saveDef(tag,simple);
    }

    private void saveDef(String tag, Snippet snippet)
    {
        if (tag == null || snippet == null) return;
        if (macroDefs == null) macroDefs = new HashMap<String,Object>();
        macroDefs.put(tag, snippet);
    }

    public void renderBlock(Writer out, Chunk context, String origin, int depth)
        throws IOException
    {
        Chunk macro = null;
        ChunkFactory theme = context.getChunkFactory();

        if (templateRef != null && theme != null) {
            templateRef = qualifyTemplateRef(origin, templateRef);
            macro = theme.makeChunk(templateRef);
        } else if (template != null) {
            macro = (theme == null) ? new Chunk() : theme.makeChunk();
            macro.append(template);
        } else {
            // no template! bail
            return;
        }

        // any problems with input?  now is the time to raise red flag.
        if (inputErrs != null) {
            if (context.renderErrorsToOutput()) {
                for (String err : inputErrs) {
                    out.append('[');
                    out.append(err);
                    out.append(']');
                }
            }
            for (String err : inputErrs) {
                context.logError(err);
            }
        }

        if (macroDefs != null) {
            Set<String> keys = macroDefs.keySet();
            if (keys != null) {
                for (String tagName : keys) {
                    Object o = macroDefs.get(tagName);
                    macro.setOrDelete(tagName,resolvePointers(context,origin,o,0));
                }
            }
        }
        macro.render(out, context);
    }

    private Object resolvePointers(Chunk context, String origin, Object o, int depth)
    {
        // don't recurse forever...
        if (depth > 10) return o;

        if (o instanceof String) o = Snippet.getSnippet((String)o, origin);
        if (o instanceof Snippet) {
            Snippet s = (Snippet)o;
            if (s.isSimplePointer()) {
                // resolve values which are one single tag
                Object n = context.resolveTagValue(s.getPointerTag(), 1, origin);
                if (n == null) return o;
                o = resolvePointers(context, origin, n, depth+1);
            }
        }
        return o;
    }

    public String getBlockStartMarker()
    {
        return MACRO_MARKER;
    }

    public String getBlockEndMarker()
    {
        return MACRO_END_MARKER;
    }

    public boolean doSmartTrimAroundBlock()
    {
        return true;
    }

    private static class JacksonJsonParser {
        public static Map<String,Object> parseJsonLax(String json) throws JsonParseException {
            return parseJson(json, false);
        }

        public static Map<String,Object> parseJsonStrict(String json) throws JsonParseException {
            return parseJson(json, true);
        }

        private static Map<String,Object> parseJson(String json, boolean isStrict) throws JsonParseException {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            if (!isStrict) {
                mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
                mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
                mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_TRAILING_COMMA, true);
            }
            try {
                com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(json);
                if (node.isObject()) {
                    Map<String,Object> defs = mapper.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<TreeMap<String, Object>>(){});
                    return defs;
                } else if (node.isArray()) {
                    throw new JsonParseException("Error processing template: exec expected JSON object, not JSON array.", null);
                } else {
                    throw new JsonParseException("Error processing template: exec expected JSON object.", null);
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new JsonParseException("Error processing template: exec expected JSON object.", e);
            }
        }
    }

    private static class JsonParseException extends Exception {
        public JsonParseException(String message, Throwable t) {
            super(message, t);
        }
    }
}
