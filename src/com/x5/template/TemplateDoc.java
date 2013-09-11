package com.x5.template;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

public class TemplateDoc implements Iterator<TemplateDoc.Doclet>, Iterable<TemplateDoc.Doclet>
{
    private static final String COMMENT_START = "{!--";
    private static final String COMMENT_END = "--}";

    public static final String LITERAL_START = "{^literal}";
    public static final String LITERAL_START2 = "{.literal}";
    public static final String LITERAL_SHORTHAND = "{^^}"; // this was a dumb idea
    public static final String LITERAL_END = "{^}";
    public static final String LITERAL_END_EXPANDED = "{~.}";
    public static final String LITERAL_END_LONGHAND = "{/literal}";

    private static final String SUB_START = "{#";
    private static final String SUB_NAME_END = "}";
    private static final String SUB_END = "{#}";
    private static final String SKIP_BLANK_LINE = "";

    public static final String MACRO_START = "{*";
    public static final String MACRO_NAME_END = "}";
    public static final String MACRO_END = "{*}";
    public static final String MACRO_LET = "{=";
    public static final String MACRO_LET_END = "}";

    private String stub;
    private InputStream in;
    private String encoding = getDefaultEncoding();

    private Doclet queued = null;

    public TemplateDoc(String name, String rawTemplate)
    {
        this.stub = truncateNameToStub(name);
        try {
            in = new ByteArrayInputStream(rawTemplate.getBytes(encoding));
        } catch (UnsupportedEncodingException e) {
            in = new ByteArrayInputStream(rawTemplate.getBytes());
        }
    }

    public TemplateDoc(String name, InputStream in)
    {
        this.stub = truncateNameToStub(name);
        this.in = in;
    }

    public Iterable<Doclet> parseTemplates(String encoding)
    throws IOException
    {
        this.encoding = encoding;
        this.brTemp = new BufferedReader(new InputStreamReader(in,encoding));
        return (Iterable<Doclet>)this;
    }

    public class Doclet
    {
        private String name;
        private String rawTemplate;

        public Doclet(String name, String rawTemplate)
        {
            this.name = name;
            this.rawTemplate = rawTemplate;
        }

        public String getName()
        {
            return name;
        }

        public String getTemplate()
        {
            return rawTemplate;
        }

        public Snippet getSnippet()
        {
            return Snippet.getSnippet(rawTemplate);
        }
    }

    static String truncateNameToStub(String name)
    {
        int slashPos = name.lastIndexOf('/');
        if (slashPos < -1) slashPos = name.lastIndexOf('\\');

        String folder = null;
        String stub;
        if (slashPos > -1) {
            folder = name.substring(0,slashPos+1);
            stub = name.substring(slashPos+1).replace('#','.');
        } else {
            stub = name.replace('#','.');
        }

        int dotPos = stub.indexOf(".");
        if (dotPos > -1) stub = stub.substring(0,dotPos);

        if (slashPos > -1) {
            char fs = System.getProperty("file.separator").charAt(0);
            folder.replace('\\',fs);
            folder.replace('/',fs);
            return folder + stub;
        } else {
            return stub;
        }
    }

    //
    // boy, this subtemplate code sure is ugly
    // ...but being able to define multiple templates per file sure is handy
    //
    private BufferedReader brTemp;
    private StringBuilder rootTemplate = new StringBuilder();
    private String line = null;

