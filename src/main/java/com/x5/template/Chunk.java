package com.x5.template;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.Map;
import java.util.regex.Matcher;

import com.x5.template.filters.Calc;
import com.x5.util.DataCapsule;
import com.x5.util.DataCapsuleReader;
import com.x5.util.ObjectDataMap;
import com.x5.util.TableData;

// Project Title: Chunk
// Description: Template Engine
// Copyright: Waived. Use freely.
// Author: Tom McClure

/**
 * <P>
 * Chunk is part Hashtable, part StringBuilder, part find-and-replace.
 *
 * <P>
 * Assign an initial template (you can stitch on bits of additional template<BR>
 * content as you go) with placeholder tags -- eg {$my_tag} -- and then<BR>
 * set up replacement rules for those tags like so:
 *
 * <PRE>
 *    TemplateSet templates = getTemplates(); // defined elsewhere
 *    Chunk myChunk = templates.makeChunk("my_template");
 *    myChunk.set("my_tag","hello tag");
 *    System.out.print( myChunk.toString() );
 * </PRE>
 *
 * <P>
 * <B>NB: Template {$tags} are bounded by curly brackets, not (parentheses).<BR>
 * Be careful to always close off {#sub_templates}bla bla bla{#} with a single<BR>
 * hash mark surrounded by curly brackets/braces.
 *
 * <P>
 * TemplateSet is handy if you have a folder with lots of html templates.<BR>
 * Here's an even simpler example, where the template string is supplied<BR>
 * without using TemplateSet:
 *
 * <PRE>
 *    String templateBody = "Hello {$name}!  Your balance is ${$balance}."
 *         + "Pleasure serving you, {$name}!";
 *    Chunk myChunk = new Chunk();
 *
 *    // .append() and .set() may be called in any order
 *    // because tag replacement is delayed until the .toString() call.
 *    myChunk.append( templateBody );
 *    myChunk.set("name", user.getName());
 *    myChunk.set("balance", user.getBalance());
 *    System.out.println( myChunk.toString() );
 *
 *    // reset values and re-use -- original templates are not modified
 *    // when .toString() output is generated.
 *    myChunk.set("name", user2.getName());
 *    myChunk.set("balance", user2.getBalance());
 *    System.out.println( myChunk.toString() );
 *
 * </PRE>
 *
 * <P>
 * The .toString() method transparently invokes the find and replace<BR>
 * functionality.
 *
 * <P>
 * FREQUENTLY ASKED QUESTIONS
 *
 * <P>
 * <B>Q: If I name things just right, will subtemplates get automatically<BR>
 * connected to like-named tags?</B>  ie, if I write a template file like this:
 *
 * <PRE>
 * bla bla bla {$myTemplate} foo foo foo
 * {#myTemplate}Hello {$name}!{#}
 * </PRE>
 *
 * <P>
 * A: No*.  To keep things simple and reduce potential for confusion, Chunk<BR>
 * does not auto-magically fill any tags based on naming conventions or<BR>
 * in-template directives.  You must explicitly invoke the "include command:<BR>
 *
 *   bla bla bla {.include #myTemplate} foo foo foo
 *
 * <P>* Actually, this documentation is outdated, and several extensions to the<BR>
 * original template syntax are now available:
 *
 * <P>There is now a powerful new tag modifier syntax which supports:<BR>
 *           * providing default values for tags in-template<BR>
 *           * automating template placement with "includes"<BR>
 *           * in-template text filters including perl-style regex and sprintf<BR>
 *           * macro-style templating<BR>
 *           * extending the system to access alternate template repositories<BR>
 *
 * <P>Complete details are here: <a href="http://www.x5software.com/chunk/">http://www.x5software.com/chunk</a>
 *
 * <P>
 * <B>Q: My final output says "infinite recursion detected."  What gives?</B>
 *
 * <P>
 * A: You did some variation of this:
 * <PRE>
 *   TEMPLATE:
 *     bla bla bla {$name}
 *     {#name_info}My name is {$name}{#}
 *
 *   CODE:
 *     ...set("name", theme.getSnippet("file#name_info"));
 *     ...toString();
 * </PRE>
 *
 * <P>
 * The outer template gets its {$name} tag replaced with "My name is {$name}" --<BR>
 * then, that replacement value is scanned for any tags that might need to be<BR>
 * swapped out for their values.  It finds {$name} and, using the rule you gave,<BR>
 * replaces it with "My name is {$name}" so we now have My name is My name is ...<BR>
 * ad infinitum.
 *
 * <P>
 * This situation is detected by assuming recursion depth will not normally go<BR>
 * deeper than 17.  If you legitimately need to nest templates that deep, you<BR>
 * can flatten out the recursion by doing a .toString() expansion partway<BR>
 * through the nest OR you can tweak the depth limit value in the Chunk.java<BR>
 * source code.
 *
 * <P>
 * <B>Q: Where did my subtemplates go?</B>
 *
 * <P>
 * A: TemplateSet parses out subtemplates and leaves no trace of them in the<BR>
 * outer template where they were defined.  Some people are surprised when<BR>
 * no placeholder tag is automagically generated and left in place of the<BR>
 * subtemplate definition -- sorry, this is not the convention.  For optional<BR>
 * elements the template/code usually looks like this:
 *
 * <PRE>
 *   TEMPLATE:
 *     bla bla bla
 *     {$memberMenu:}
 *     {#member_menu}this that etc{#}
 *     foo foo foo
 *
 *   CODE:
 *     if (isLoggedIn) {
 *          myChunk.set("memberMenu", theme.getSnippet("file#member_menu"));
 *     }
 * </PRE>
 *
 * <P>
 * The subtemplate does not need to be defined right next to the tag where<BR>
 * it will be used (although that practice does promote readability).
 *
 * <P>
 * <B>Q: Are tag names and subtemplate names case sensitive?</B>
 *
 * <P>
 * A: Yes. I prefer to use mixed case in {$tagNames} with first letter<BR>
 * lowercase.  In my experience this aids readability since tags are similar<BR>
 * to java variables in concept and that is the java case convention for<BR>
 * variables.   Similarly, I prefer lowercase with underscores for all<BR>
 * {#sub_template_names}{#} since templates tend to be defined within html<BR>
 * files which are typically named in all lowercase.
 *
 * <P>
 * ADVANCED USES
 *
 * <P>
 * Chunk works great for the simple examples above, but Chunk can do<BR>
 * so much more... the find-and-replace alg is recursive, which is<BR>
 * extremely handy once you get the hang of it.
 *
 * <P>
 * Internally it resists creating an expensive Hashtable until certain<BR>
 * threshhold conditions are reached.
 *
 * <P>
 * Output can be constructed on-the-fly with .append() -- say<BR>
 * you're building an HTML table but don't know ahead of time how<BR>
 * many rows and columns it will contain, just loop through all<BR>
 * your data with calls to .append() after preparing each cell and row:
 *
 * <PRE>
 *    Chunk table = templates.makeChunk("my_table");
 *    Chunk rows = templates.makeChunk();
 *    Chunk row = templates.makeChunk("my_table.my_row");
 *    Chunk cell = templates.makeChunk("my_table.my_cell");
 *
 *    rows.set("background_color", getRowColor() );
 *
 *    while (dataSet.hasMoreData()) {
 *        DataObj data = dataSet.nextDataObj();
 *        String[] attributes = data.getAttributes();
 *
 *        StringBuilder cells = new StringBuilder();
 *        for (int i=0; i &lt; attributes.length; i++) {
 *            cell.set("cellContent", attributes[i]);
 *            cells.append( cell.toString() );
 *        }
 *
 *        row.set("name", data.getName());
 *        row.set("id", data.getID());
 *        row.set("cells",cells);
 *
 *        rows.append( row.toString() );
 *    }
 *
 *    table.set("table_rows",rows);
 *    System.out.println( table.toString() );
 * </PRE>
 *
 * <PRE>
 * Possible contents of my_table.html:
 *
 * &lt;TABLE&gt;
 * {$table_rows}
 * &lt;/TABLE&gt;
 *
 * {#my_row}
 * &lt;TR bgcolor="{$background_color}"&gt;
 *  &lt;TD&gt;{$id} - {$name}&lt;/TD&gt;
 *  {$cells}
 * &lt;/TR&gt;
 * {#}
 *
 * {#my_cell}
 * &lt;TD&gt;{$cell_content}&lt;/TD&gt;
 * {#}
 * </PRE>
 *
 * Copyright: waived, free to use<BR>
 * Company: <A href="http://www.x5software.com/">X5 Software</A><BR>
 * Updates: <A href="http://www.x5software.com/chunk/">Chunk Documentation</A><BR>
 *
 * @author Tom McClure
 * @version 2.6.5
 */

