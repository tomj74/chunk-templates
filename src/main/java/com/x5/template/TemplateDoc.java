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
import java.util.regex.Matcher;

public class TemplateDoc implements Iterator<TemplateDoc.Doclet>, Iterable<TemplateDoc.Doclet>
{
    private static final String COMMENT_START = "{!--";
    private static final String COMMENT_END = "--}";

    public static final String LITERAL_START = "{^literal}";
    public static final String LITERAL_START2 = "{.literal}";
    public static final String LITERAL_SHORTHAND = "{^^}"; // this was a dumb idea
    public static final String LITERAL_END = "{^}";
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
        private String origin;

        public Doclet(String name, String rawTemplate, String origin)
        {
            this.name = name;
            this.rawTemplate = rawTemplate;
            this.origin = origin;
        }

        public String getName()
        {
            return name;
        }

        public String getTemplate()
        {
            return rawTemplate;
        }

        public String getOrigin()
        {
            return origin;
        }

        public Snippet getSnippet()
        {
            return Snippet.getSnippet(rawTemplate, origin);
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
            stub = name.substring(slashPos+1);
        } else {
            stub = name;
        }

        int hashPos = stub.indexOf("#");
        if (hashPos > -1) stub = stub.substring(0, hashPos);

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
                // There might not be a newline at EOF but it's safer to just add one anyway.
                // If someone has a burning need for a snippet that doesn't end in a newline,
                // they can achieve this via a {#subtemplate}with no trailing linefeed{#}
                rootTemplate.append("\n");
            }
            if (subtpl != null) {
                return subtpl;
            }
        }
        String root = rootTemplate.toString();
        rootTemplate = null;
        return new Doclet(stub, root, stub);
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
        Matcher m = LITERAL_CLOSE.matcher(firstLine);
        if (m.find(litBegin+2)) {
            // easy case -- literal does not span lines
            int litEnd = m.end();
            sbTemp.append(firstLine.substring(0, litEnd));
            return firstLine.substring(litEnd);
        } else {
            sbTemp.append(firstLine);
            sbTemp.append("\n");
            // multi-line literal, keep appending until we encounter literal-end marker
            String line = null;
            while (brTemp.ready()) {
                line = brTemp.readLine();
                if (line == null) break;

                m.reset(line);
                if (m.find()) {
                    int litEnd = m.end();
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
        Matcher m = LITERAL_OPEN_ANYWHERE.matcher(text);
        if (m.find(startAt)) {
            return m.start();
        } else {
            return -1;
        }
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
            return new Doclet(name, sbTemp.toString(), stub);
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
                    return new Doclet(name, sbTemp.toString(), stub);
                }
            }
            // end of file but with no matching SUB_END? -- wrap it up...
            line = "";
            return new Doclet(name, sbTemp.toString(), stub);
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

    private static final java.util.regex.Pattern SUPER_TAG =
            java.util.regex.Pattern.compile("\\{\\% *super *\\%?\\}");

    public static StringBuilder expandShorthand(String name, StringBuilder template)
    {
        // do NOT place in cache if ^super directive is found
        // that way, the parent layer will be used instead.
        if (template.indexOf("{^super}") > -1 || template.indexOf("{.super}") > -1) return null;
        Matcher m = SUPER_TAG.matcher(template);
        if (m.find()) return null;

        // restrict search to inside tags
        int cursor = template.indexOf("{");

        while (cursor > -1) {
            if (template.length() == cursor+1) return template; // kick out at first sign of trouble
            char afterBrace = template.charAt(cursor+1);
            if (afterBrace == '^' || afterBrace == '.' || afterBrace == '%') {
                // check for literal block, and do not perform expansions
                // inside any literal blocks.
                int afterLiteralBlock = skipLiterals(template,cursor);
                if (afterLiteralBlock != cursor) {
                    // ooh, skipped a literal
                    cursor = afterLiteralBlock;
                } else {
                    if (afterBrace != '%') {
                        // . is shorthand for ~. eg {.include #xyz} or {.wiki.External_Content}
                        template.replace(cursor+1,cursor+2,"~.");
                    } else {
                        // trim whitespace from expression eg: {%    $tag    %}
                        int exprStart = cursor + 2;
                        while (exprStart < template.length() && Character.isWhitespace(template.charAt(exprStart))) {
                            exprStart++;
                        }
                        afterBrace = template.charAt(exprStart);
                        if (Snippet.MAGIC_CHARS.indexOf(afterBrace) < 0) {
                            // assume '.' directive, no magic char was found
                            template.replace(exprStart,exprStart,"~.");
                            afterBrace = '~';
                        }
                    }
                    cursor += 2;
                }
            } else if (afterBrace == '/') {
                // {/ is short for {./ which is short for {~./
                template.replace(cursor+1,cursor+2,"~./");
                // re-process, do not advance cursor.
            } else {
                cursor += 2;
            }
            // on to the next tag...
            if (cursor > -1) cursor = template.indexOf("{",cursor);
        }

        return template;
    }

    private static final String LITERAL_OPEN = "(\\{\\^\\^\\}|\\{[\\.\\^]literal\\}|\\{\\% *literal *\\%?\\})";
    private static final java.util.regex.Pattern LITERAL_OPEN_HERE =
            java.util.regex.Pattern.compile("\\G" + LITERAL_OPEN);
    private static final java.util.regex.Pattern LITERAL_OPEN_ANYWHERE =
            java.util.regex.Pattern.compile(LITERAL_OPEN);
    private static final java.util.regex.Pattern LITERAL_CLOSE =
            java.util.regex.Pattern.compile("(\\{\\^\\}|\\{/literal\\}|\\{\\% *endliteral *\\%?\\})");

    private static int skipLiterals(StringBuilder template, int cursor)
    {
        int scanStart = cursor;
        Matcher m = LITERAL_OPEN_HERE.matcher(template);
        if (m.find(scanStart)) {
            scanStart = m.end();
        }

        if (scanStart > cursor) {
            // scan for closing tag
            m = LITERAL_CLOSE.matcher(template);
            if (m.find(scanStart)) {
                return m.end();
            } else {
                return template.length();
            }
        } else {
            return cursor;
        }
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