    protected Doclet nextTemplate()
        throws IOException
    {
        if (rootTemplate == null) return null;
        if (bufferStack.size() > 0) {
            Doclet subtpl = nextSubtemplate(popNameFromStack(),"");
            if (subtpl != null) {
                return subtpl;
            }
        }
        StringBuilder commentBuf;

        while (brTemp.ready()) {
            line = brTemp.readLine();
            if (line == null) break;
            int comPos = line.indexOf(COMMENT_START);
            int subPos = line.indexOf(SUB_START);
            // first, skip over any comments
            while (comPos > -1 && (subPos < 0 || subPos > comPos)) {
                commentBuf = new StringBuilder();
                line = skipComment(comPos,line,brTemp,commentBuf);
                // line up to comPos is beforeComment
                // line after comPos is now afterComment
                String beforeComment = line.substring(0,comPos);
                String afterComment = line.substring(comPos);

                // preserve comment for clean fetch
                rootTemplate.append(beforeComment);
                rootTemplate.append(commentBuf);
                line = afterComment;

                // check for another comment on same line
                comPos = line.indexOf(COMMENT_START);
                subPos = line.indexOf(SUB_START);
            }
            // then, strip out any subtemplates
            Doclet subtpl = null;
            boolean lineFeed = true;
            if (subPos > -1) {
                int subEndPos = line.indexOf(SUB_END);
                if (subEndPos == subPos) {
                    // errant subtemplate end marker, ignore
                } else {
                    // parse out new template name and begin recursive separation of subtemplates
                    int subNameEnd = line.indexOf(SUB_NAME_END, subPos + SUB_START.length());
                    if (subNameEnd > -1) {
                        rootTemplate.append(line.substring(0,subPos));
                        String subName = line.substring(subPos + SUB_START.length(),subNameEnd);
                        String restOfLine = line.substring(subNameEnd + SUB_NAME_END.length());
                        subtpl = nextSubtemplate(stub + "#" + subName, restOfLine);
                        // if after removing subtemplate, line is blank, don't output a blank line
                        if (line.length() < 1) {
                            lineFeed = false;
                        }
                    }
                }
            }
            if (lineFeed) {
                rootTemplate.append(line);
                // This will not add newline at EOF even if present in template file
                // Is this the Right Thing to do?
                // And if not, how would we even detect a trailing newline in the file?
                // ie since readLine() returns the same string either way.
                if (brTemp.ready()) rootTemplate.append("\n");
            }
            if (subtpl != null) {
                return subtpl;
            }
        }
        String root = rootTemplate.toString();
        rootTemplate = null;
        return new Doclet(stub,root);
    }

    private String getCommentLines(int comBegin, String firstLine, BufferedReader brTemp, StringBuilder sbTemp)
        throws IOException
    {
        int comEnd = firstLine.indexOf(COMMENT_END,comBegin+2);
        int endMarkerLen = COMMENT_END.length();

        if (comEnd > -1) {
            // easy case -- comment does not span lines
            comEnd += endMarkerLen;
            sbTemp.append(firstLine.substring(0,comEnd));
            return firstLine.substring(comEnd);
        } else {
            sbTemp.append(firstLine);
            sbTemp.append("\n");
            // multi-line comment, keep appending until we encounter comment-end marker
            String line = null;
            while (brTemp.ready()) {
                line = brTemp.readLine();
                if (line == null) break;

                comEnd = line.indexOf(COMMENT_END);
                if (comEnd > -1) {
                    comEnd += endMarkerLen;
                    sbTemp.append(line.substring(0,comEnd));
                    return line.substring(comEnd);
                }
                sbTemp.append(line);
                sbTemp.append("\n");
            }
            // never found! appended rest of file as unterminated comment. burp.
            return "";
        }
    }