public class Chunk implements Map<String,Object>
{
    public static final int HASH_THRESH = 8;
    public static final int DEPTH_LIMIT = 17;

    public static final String VERSION = "2.6.5";

    private static final String TRUE = "TRUE";

    protected Snippet templateRoot = null;
    private String templateOrigin = null;
    private String[] firstTags = new String[HASH_THRESH];
    private Object[] firstValues = new Object[HASH_THRESH];
    private int tagCount = 0;
    protected Vector<Snippet> template = null;
    private Hashtable<String,Object> tags = null;
    protected String tagStart = TemplateSet.DEFAULT_TAG_START;
    protected String tagEnd = TemplateSet.DEFAULT_TAG_END;

    private Vector<Vector<Chunk>> contextStack = null;

    private ContentSource macroLibrary = null;
    private ChunkFactory chunkFactory = null;

    private String localeCode = null;
    private ChunkLocale locale = null;

    // print errors to output?
    private boolean renderErrs = true;
    private PrintStream errLog = null;

    // package visibility
    void setMacroLibrary(ContentSource repository, ChunkFactory factory)
    {
        this.macroLibrary = repository;
        if (altSources != null) {
            addProtocol(repository);
        }
        this.chunkFactory = factory;
    }

    public ContentSource getTemplateSet()
    {
        return this.macroLibrary;
    }

    public void setChunkFactory(ChunkFactory factory)
    {
        this.chunkFactory = factory;
    }

    public ChunkFactory getChunkFactory()
    {
        return chunkFactory;
    }

    public void append(Snippet toAdd)
    {
        // don't bother with overhead of vector until necessary
        if (templateRoot == null && template == null) {
            templateRoot = toAdd;
        } else {
            if (template == null) {
                template = new Vector<Snippet>();
                template.addElement(templateRoot);
                template.addElement(toAdd);
            } else {
                template.addElement(toAdd);
            }
        }
    }

    /**
     * Add a String on to the end a Chunk's template.
     */
    public void append(String toAdd)
    {
        if (toAdd == null) return;

        // parse string into snippets-to-process and literals etc.
        Snippet snippet = Snippet.getSnippet(toAdd);
        append(snippet);
    }