    private String getLiteralLines(int litBegin, String firstLine, BufferedReader brTemp, StringBuilder sbTemp)
        throws IOException
    {
        int litEnd = firstLine.indexOf(LITERAL_END,litBegin+2);
        int endMarkerLen = LITERAL_END.length();

        // {^} ends a literal block, OR {/literal} -- whichever comes first.
        int litEndLong = firstLine.indexOf(LITERAL_END_LONGHAND,litBegin+2);
        if (litEndLong > -1 && (litEnd < 0 || litEndLong < litEnd)) {
            litEnd = litEndLong;
            endMarkerLen = LITERAL_END_LONGHAND.length();
        }

        if (litEnd > -1) {
            // easy case -- literal does not span lines
            litEnd += endMarkerLen;
            sbTemp.append(firstLine.substring(0,litEnd));
            return firstLine.substring(litEnd);
        } else {
            sbTemp.append(firstLine);
            sbTemp.append("\n");
            // multi-line literal, keep appending until we encounter literal-end marker
            String line = null;
            while (brTemp.ready()) {
                line = brTemp.readLine();
                if (line == null) break;

                litEnd = line.indexOf(LITERAL_END);
                // {^} ends a literal block, OR {/literal} -- whichever comes first.
                litEndLong = line.indexOf(LITERAL_END_LONGHAND);
                if (litEndLong > -1 && (litEnd < 0 || litEndLong < litEnd)) {
                    litEnd = litEndLong;
                    endMarkerLen = LITERAL_END_LONGHAND.length();
                }

                if (litEnd > -1) {
                    litEnd += endMarkerLen;
                    sbTemp.append(line.substring(0,litEnd));
                    return line.substring(litEnd);
                }
                sbTemp.append(line);
                sbTemp.append("\n");
            }
            // never found! appended rest of file as unterminated literal. burp.
            return "";
        }
    }

    // locate end of comment, and strip it but save it!
    private String skipComment(int comPos, String firstLine, BufferedReader brTemp, StringBuilder commentBuf)
        throws IOException
    {
        String beforeComment = firstLine.substring(0,comPos);

        int comEndPos = firstLine.indexOf(COMMENT_END);
        if (comEndPos > -1) {
            // easy case -- comment does not span lines
            comEndPos += COMMENT_END.length();
            // if removing comment leaves line with only whitespace...
            commentBuf.append(firstLine.substring(comPos,comEndPos));
            return beforeComment + firstLine.substring(comEndPos);
        } else {
            // keep eating lines until the end marker is found
            commentBuf.append(firstLine.substring(comPos));
            commentBuf.append("\n");

            String line = null;
            while (brTemp.ready()) {
                line = brTemp.readLine();
                if (line == null) break;

                comEndPos = line.indexOf(COMMENT_END);
                if (comEndPos > -1) {
                    comEndPos += COMMENT_END.length();
                    commentBuf.append(line.substring(0,comEndPos));
                    return beforeComment + line.substring(comEndPos);
                } else {
                    commentBuf.append(line);
                    commentBuf.append("\n");
                }
            }
            // never found!  ate rest of file.  burp.
            return beforeComment;
        }
    }

    // locate end of comment and remove it from input
    private String stripComment(int comPos, String firstLine, BufferedReader brTemp)
        throws IOException
    {
        String beforeComment = firstLine.substring(0,comPos);
        int comEndPos = firstLine.indexOf(COMMENT_END);
        if (comEndPos > -1) {
            // easy case -- comment does not span lines
            comEndPos += COMMENT_END.length();
            // if removing comment leaves line with only whitespace...
            return beforeComment + firstLine.substring(comEndPos);
        } else {
            // keep eating lines until the end marker is found
            String line = null;
            while (brTemp.ready()) {
                line = brTemp.readLine();
                if (line == null) break;

                comEndPos = line.indexOf(COMMENT_END);
                if (comEndPos > -1) {
                    comEndPos += COMMENT_END.length();
                    return beforeComment + line.substring(comEndPos);
                }
            }
            // never found!  ate rest of file.  burp.
            return beforeComment;
        }
    }

    public static int findLiteralMarker(String text)
    {
        return findLiteralMarker(text, 0);
    }

    public static int findLiteralMarker(String text, int startAt)
    {
        int literalPos = text.indexOf(LITERAL_START, startAt);
        int litPos     = text.indexOf(LITERAL_SHORTHAND, startAt);
        if (litPos > -1 && literalPos > -1) {
            literalPos = Math.min(literalPos, litPos);
        } else if (litPos > -1 || literalPos > -1) {
            literalPos = Math.max(literalPos, litPos);
        }
        return literalPos;
    }

    private ArrayList<String> lineStack = new ArrayList<String>();
    private ArrayList<String> nameStack = new ArrayList<String>();
    private ArrayList<StringBuilder> bufferStack = new ArrayList<StringBuilder>();
    // scan until matching end-of-subtemplate marker found {#}
    // recurse for stripping/caching nested subtemplates
    // strip out all comments
    // preserve {^literal}...{^} blocks (also {^^}...{^} )
    private Doclet nextSubtemplate(String name, String firstLine)
        throws IOException
    {
        StringBuilder sbTemp;
        if (bufferStack.size() > 0) {
            sbTemp = popBufferFromStack();
        } else {
            sbTemp = new StringBuilder();
        }
        // scan for markers
        int subEndPos  = firstLine.indexOf(SUB_END);
        int comPos     = firstLine.indexOf(COMMENT_START);
        int literalPos = findLiteralMarker(firstLine);

        boolean skipFirstLine = false;

        // special handling for literal blocks & comments
        while (literalPos > -1 || comPos > -1) {
            // if end-marker present, kick out if it's not inside a comment or a literal block
            if (subEndPos > -1) {
                if ((literalPos < 0 || subEndPos < literalPos) && (comPos < 0 || subEndPos < comPos)) {
                    break;
                }
            }

            // first, preserve any literal blocks
            while (literalPos > -1 && (comPos < 0 || comPos > literalPos)) {
                if (subEndPos < 0 || subEndPos > literalPos) {
                    firstLine = getLiteralLines(literalPos, firstLine, brTemp, sbTemp);
                    // skipped literal block.  re-scan for markers.
                    comPos = firstLine.indexOf(COMMENT_START);
                    subEndPos = firstLine.indexOf(SUB_END);
                    literalPos = findLiteralMarker(firstLine);
                } else {
                    break;
                }
            }

            // next, strip out any comments
            while (comPos > -1 && (subEndPos < 0 || subEndPos > comPos) && (literalPos < 0 || literalPos > comPos)) {
                int lenBefore = firstLine.length();
                firstLine = stripComment(comPos,firstLine,brTemp);
                int lenAfter = firstLine.length();
                if (lenBefore != lenAfter && firstLine.trim().length() == 0) {
                    skipFirstLine = true;
                }
                // stripped comment lines.  re-scan for markers.
                comPos = firstLine.indexOf(COMMENT_START);
                subEndPos = firstLine.indexOf(SUB_END);
                literalPos = findLiteralMarker(firstLine);
            }
        }

        // keep reading lines until we encounter end marker
        if (subEndPos > -1) {
            // aha, the subtemplate ends on this line.
            sbTemp.append(firstLine.substring(0,subEndPos));
            line = firstLine.substring(subEndPos+SUB_END.length());
            return new Doclet(name,sbTemp.toString());
        } else {
            // subtemplate not finished, keep going
            if (!skipFirstLine) {
                sbTemp.append(firstLine);
                if (brTemp.ready() && firstLine.length() > 0) sbTemp.append("\n");
            }
            while (brTemp.ready()) {
                try {
                    Doclet nested = getNestedTemplate(name, sbTemp);
                    if (nested != null) {
                        return nested;
                    }
                    String line = popLineFromStack();
                    if (line == null) break;
                    if (line == SKIP_BLANK_LINE) continue;

                    sbTemp.append(line);
                    if (brTemp.ready()) sbTemp.append("\n");
                } catch (EndOfSnippetException e) {
                    line = e.getRestOfLine();
                    return new Doclet(name,sbTemp.toString());
                }
            }
            // end of file but with no matching SUB_END? -- wrap it up...
            line = "";
            return new Doclet(name,sbTemp.toString());
        }
    }

    private StringBuilder popBufferFromStack()
    {
        if (bufferStack.size() > 0) {
            return bufferStack.remove(bufferStack.size()-1);
        } else {
            return null;
        }
    }

    private String popLineFromStack()
    {
        return popStringFromStack(lineStack);
    }

    private String popNameFromStack()
    {
        return popStringFromStack(nameStack);
    }

    private String popStringFromStack(ArrayList<String> stack)
    {
        if (stack.size() > 0) {
            return stack.remove(stack.size()-1);
        } else {
            return null;
        }
    }