    /**
     * Add a Chunk on to the end of a Chunk's "template" -- this "child"
     * Chunk won't get it's .toString() invoked until the parent Chunk's
     * tags are replaced, ie when the parent Chunk's .toString() method
     * is invoked.  The child does NOT get cloned in this call so if
     * you append a chunk, alter its tags and append it again, you'll
     * see it in its final state twice after the template expansion.
     * To keep incremental changes, use append(child.toString()) instead.
     */
    public void append(Chunk toAdd)
    {
        // if we're adding a chunk we'll almost definitely add more than one.
        // switch to vector
        if (template == null) {
            template = new Vector<Snippet>();
            if (templateRoot != null) template.addElement(templateRoot);
        }
        // internally, we stash in tag table and wrap in Snippet.
        String chunkKey = ";CHUNK_" + toAdd.hashCode();
        set(chunkKey,toAdd);
        String autoTag = makeTag(chunkKey);
        template.addElement(Snippet.getSnippet(autoTag));
    }

    /**
     * Creates a find-and-replace rule for tag replacement.  Overwrites any
     * previous rules for this tagName.  Do not include the tag boundary
     * markers in the tagName, ie
     * GOOD: set("this", "that")
     * BAD: set("{$this}", "that")
     *
     * @param tagName will be ignored if null.
     * @param tagValue will be translated to the empty String if null -- use setOrDelete() instead of set() if you don't need/want this behavior.
     */
    public void set(String tagName, String tagValue)
    {
        set(tagName, tagValue, "");
    }

    /**
     * Creates a find-and-replace rule for tag replacement.  See remarks
     * at append(Chunk c).
     * @param tagName will be ignored if null.
     * @param tagValue will be translated to the empty String if null.
     * @see #append(Chunk)
     */
    public void set(String tagName, Chunk tagValue)
    {
        set(tagName, tagValue, "");
    }

    /**
     * Convenience method, chains to set(String s, Object o, String ifNull)
     */
    public void set(String tagName, Object tagValue)
    {
        String ifNull = null;
        set(tagName, tagValue, ifNull);
    }

    /**
     * Careful, setOrDelete will DELETE a previous value
     * for the tag at this level if passed a null value.
     *
     * This is a way around the standard behavior which interprets
     * null values as the empty string.
     *
     * The default is to use the empty string for null object/strings.
     * setOrDelete provides an alternate option, leaving the tag "unset"
     * in case the value is null, which allows the template to provide
     * its own default value.
     */
    public void setOrDelete(String tagName, Object tagValue)
    {
        if (tagValue == null) {
            // containsKey handy side effect -- converts to hashtable if nec.
            if (this.containsKey(tagName)) {
                // unset!!
                tags.remove(tagName);
            }
            return;
        } else {
            set(tagName, tagValue, null);
        }
    }

    /**
     * setLiteral() tag values will render verbatim, so even if the value
     * contains tags/specials they will not be expanded.  However, if
     * the final engine output is passed back into the chunk processor
     * as a static string (for example, using chunk to pre-generate table
     * rows that are destined for placement in another chunk), it will not
     * be protected from the engine in that second pass.
     *
     * To prevent re-processing higher up the chain, encase your string
     * in {% literal %}...{% endliteral %} tags, with the tradeoff that
     * they will appear in your final output.  This is by design, so the
     * literal will be preserved even after multiple passes through the
     * engine.
     *
     * Typical workaround: <!-- {% literal %} --> ... <!-- {% endliteral %} -->
     *
     * Or, just be super-careful to use setLiteral() again when placing
     * pre-processed output into higher-level chunks.
     *
     * @param tagName
     * @param literalValue
     */
    public void setLiteral(String tagName, String literalValue)
    {
        Snippet hardValue = Snippet.makeLiteralSnippet(literalValue);
        set(tagName, hardValue);
    }

    /**
     * Create a tag replacement rule, supplying a default value in case
     * the value passed is null.  If both the tagValue and the fallback
     * are null, the rule created will resolve all instances of the tag
     * to the string "NULL"
     * @param tagName tag to replace
     * @param tagValue replacement value -- no-op unless this is of type String or Chunk.
     * @param ifNull fallback replacement value in case tagValue is null
     */
    public void set(String tagName, Object tagValue, String ifNull)
    {
        // all "set" methods eventually chain to here
        if (tagName == null) return;
        // ensure that tagValue is either a String or a Chunk (or some tabular data)
        if (tagValue != null) {
            tagValue = coercePrimitivesToString(tagValue);
            if (tagValue instanceof Chunk || tagValue instanceof TableData) {
                // don't treat chunk or tabledata as a Map
            } else if (tagValue instanceof Map) {
                // great, a map!
            } else if (!(tagValue instanceof String
                    || tagValue instanceof Snippet
                    || tagValue instanceof List
                    || tagValue instanceof Object[])) {
                // force to map
                tagValue = new ObjectDataMap(tagValue);
            }
        }
        if (tagValue == null) {
            tagValue = (ifNull == null) ? "NULL" : ifNull;
        }
        if (tags != null) {
            tags.put(tagName,tagValue);
        } else {
            // sequential scan is kinda inefficient but
            // completely acceptable for small datasets
            for (int i=0; i<tagCount; i++) {
                if (firstTags[i].equals(tagName)) {
                    // supplying new value for existing tag
                    firstValues[i] = tagValue;
                    return;
                }
            }
            if (tagCount >= HASH_THRESH) {
                // threshhold reached, upgrade to hashtable
                tags = new Hashtable<String,Object>(HASH_THRESH * 2);
                copyToHashtable();
                tags.put(tagName,tagValue);
            } else {
                firstTags[tagCount] = tagName;
                firstValues[tagCount] = tagValue;
                tagCount++;
            }
        }
    }

    /**
     * Make bean properties available to template
     */
    public void setToBean(String tagName, Object bean)
    {
        setToBean(tagName, bean, null);
    }

    /**
     * Make bean properties available to template
     */
    @SuppressWarnings("rawtypes")
    public void setToBean(String tagName, Object bean, String ifNull)
    {
        Map boxedBean = ObjectDataMap.wrapBean(bean);
        set(tagName, boxedBean, ifNull);
    }