    private Doclet getNestedTemplate(String name, StringBuilder sbTemp)
        throws IOException, EndOfSnippetException
    {
        String line = brTemp.readLine();
        if (line == null) {
            lineStack.add(null);
            return null;
        }

        int comPos = line.indexOf(COMMENT_START);
        int subPos = line.indexOf(SUB_START);
        int subEndPos = line.indexOf(SUB_END);
        int litPos = findLiteralMarker(line);

        // special handling for literal blocks & comments
        while (litPos > -1 || comPos > -1) {
            // if end-marker present, kick out if it's not inside a comment or a literal block
            if (subEndPos > -1) {
                if ((litPos < 0 || subEndPos < litPos) && (comPos < 0 || comPos < subEndPos)) {
                    break;
                }
            }
            // if start-marker present, kick out if it's not inside a comment or a literal block
            if (subPos > -1) {
                if ((litPos < 0 || subPos < litPos) && (comPos < 0 || comPos < subPos)) {
                    break;
                }
            }
            // first, preserve any literal blocks
            while (litPos > -1 && (comPos < 0 || comPos > litPos)) {
                if (subEndPos < 0 || subEndPos > litPos) {
                    line = getLiteralLines(litPos, line, brTemp, sbTemp);
                    // skipped literal block. re-scan for markers.
                    comPos = line.indexOf(COMMENT_START);
                    subPos = line.indexOf(SUB_START);
                    subEndPos = line.indexOf(SUB_END);
                    litPos = findLiteralMarker(line);
                } else {
                    break;
                }
            }

            // next, skip over any comments
            while (comPos > -1 && (subPos < 0 || subPos > comPos) && (subEndPos < 0 || subEndPos > comPos) && (litPos < 0 || litPos > comPos)) {
                // new plan -- preserve comments, let Snippet strip them out
                line = getCommentLines(comPos, line, brTemp, sbTemp);

                // re-scan for markers
                comPos = line.indexOf(COMMENT_START);
                subPos = line.indexOf(SUB_START);
                subEndPos = line.indexOf(SUB_END);
                litPos = findLiteralMarker(line);
            }
        }

        // keep reading lines until end marker
        if (subPos > -1 || subEndPos > -1) {
            if (subEndPos > -1 && (subPos == -1 || subEndPos <= subPos)) {
                // wrap it up
                sbTemp.append(line.substring(0,subEndPos));
                throw new EndOfSnippetException(line.substring(subEndPos+SUB_END.length()));
            } else if (subPos > -1) {
                int subNameEnd = line.indexOf(SUB_NAME_END, subPos + SUB_START.length());
                if (subNameEnd > -1) {
                    sbTemp.append(line.substring(0,subPos));
                    String subName = line.substring(subPos + SUB_START.length(),subNameEnd);
                    String restOfLine = line.substring(subNameEnd + SUB_NAME_END.length());
                    // RECURSE...
                    bufferStack.add(sbTemp);
                    nameStack.add(name);
                    Doclet nested = nextSubtemplate(name + "#" + subName, restOfLine);
                    // if after removing subtemplate, line is blank, don't output a blank line
                    if (line.length() < 1) lineStack.add(SKIP_BLANK_LINE);
                    return nested;
                }
            }
        }
        lineStack.add(line);
        return null;
    }

    public static StringBuilder expandShorthand(String name, StringBuilder template)
    {
        // do NOT place in cache if ^super directive is found
        // that way, the parent layer will be used instead.
        if (template.indexOf("{^super}") > -1 || template.indexOf("{.super}") > -1) return null;

        // to allow shorthand intra-template references, must pre-process the template
        // at this point and expand any intra-template references, eg:
        //  {~.includeIf(...).#xxx} => {~.includeIf(...).template_name#xxx}
        //
        // Hmm, refs that start with a hash should always be toplevel!
        //  so {#subtemplate}...{~.includeIf(...).#xxx} ...{#}
        //  is a reference to template_name#xxx NOT a nested sub like template_name#subtemplate#xxx
        //
        // might not be worth it, would have to track down refs inside onmatch and ondefined filters
        // although it could be more efficient to expand shorthand syntax at this stage:
        //  {+#sub} => {~.include.template_name#sub}
        //  {+(cond)#sub} => {~.includeIf(cond).template_name#sub}
        //
        // and you'd have to catch stuff like this...
        // {~asdf|onmatch(/xyz/,+#xyz,/abc/,+#abc)nomatch(+#def)}
        //  => {~asdf|onmatch(/xyz/,~.include.template_name#xyz,/abc/,~.include.template_name#abc)nomatch(~.include.template_name#def)}
        //
        // or even just default values a la (I don't even remember, is this supported?)
        //  {~asdf:+#def} => {~asdf:+template_name#def} => {~asdf:~.include.template_name#def}

        // determine what shorthand refs should expand into
        // (template filename is everything up to the first dot)
        String fullRef = name;
        if (fullRef != null) {
            int dotPos = fullRef.indexOf('.');
            if (dotPos > 0) fullRef = name.substring(0,dotPos);
        }

        // restrict search to inside tags
        int cursor = template.indexOf("{");

        while (cursor > -1) {
            if (template.length() == cursor+1) return template; // kick out at first sign of trouble
            char afterBrace = template.charAt(cursor+1);
            if (afterBrace == '+') {
                cursor = expandShorthandInclude(template,fullRef,cursor);
            } else if (afterBrace == '~' || afterBrace == '$') {
                cursor = expandShorthandTag(template,fullRef,cursor);
            } else if (afterBrace == '^' || afterBrace == '.') {
                // check for literal block, and do not perform expansions
                // inside any literal blocks.
                int afterLiteralBlock = skipLiterals(template,cursor);
                if (afterLiteralBlock == cursor) {
                    // ^ is shorthand for ~. eg {^include.#xyz} or {^wiki.External_Content}
                    template.replace(cursor+1,cursor+2,"~.");
                    // re-process, do not advance cursor.
                } else {
                    cursor = afterLiteralBlock;
                }
            } else if (afterBrace == '/') {
                // {/ is short for {^/ which is short for {~./
                template.replace(cursor+1,cursor+2,"~./");
                // re-process, do not advance cursor.
            } else if (afterBrace == '*') {
                cursor = expandShorthandMacro(template,fullRef,cursor);
            } else {
                cursor += 2;
            }
            // on to the next tag...
            if (cursor > -1) cursor = template.indexOf("{",cursor);
        }

        return template;
    }

    private static int expandShorthandInclude(StringBuilder template, String fullRef, int cursor)
    {
        if (template.length() == cursor+2) return -1;

        char afterPlus = template.charAt(cursor+2);
        if (afterPlus == '#') {
            // got one, replace + with long include syntax and fully qualified reference
            template.replace(cursor+1,cursor+2,"~.include."+fullRef);
            cursor += 11; // skip {~.include.
            cursor += fullRef.length(); // skip what we just inserted
            cursor = template.indexOf("}",cursor);
        } else if (afterPlus == '(') {
            // scan to end of condition
            int endCond = nextUnescapedDelim(")",template,cursor+3);
            if (endCond < 0) return -1; // kick out at any sign of trouble
            String cond = template.substring(cursor+2,endCond+1);
            if (template.length() == endCond+1) return -1;
            if (template.charAt(endCond) == '#') {
                // got one, replace +(cond) with long includeIf syntax and FQRef
                String expanded = "~.includeIf"+cond+"."+fullRef;
                template.replace(cursor+1,endCond+1,expanded);
                cursor++; // skip {
                cursor += expanded.length();
                cursor = template.indexOf("}",cursor);
            }
        } else {
            // move along, nothing to expand here.
            cursor += 2;
        }

        return cursor;
    }