    /**
     * For convenience, sets a flag to "TRUE" - to reverse, call unset("flag")
     */
    public void set(String tagName)
    {
        set(tagName, TRUE);
    }

    /**
     * For convenience, auto-converts int to String and creates
     * tag replacement rule.  Overwrites any existing rule with this tagName.
     */
    public void set(String tagName, int tagValue)
    {
        set(tagName, Integer.toString(tagValue));
    }

    /**
     * For convenience, auto-converts char to String and creates
     * tag replacement rule.  Overwrites any existing rule with this tagName.
     */
    public void set(String tagName, char tagValue)
    {
        set(tagName, Character.toString(tagValue));
    }

    /**
     * For convenience, auto-converts long to String and creates
     * tag replacement rule.  Overwrites any existing rule with this tagName.
     */
    public void set(String tagName, long tagValue)
    {
        set(tagName, Long.toString(tagValue));
    }

    /**
     * For convenience, auto-converts StringBuilder to String and creates
     * tag replacement rule.  Overwrites any existing rule with this tagName.
     */
    public void set(String tagName, StringBuilder tagValue)
    {
        if (tagValue != null) set(tagName, tagValue.toString());
    }

    /**
     * For convenience, auto-converts StringBuffer to String and creates
     * tag replacement rule.  Overwrites any existing rule with this tagName.
     */
    public void set(String tagName, StringBuffer tagValue)
    {
        if (tagValue != null) set(tagName, tagValue.toString());
    }

    /**
     * unset("tag") deletes the named tag expansion rule from the ruleset.
     * @param tagName
     */
    public void unset(String tagName)
    {
        if (tagName != null) { setOrDelete(tagName, null); }
    }

    /**
     * @return true if a rule exists for this tagName, otherwise false.
     * Returns false if tagName is null.
     */
    public boolean hasValue(String tagName)
    {
        if (tagName == null) return false;
        if (tags != null) {
            return tags.containsKey(tagName);
        } else {
            for (int i=0; i<tagCount; i++) {
                if (firstTags[i].equals(tagName)) return true;
            }
            return false;
        }
    }


    /**
     * @return true if a rule does not yet exist for this tagName, otherwise
     * false. Returns false if tagName is null.
     */
    public boolean stillNeeds(String tagName)
    {
        if (tagName == null) return false;
        return !hasValue(tagName);
    }

    /**
     * Apply all tag replacement rules recursively and return template
     * contents with translated tags.  Rules and original template pieces
     * remain intact, so toString can be called several times, modifying
     * rules between each invocation to produce a slightly different output
     * each time.
     * @return A String with all template pieces assembled and all known tags recursively resolved.
     */
    public String toString()
    {
        StringWriter out = new StringWriter();
        try {
            render(out);
            out.flush();
            return out.toString();
        } catch (IOException e) {
            return e.getLocalizedMessage();
        }
    }

    public String toString(Chunk context)
    {
        StringWriter out = new StringWriter();
        try {
            render(out, context);
            out.flush();
            return out.toString();
        } catch (IOException e) {
            return e.getLocalizedMessage();
        }
    }

    public void render(PrintStream out)
    throws IOException
    {
        java.io.PrintWriter writer = new java.io.PrintWriter(out);
        render(writer);
        writer.flush();
    }

    public void render(Writer out)
    throws IOException
    {
        explodeForParentToPrinter(out, null);
    }

    public void render(Writer out, Chunk context)
    throws IOException
    {
        // sometimes orphaned chunks need to be reunited with their ancestry
        Vector<Chunk> parentContext = context.prepareParentContext();
        explodeForParentToPrinter(out, parentContext);
    }

    private void pushContextStack(Vector<Chunk> parentContext)
    {
        if (contextStack == null) {
            contextStack = new Vector<Vector<Chunk>>();
        }
        contextStack.insertElementAt(parentContext, 0);
    }

    private void popContextStack()
    {
        if (contextStack == null || contextStack.size() == 0) return;
        contextStack.removeElementAt(0);
    }

    private void explodeForParentToPrinter(Writer out, Vector<Chunk> ancestors)
    throws IOException
    {
        if (template == null && templateRoot == null) return;

        if (ancestors != null) {
            // PUSH ANCESTORS ONTO STACK AND LOCK DOWN
            synchronized(this) {
                pushContextStack(ancestors);
                renderForParentToPrinter(out);
                popContextStack();
            }
        } else {
            renderForParentToPrinter(out);
        }
    }

    private void renderForParentToPrinter(Writer out)
    throws IOException
    {
        if (template == null) {
            explodeToPrinter(out, templateRoot, 1);
        } else {
            // If template was constructed incrementally, with several .append(...) calls,
            // some block-open tags might not be grouped with the matching block-closed tag.
            // Merge templates together into a single Snippet if possible.
            if (template.size() > 1) {
                template = mergeTemplateParts();
            }
            for (int i=0; i < template.size(); i++) {
                Snippet s = template.elementAt(i);
                explodeToPrinter(out, s, 1);
            }
        }
    }

    private Vector<Snippet> mergeTemplateParts()
    {
        Snippet merged;
        try {
            merged = Snippet.consolidateSnippets(template);
        } catch (EndOfSnippetException e) {
            return template;
        }
        Vector<Snippet> newTemplate = new Vector<Snippet>();
        newTemplate.add(merged);
        return newTemplate;
    }

    void explodeToPrinter(Writer out, Object obj, int depth)
    throws IOException
    {
        if (depth >= DEPTH_LIMIT) {

            String err = handleError("[**ERR** max template recursions: "+DEPTH_LIMIT+"]");
            if (err != null) out.append(err);

        } else if (obj instanceof Snippet) {

            Snippet snippet = (Snippet)obj;
            snippet.render(out, this, depth);

        } else if (obj instanceof String) {

            // snippet-ify to catch/skip literal blocks
            Snippet snippet = Snippet.getSnippet((String)obj);
            explodeToPrinter(out, snippet, depth);

        } else if (obj instanceof Chunk) {

            Vector<Chunk> parentContext = prepareParentContext();
            Chunk c = (Chunk) obj;
            c.explodeForParentToPrinter(out, parentContext);

        } else if (obj instanceof DataCapsule[]) {

            // auto-expand?
            DataCapsuleReader reader = DataCapsuleReader.getReader((DataCapsule[])obj);
            String err = handleError("[LIST("+reader.getDataClassName()+") - Use a loop construct to display list data.]");
            if (err != null) out.append(err);

        } else if (obj instanceof String[]) {

            String err = handleError("[LIST(java.lang.String) - Use a loop construct to display list data, or pipe to join().]");
            if (err != null) out.append(err);

        } else if (obj instanceof List) {

            String err = handleError("[LIST - Use a loop construct to display list data, or pipe to join().]");
            if (err != null) out.append(err);

        } else {

            // implicit cast to string
            String repr = ObjectDataMap.getAsString(obj);
            explodeToPrinter(out, repr, depth);

        }
    }

    @SuppressWarnings("unchecked")
    private Vector<Chunk> prepareParentContext()
    {
        if (contextStack == null) {
            Vector<Chunk> parentContext = new Vector<Chunk>();
            parentContext.add(this);
            return parentContext;
        } else {
            // current context is first element on stack
            Vector<Chunk> parentContext = contextStack.firstElement();
            parentContext = (Vector<Chunk>)parentContext.clone();
            parentContext.insertElementAt(this,0);
            return parentContext;
        }
    }

    private Vector<Chunk> getCurrentParentContext()
    {
        if (contextStack == null || contextStack.size() == 0) {
            return null;
        } else {
            // current context is first element on stack
            return contextStack.firstElement();
        }
    }

    /**
     * Retrieves a tag replacement rule.  getTagValue() responds outside the context
     * of recursive tag replacement, so the return value may include unresolved
     * tags.  To iterate up the ancestor chain, use get() instead.
     *
     * @return The Chunk or Snippet etc. that this tag will resolve to, or null
     * if no rule yet exists.
     */
    public Object getTagValue(String tagName)
    {
        if (tags != null) {
            Object x = tags.get(tagName);
            if (x instanceof String) {
                // first request for this value.  lazy-convert to Snippet.
                // subsequent fetches will benefit from pre-scan.
                Snippet s = Snippet.getSnippet((String)x);
                tags.put(tagName, s);
                return s.isSimple() ? s.toString() : s;
            } else if (x instanceof Snippet) {
                Snippet s = (Snippet)x;
                return s.isSimple() ? s.toString() : s;
            } else {
                return x;
            }
        } else {
            for (int i=0; i<tagCount; i++) {
                if (firstTags[i].equals(tagName)) {
                    Object x = firstValues[i];
                    if (x instanceof String) {
                        // first request for this value. lazy-convert to Snippet.
                        // subsequent fetches will benefit from pre-scan.
                        Snippet s = Snippet.getSnippet((String)x);
                        firstValues[i] = s;
                        return s.isSimple() ? s.toString() : s;
                    } else if (x instanceof Snippet) {
                        Snippet s = (Snippet)x;
                        return s.isSimple() ? s.toString() : s;
                    } else {
                        return x;
                    }
                }
            }
        }
        return null;
    }

    private Hashtable<String,ContentSource> altSources = null;

    public void addProtocol(ContentSource src)
    {
        if (altSources == null) {
            altSources = new Hashtable<String,ContentSource>();
            // delayed adding macro library for memory efficiency
            // (avoid overhead of hashtable whenever possible)
            if (macroLibrary != null) {
                altSources.put(macroLibrary.getProtocol(), macroLibrary);
            }
        }
        String protocol = src.getProtocol();
        altSources.put(protocol,src);
    }

    private Object altFetch(String tagName, int depth)
    {
        return altFetch(tagName, depth, false);
    }

    private static final java.util.regex.Pattern INCLUDEIF_PATTERN =
        java.util.regex.Pattern.compile("^\\.include(If|\\.\\()");