    private static int expandShorthandTag(StringBuilder template, String fullRef, int cursor)
    {
        int tagEnd = nextUnescapedDelim("}",template,cursor+2);
        if (tagEnd < 0) return -1; // kick out at any sign of trouble

        // so, this is lame but 99.999% of the time the following strings
        // inside a tag body can be expanded correctly without regard to context:
        //
        //  ,+# => ,~.include.xxx# - inside onmatch
        //  :+# => :~.include.xxx# - ifnull-include
        //  (+# => (~.include.xxx# - inside nomatch/ondefined
        //  ).# => ).xxx# => - long includeIf(...) syntax
        //  ~.include.# => ~.include.xxx# - long include syntax
        //
        // where xxx is the fully qualified template reference
        //
        // not the most efficient, but fast enough
        //
        String tagDirective = template.substring(cursor+2,tagEnd);

        //
        // shorthand refs in fnCall args like ^loop(...) and ^grid(...)
        // will present a little differently.
        //
        if (tagDirective.startsWith(".loop") || tagDirective.startsWith(".grid")) {
            return expandFnArgs(template, fullRef, cursor, tagDirective, tagEnd);
        }

        int tagCursor = 0;
        StringBuilder expanded = null;

        int hashPos = tagDirective.indexOf("#");

        while (hashPos > 1) {
            char a = tagDirective.charAt(hashPos-2);
            char b = tagDirective.charAt(hashPos-1);
            if (b == '+') {
                if (a == ',' || a == ':' || a == '(') {
                    if (expanded == null) expanded = new StringBuilder();
                    expanded.append(tagDirective.substring(tagCursor,hashPos-1));
                    expanded.append("~.include.");
                    expanded.append(fullRef);
                    tagCursor = hashPos;
                }
            } else if ((a == ')' || a == 'e' || a == 'c') && (b == '.' || b == ' ')) {
                // e for include, c for exec
                if (expanded == null) expanded = new StringBuilder();
                expanded.append(tagDirective.substring(tagCursor,hashPos));
                expanded.append(fullRef);
                tagCursor = hashPos;
            } else if (b == '(' && a == 'r') {
                // {$tag|filter(#ref)}
                if (expanded == null) expanded = new StringBuilder();
                expanded.append(tagDirective.substring(tagCursor,hashPos));
                expanded.append(fullRef);
                tagCursor = hashPos;
            }

            hashPos = tagDirective.indexOf("#",hashPos+1);
        }
        if (expanded != null) {
            expanded.append(tagDirective.substring(tagCursor));
            String expandedTag = expanded.toString();
            template.replace(cursor+2,tagEnd,expandedTag);
            // update tagEnd to reflect added chars
            tagEnd += (expandedTag.length() - tagDirective.length());
        }
        cursor = tagEnd+1;

        return cursor;
    }

    private static int expandFnArgs(StringBuilder template, String fullRef, int cursor, String fnCall, int tagEnd)
    {
        int tagCursor = 0;
        StringBuilder expanded = null;

        int hashPos = fnCall.indexOf("#");

        // let's just (lame but works) assume that all hashes after a
        // delimiter are hashrefs that need to be expanded
        while (hashPos > 1) {
            char preH = fnCall.charAt(hashPos-1);
            if (preH == '"' || preH == ',' || preH == ' ' || preH == '(') {
                if (expanded == null) expanded = new StringBuilder();
                // everything new up to now is certified "clean"
                expanded.append(fnCall.substring(tagCursor,hashPos));
                // pop in the base template ref
                expanded.append(fullRef);
                tagCursor = hashPos;
            }

            hashPos = fnCall.indexOf("#",hashPos+1);

        }

        if (expanded != null) {
            // grab the tail
            expanded.append(fnCall.substring(tagCursor));
            String expandedTag = expanded.toString();
            // insert tag, now with fully-qualified refs back into template
            template.replace(cursor+2,tagEnd,expandedTag);
            // update tagEnd to reflect added chars
            tagEnd += (expandedTag.length() - fnCall.length());
        }
        cursor = tagEnd+1;

        return cursor;
    }

    private static int skipLiterals(StringBuilder template, int cursor)
    {
        int wall = template.length();
        int shortLen = LITERAL_SHORTHAND.length();
        int scanStart = cursor;
        if (cursor + shortLen <= wall && template.substring(cursor,cursor+shortLen).equals(LITERAL_SHORTHAND)) {
            scanStart = cursor + shortLen;
        } else {
            int longLen = LITERAL_START2.length();
            if (cursor + longLen <= wall && template.substring(cursor,cursor+longLen).equals(LITERAL_START2)) {
                scanStart = cursor + longLen;
            } else {
                longLen = LITERAL_START.length();
                if (cursor + longLen <= wall && template.substring(cursor,cursor+longLen).equals(LITERAL_START)) {
                    scanStart = cursor + longLen;
                }
            }
        }

        if (scanStart > cursor) {
            // found a literal-block start marker.  scan for the matching end-marker.
            int tail = template.indexOf(LITERAL_END, scanStart);
            int longTail = template.indexOf(LITERAL_END_LONGHAND, scanStart);
            tail = (tail < 0) ? longTail : (longTail < 0) ? tail : Math.min(tail, longTail);
            if (tail < 0) {
                return wall;
            } else {
                return tail + (tail == longTail ? LITERAL_END_LONGHAND.length() : LITERAL_END.length());
            }
        } else {
            return cursor;
        }
    }

    private static int expandShorthandMacro(StringBuilder template, String fullRef, int cursor)
    {
        int offset = 2;
        while (template.charAt(cursor+offset) == ' ') offset++;

        if (template.charAt(cursor+offset) == '#') {
            template.insert(cursor+offset,fullRef);
            int macroMarkerEnd = template.indexOf(MACRO_NAME_END,cursor+offset+fullRef.length()+1);
            if (macroMarkerEnd < 0) return cursor+1;
            return macroMarkerEnd + MACRO_NAME_END.length();
        }
        int macroMarkerEnd = template.indexOf(MACRO_NAME_END,cursor+offset);
        if (macroMarkerEnd < 0) return cursor+1;
        return macroMarkerEnd + MACRO_NAME_END.length();
    }

    public static int nextUnescapedDelim(String delim, StringBuilder sb, int searchFrom)
    {
        int delimPos = sb.indexOf(delim, searchFrom);

        boolean isProvenDelimeter = false;
        while (!isProvenDelimeter) {
            // count number of backslashes that precede this forward slash
            int bsCount = 0;
            while (delimPos-(1+bsCount) >= searchFrom && sb.charAt(delimPos - (1+bsCount)) == '\\') {
                bsCount++;
            }
            // if odd number of backslashes precede this delimiter char, it's escaped
            // if even number precede, it's not escaped, it's the true delimiter
            // (because it's preceded by either no backslash or an escaped backslash)
            if (bsCount % 2 == 0) {
                isProvenDelimeter = true;
            } else {
                // keep looking for real delimiter
                delimPos = sb.indexOf(delim, delimPos+1);
                // if the expr is not legal (missing delimiters??), bail out
                if (delimPos < 0) return -1;
            }
        }
        return delimPos;
    }

    public boolean hasNext()
    {
        if (queued != null) {
            return true;
        } else {
            try {
                queued = nextTemplate();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
            return queued != null;
        }
    }

    public Doclet next()
    {
        if (queued != null) {
            Doclet nextDoc = queued;
            queued = null;
            return nextDoc;
        } else {
            try {
                return nextTemplate();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
            return null;
        }
    }

    public void remove()
    {
    }

    public Iterator<Doclet> iterator()
    {
        return this;
    }

    static String getDefaultEncoding()
    {
        // can use system env var to specify default encoding
        // other than UTF-8
        String override = System.getProperty("chunk.template.charset");
        if (override != null) {
            if (override.equalsIgnoreCase("SYSTEM")) {
                // use system default charset
                return Charset.defaultCharset().toString();
            } else {
                return override;
            }
        } else {
            // default is UTF-8
            return "UTF-8";
        }
    }

}