    private Object altFetch(String tagName, int depth, boolean ignoreParentContext)
    {
        String tagValue = null;

        // the .calc(...) fn
        if (tagName.startsWith(".calc(")) {
            String eval = null;
            try {
                eval = Calc.evalCalc(tagName,this);
            } catch (NoClassDefFoundError e) {
                String errMsg = "[ERROR: jeplite jar missing from classpath! .calc command requires jeplite library]";
                eval = handleError(errMsg);
            }

            return eval;
        }

        if (tagName.startsWith(".version")) {
            return VERSION;
        }

        // the .loop(...) fn
        if (tagName.startsWith(".loop")) {
            return LoopTag.expandLoop(tagName, this, this.templateOrigin, depth);
        }

        // the .tagStack fn
        if (tagName.startsWith(".tagStack")) {
            String format = "text";
            if (tagName.contains("html")) {
                format = "html";
            }
            return this.formatTagStack(format);
        }

        if (altSources == null && macroLibrary == null && getCurrentParentContext() == null) {
            // it ain't there to fetch
            return null;
        }

        // the includeIfPattern (defined above)
        // matches ".includeIf" and ".include.(" <-- ie from +(cond) expansion
        Matcher m = INCLUDEIF_PATTERN.matcher(tagName);
        if (m.find()) {
            // this is either lame or very sneaky
            String translation = Filter.translateIncludeIf(tagName,tagStart,tagEnd,this);

            return translation;
        }

        // parse content source "protocol"
        int delimPos = tagName.indexOf(".",1);
        int spacePos = tagName.indexOf(" ",1); // {.include abc#xyz} is ok too
        if (delimPos < 0 && spacePos < 0) {
            if (tagName.startsWith("./")) {
                // extra end tag, pass through
                return null;
                //return "[CHUNK_ERR: extra end tag, no matching tag found for "+tagName.substring(1)+"]";
            } else {
                String errMsg = "[CHUNK_ERR: malformed content reference: '"+tagName+"' -- missing argument]";
                return handleError(errMsg);
            }
        }
        if (spacePos > 0 && (delimPos < 0 || spacePos < delimPos)) delimPos = spacePos;
        String srcName = tagName.substring(1,delimPos);
        String itemName = tagName.substring(delimPos+1);

        // for this to work, caller must have already provided an object which
        // implements com.x5.template.ContentSource
        //  -- then templates can delegate to this source using the syntax
        // {.protocol.itemName}   eg {.wiki.About_Us}  or {.include.#some_template}

        // strip away filters, defaults
        String cleanItemName = itemName;
        cleanItemName = cleanItemName.replaceAll("[\\|:].*$", "");

        ContentSource fetcher = null;
        if (altSources != null) {
            fetcher = altSources.get(srcName);
        } else if (macroLibrary != null && srcName.equals(macroLibrary.getProtocol())) {
            // if the only alt source is the macro library for includes,
            // no hashtable is made (for memory efficiency)
            fetcher = macroLibrary;
        }
        // when altSources exists, it handles includes too
        if (fetcher != null) {
            if (fetcher instanceof Theme) {
                // include's are special, handle via macroLibrary TemplateSet
                // slight optimization, return Snippet instead of String
                Theme theme = (Theme)fetcher;
                String templateRef = BlockTag.qualifyTemplateRef(templateOrigin, cleanItemName);
                Snippet s = theme.getSnippet(templateRef);
                if (s != null) return s;
            } else {
                tagValue = fetcher.fetch(cleanItemName);
            }
        }

        if (tagValue == null && !ignoreParentContext) {
            // still null? maybe an ancestor knows how to grok
            Vector<Chunk> parentContext = getCurrentParentContext();
            if (parentContext != null) {
                for (Chunk ancestor : parentContext) {
                    // lazy... should repeat if/else above to avoid re-parsing the tag
                    Object x = ancestor.altFetch(tagName, depth, true);
                    if (x != null) return x;
                }
            }
        }

        return tagValue;
    }

    private String resolveBackticks(String lookupName, int depth)
    {
        int backtickA = lookupName.indexOf('`');
        if (backtickA < 0) return lookupName;
        int backtickB = lookupName.indexOf('`',backtickA+1);
        if (backtickB < 0) return lookupName;

        String embeddedTag = lookupName.substring(backtickA+2,backtickB);
        char typeChar = lookupName.charAt(backtickA+1);
        if (typeChar == '^' || typeChar == '.') {
            embeddedTag = '.'+embeddedTag;
        } else if (typeChar != '~' && typeChar != '$') {
            // only ^ and ~ (and $) are legal for now
            return lookupName;
        }

        Object backtickExprValue = resolveTagValue(embeddedTag, depth);
        if (backtickExprValue == null) {
            return lookupName;
        } else {
            String dynLookupName = lookupName.substring(0,backtickA)
              + backtickExprValue
              + lookupName.substring(backtickB+1);

            // there may be more...
            return resolveBackticks(dynLookupName, depth);
        }
    }

    protected Object resolveTagValue(SnippetTag tag, int depth, String origin)
    {
        if (origin == null) {
            return _resolveTagValue(tag, depth, false);
        } else {
            this.templateOrigin = origin;
            Object value = _resolveTagValue(tag, depth, false);
            this.templateOrigin = null;

            return value;
        }
    }

    protected Object resolveTagValue(SnippetTag tag, int depth)
    {
        return _resolveTagValue(tag, depth, false);
    }

    @SuppressWarnings("rawtypes")
    protected Object _resolveTagValue(SnippetTag tag, int depth, boolean ignoreParentContext)
    {
        String[] path = tag.getPath();
        int segment = 0;
        String segmentName = path[segment];

        if (segmentName.indexOf('`') > -1) {
            segmentName = resolveBackticks(segmentName, depth);
        }

        Object tagValue = null;

        if (segmentName.charAt(0) == '.') {
            tagValue = altFetch(segmentName, depth);
        } else if (hasValue(segmentName)) {
            tagValue = getTagValue(segmentName);
        } else {
            if (ignoreParentContext) {
                return tagValue;
            }
            Vector<Chunk> parentContext = getCurrentParentContext();
            if (parentContext != null) {
                // now look in ancestors (iteration, not recursion, so sue me)
                for (Chunk ancestor : parentContext) {
                    tagValue = ancestor.getTagValue(segmentName);
                    ////tagValue = ancestor._resolveTagValue(tag, depth, true);
                    if (tagValue != null) break;
                }
            }
        }

        segment++;
        // If path has more segments, drill deeper until reference is resolved
        // or path-map hits dead end
        while (path.length > segment && tagValue != null) {
            if (tagValue instanceof Map) {
                segmentName = resolveBackticks(path[segment], depth);
                Map obj = (Map)tagValue;
                tagValue = obj.get(segmentName);
                segment++;
                // Sometimes dotted tags are not actually buried
                // deep inside objects... cf LoopTag $x.first
                // This is a hacky way to allow single-depth refs to work
                if (tagValue == null && path.length == segment) {
                    String fakeRef = path[segment-2] + "." + segmentName;
                    tagValue = getTagValue(fakeRef);
                }
            } else {
                tagValue = null;
            }
        }
        // convert primitives to string
        if (!(tagValue instanceof String)) {
            tagValue = coercePrimitivesToString(tagValue);
        }

        String filters = tag.getFilters();

        if (tagValue == null) {

            String tagDefault = tag.getDefaultValue();
            if (filters != null && (tag.applyFiltersFirst() || tagDefault == null)) {
                // filtering may result in null being transformed to not null
                Object filteredNull = Filter.applyFilter(this, filters, null);
                if (filteredNull != null) {
                    return filteredNull;
                }
            }
            if (tag.applyFiltersFirst()) {
                return tagDefault;
            } else if (filters != null) {
                return Filter.applyFilter(this, filters, tagDefault);
            } else {
                return tagDefault;
            }

        } else {

            if (filters == null) {
                return tagValue;
            } else {
                Object filteredVal = Filter.applyFilter(this, filters, tagValue);
                if (filteredVal == null && tag.applyFiltersFirst()) {
                    return tag.getDefaultValue();
                } else {
                    return filteredVal;
                }
            }
        }
    }

    // unbox and stringify primitive wrapper objects
    private Object coercePrimitivesToString(Object o)
    {
        if (o instanceof Boolean) {
            return ((Boolean)o).booleanValue() ? "TRUE" : null;
        } else if (o != null && ObjectDataMap.isWrapperType(o.getClass())) {
            return o.toString();
        } else {
            return o;
        }
    }

    protected Object resolveTagValue(String tagName, int depth)
    {
        return _resolveTagValue(SnippetTag.parseTag(tagName), depth, false);
    }

    /**
     * Clears all tag replacement rules.
     */
    public void resetTags()
    {
        if (tags != null) {
            tags.clear();
        } else {
            tagCount = 0;
        }
    }

    public void clear()
    {
        resetTags();
    }

    /**
     * Clears template
     */
    public void resetTemplate()
    {
        if (this.template == null) {
            this.templateRoot = null;
        } else {
            this.template.clear();
        }
    }

    public boolean containsKey(Object key)
    {
        if (tags == null) {
            tags = new Hashtable<String,Object>();
            copyToHashtable();
        }
        return tags.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        if (tags == null) {
            tags = new Hashtable<String,Object>();
            copyToHashtable();
        }
        return tags.containsValue(value);
    }

    public Set<java.util.Map.Entry<String, Object>> entrySet()
    {
        if (tags == null) {
            tags = new Hashtable<String,Object>();
            copyToHashtable();
        }
        return tags.entrySet();
    }

    public boolean equals(Object o)
    {
        if (tags == null) {
            tags = new Hashtable<String,Object>();
            copyToHashtable();
        }
        return tags.equals(o);
    }

    public Object get(Object key)
    {
        return resolveTagValue((String)key, 1);
    }

    public int hashCode()
    {
        if (tags == null) {
            tags = new Hashtable<String,Object>();
            copyToHashtable();
        }
        return tags.hashCode();
    }

    public boolean isEmpty()
    {
        if (tags == null) {
            return tagCount == 0;
        } else {
            return tags.isEmpty();
        }
    }

    public java.util.Set<String> keySet()
    {
        if (tags == null) {
            tags = new Hashtable<String,Object>();
            copyToHashtable();
        }
        return tags.keySet();
    }

    public Object put(String key, Object value)
    {
        Object x = getTagValue(key);
        set(key, value, "");
        return x;
    }

    public Object remove(Object key)
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void putAll(Map t)
    {
        if (t == null || t.size() < 0) return;
        java.util.Set<String> set = t.keySet();
        java.util.Iterator<String> i = set.iterator();
        while (i.hasNext()) {
            String tagName = i.next();
            set(tagName, t.get(tagName), "");
        }
    }

    public int size()
    {
        if (tags != null) return tags.size();
        return tagCount;
    }

    public java.util.Collection<Object> values()
    {
        if (tags == null) {
            tags = new Hashtable<String,Object>();
            copyToHashtable();
        }
        return tags.values();
    }

    /**
     * Adds multiple find-and-replace rules using all entries in the
     * Hashtable.  Replaces an existing rule if tagNames collide.
     */
    public void setMultiple(Map<String,Object> rules)
    {
        if (rules == null || rules.size() <= 0) return;
        Set<String> keys = rules.keySet();
        for (String tagName : keys) {
            setOrDelete(tagName,rules.get(tagName));
        }
    }

    /**
     * Adds multiple find-and-replace rules using all rules from the passed
     * Chunk.  Replaces any existing rules with the same tagName.
     */
    public void setMultiple(Chunk copyFrom)
    {
        if (copyFrom != null) {
            Map<String,Object> h = copyFrom.getTagsTable();
            setMultiple(h);
        }
    }

    /**
     * Retrieve all find-and-replace rules.  Alterations to the returned
     * Hashtable WILL AFFECT the tag replacement rules of the Chunk directly.
     * Does not return a clone.
     * @return a Hashtable containing the Chunk's find-and-replace rules.
     */
    public Map<String,Object> getTagsTable()
    {
        if (tags != null) {
            return tags;
        } else {
            if (tagCount <= 0) {
                return null;
            } else {
                copyToHashtable();
                return tags;
            }
        }
    }

    private void copyToHashtable()
    {
        if (tags == null) tags = new Hashtable<String,Object>(tagCount*2);
        for (int i=0; i<tagCount; i++) {
            tags.put(firstTags[i],firstValues[i]);
        }
    }

    /**
     * formatTagStack and outputTags help implement the
     * {.tagStack} debug feature, for template writers
     * to see what tags are available during development.
     * use {.tagStack(html)} to make output more readable in a web page.
     *
     * @param format
     * @return
     */
    private String formatTagStack(String format)
    {
        StringBuilder stack = new StringBuilder();

        String lineFeed = "\n";
        String indent = "  ";
        if (format.equals("html")) {
            lineFeed = "<br/>\n";
            indent = "&nbsp;&nbsp;";
        }

        stack.append("Available tags:");
        stack.append(lineFeed);

        int indentLevel = 0;

        this.outputTags(stack,lineFeed,indent,indentLevel);
        indentLevel++;

        Vector<Chunk> parentContext = getCurrentParentContext();
        if (parentContext != null) {
            for (Chunk ancestor : parentContext) {
                ancestor.outputTags(stack,lineFeed,indent,indentLevel);
                indentLevel++;
            }
        }

        return stack.toString();
    }

    private void outputTags(StringBuilder output, String lf, String ind, int indent)
    {
        ArrayList<String> list = new ArrayList<String>();
        if (tags == null) {
            for (int i=0; i<tagCount; i++) {
                list.add(firstTags[i]);
            }
        } else {
            list.addAll(tags.keySet());
        }

        Collections.sort(list);
        for (String tag:list) {
            for (int x=0; x<indent; x++) output.append(ind);
            output.append('$');
            output.append(tag);
            output.append(lf);
        }
    }

    /**
     * Smart objects implementing DataCapsule can provide their own
     * legend of available tags and which methods to call for exporting
     * the tag data.
     *
     * NB: set() won't unwrap its data until "runtime" ie when
     * toString() is called.  addData() is not like that --
     * the exported values are copied and frozen when the addData()
     * call is made.  No pointer to the DataCapsule is kept.
     *
     * @param smartObj
     */
    public void addData(DataCapsule smartObj)
    {
        DataCapsuleReader reader = DataCapsuleReader.getReader(smartObj);

        String[] tags = reader.getColumnLabels(null);
        Object[] data = reader.extractData(smartObj);

        for (int i=0; i<tags.length; i++) {
            Object val = data[i];
            if (val == null || val instanceof String || val instanceof DataCapsule) {
                this.setOrDelete(tags[i], val);
            } else {
                this.set(tags[i], val.toString());
            }
        }
    }

    /**
     * Two smart objects of the same type in a single template?
     *
     * No problem. Provide a unique altPrefix for each one
     * to avoid tag namespace collisions.
     *
     * Note: these two calls are now equivalent:
     *   chunk.addData(capsule,"x");
     *   chunk.set("x",capsule);
     *
     * The second form is preferred, so this method may get deprecated.
     *
     * @param smartObj
     * @param altPrefix
     */
    public void addData(DataCapsule smartObj, String altPrefix)
    {
        if (smartObj == null) return;
        if (altPrefix == null) {
            this.addData(smartObj);
        } else {
            this.set(altPrefix,smartObj);
        }
    }

    public String makeTag(String tagName)
    {
        return tagStart + tagName + tagEnd;
    }

    public void setErrorHandling(boolean renderErrs, PrintStream err)
    {
        this.renderErrs = renderErrs;
        this.errLog = err;
    }

    boolean renderErrorsToOutput()
    {
        return renderErrs;
    }

    private String handleError(String errMsg)
    {
        logError(errMsg);
        return renderErrs ? errMsg : null;
    }

    void logError(String errMsg)
    {
        logChunkError(errLog, errMsg);
    }

    private static final SimpleDateFormat LOG_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zZ ");

    static void logChunkError(PrintStream log, String errMsg)
    {
        if (log != null) {
            // add timestamp
            log.print(LOG_DATE.format(new java.util.Date()));
            log.println(errMsg);
        }
    }

    public void setLocale(String localeCode)
    {
        this.localeCode = localeCode;
    }

    public void setLocale(Locale javaLocale)
    {
        if (javaLocale == null) {
            this.localeCode = null;
        } else {
            String localeCode = javaLocale.toString();
            localeCode = localeCode.replace('-', '_');
            this.setLocale(localeCode);
        }
    }

    public void setLocale(ChunkLocale chunkLocale)
    {
        if (chunkLocale == null) {
            this.localeCode = null;
        } else {
            this.setLocale(chunkLocale.toString());
        }
    }

    public ChunkLocale getLocale()
    {
        if (localeCode == null) return null;
        if (locale == null) {
            locale = ChunkLocale.getInstance(localeCode, this);
        }
        return locale;
    }

    /**
     * Useful utility function.  An efficient find-and-replace-all algorithm
     * for simple cases when regexp would be overkill.  IMO they should have
     * included this with String.
     * @param toSearch  text body.
     * @param find  text to search for.
     * @param replace  text to insert in place of "find" -- defaults to empty String if null is passed.
     * @return a new String based on input with all instances of "find" replaced with "replace"
     */
    public static String findAndReplace(String toSearch, String find, String replace)
    {
        if (find == null || toSearch == null || toSearch.indexOf(find) == -1) return toSearch;
        if (replace == null) replace = "";
        int marker=0, findPos, findLen = find.length();
        StringBuilder sb = new StringBuilder();
        while ((findPos = toSearch.indexOf(find,marker)) > -1) {
            sb.append(toSearch.substring(marker,findPos));
            sb.append(replace);
            marker = findPos+findLen;
        }
        sb.append(toSearch.substring(marker));
        return sb.toString();
    }

    public String getTemplateOrigin()
    {
        return this.templateOrigin;
    }

}
