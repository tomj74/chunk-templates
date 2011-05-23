package com.x5.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Pattern;

import com.x5.util.DataCapsule;
import com.x5.util.DataCapsuleReader;
import com.x5.util.TableData;

// Project Title: Chunk
// Description: Template Util
// Copyright: Copyright (c) 2007
// Author: Tom McClure

/**
 * <P>
 * Chunk is part Hashtable, part StringBuilder, part find-and-replace.
 *
 * <P>
 * Assign an initial template (you can stitch on bits of additional template<BR>
 * content as you go) with placeholder tags -- eg {~my_tag} -- and then<BR>
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
 * <B>NB: Template {~tags} are bounded by curly brackets, not (parentheses).<BR>
 * And don't forget the ~squiggle (aka tilde, pronounced "TILL-duh").</B>  Also,<BR>
 * be careful to always close off {#sub_templates}bla bla bla{#} with a single<BR>
 * hash mark surrounded by curly brackets.
 *
 * <P>
 * TemplateSet is handy if you have a folder with lots of html templates.<BR>
 * Here's an even simpler example, where the template string is supplied<BR>
 * without using TemplateSet:
 *
 * <PRE>
 *    String templateBody = "Hello {~name}!  Your balance is ${~balance}."
 *         + "Pleasure serving you, {~name}!";
 *    Chunk myChunk = new Chunk();
 *
 *    // .add() and .set() may be called in any order
 *    // because tag replacement is delayed until the .toString() call.
 *    myChunk.add( templateBody );
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
 * bla bla bla {~myTemplate} foo foo foo
 * {#myTemplate}Hello {~name}!{#}
 * </PRE>
 *
 * <P>
 * A: No*.  To keep things simple and reduce potential for confusion, Chunk<BR>
 * does not auto-magically fill any tags based on naming conventions or<BR>
 * in-template directives.  You must explicitly define this rule via set:<BR>
 *   set("myTemplate", templates.get("file.myTemplate"));
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
 * <P>Complete details are here: <a href="http://www.dagblastit.com/src/template/howto.html">http://www.dagblastit.com/src/template/howto.html</a>
 *
 * <P>
 * <B>Q: My final output says "infinite recursion detected."  What gives?</B>
 *
 * <P>
 * A: You did some variation of this:
 * <PRE>
 *   TEMPLATE:
 *     bla bla bla {~name}
 *     {#name_info}My name is {~name}{#}
 *
 *   CODE:
 *     ...set("name", templates.get("file#name_info"));
 *     ...toString();
 * </PRE>
 *
 * <P>
 * The outer template gets its {~name} tag replaced with "My name is {~name}" --<BR>
 * then, that replacement value is scanned for any tags that might need to be<BR>
 * swapped out for their values.  It finds {~name} and, using the rule you gave,<BR>
 * replaces it with "My name is {~name}" so we now have My name is My name is ...<BR>
 * ad infinitum.
 *
 * <P>
 * This situation is detected by assuming recursion depth will not normally go<BR>
 * deeper than 7.  If you legitimately need to nest templates that deep, you<BR>
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
 *     {~memberMenu:}
 *     {#member_menu}this that etc{#}
 *     foo foo foo
 *
 *   CODE:
 *     if (isLoggedIn) {
 *          myChunk.set("memberMenu", templates.get("file.member_menu"));
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
 * A: Yes. I prefer to use mixed case in {~tagNames} with first letter<BR>
 * lowercase.  In my experience this aids readability since tags are similar<BR>
 * to java variables in concept and that is the java case convention for<BR>
 * variables.   Similarly, I prefer lowercase with underscores for all<BR>
 * {#sub_template_names}{#} since templates tend to be defined within html<BR>
 * files which are typically named in all lowercase.
 *
 * <P>
 * <B>Q: I defined a whole lot of subtemplates in one file.  Do I have to<BR>
 * specify the filename stub over and over every time I fetch a subtemplate?</B>
 *
 * <P>
 * A: Nope!  You can now make a TemplateSet out of a single template file.<BR>
 * For example, these two pieces of code are equivalent:<BR>
 *
 * <PRE>
 *    // old way
 *    TemplateSet html = getTemplates();
 *    Chunk myTable = html.makeChunk("my_file.table");
 *    Chunk myRow   = html.makeChunk("my_file.row");
 *    Chunk myCell  = html.makeChunk("my_file.cell");
 *
 *    // new way, less repetitious
 *    TemplateSet html = getTemplates();
 *    TemplateSet myHtml = html.getSubset("my_file");
 *    Chunk myTable = myHtml.makeChunk("table");
 *    Chunk myRow   = myHtml.makeChunk("row");
 *    Chunk myCell  = myHtml.makeChunk("cell");
 * </PRE>
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
 *    rows.set("backgroundColor", getRowColor() );
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
 *    table.set("tableRows",rows);
 *    System.out.println( table.toString() );
 * </PRE>
 *
 * <PRE>
 * Possible contents of my_table.html:
 *
 * &lt;TABLE&gt;
 * {~tableRows}
 * &lt;/TABLE&gt;
 *
 * {#my_row}
 * &lt;TR bgcolor="{~backgroundColor}"&gt;
 *  &lt;TD&gt;{~id} - {~name}&lt;/TD&gt;
 *  {~cells}
 * &lt;/TR&gt;
 * {#}
 *
 * {#my_cell}
 * &lt;TD&gt;{~cellContent}&lt;/TD&gt;
 * {#}
 * </PRE>
 *
 * Copyright: Copyright (c) 2003<BR>
 * Company: <A href="http://www.x5software.com/">X5 Software</A><BR>
 * Updates: <A href="http://www.dagblastit.com/">www.dagblastit.com</A><BR>
 *
 * @author Tom McClure
 * @version 3.0
 */

public class Chunk implements Map<String,Object>
{
    public static final int HASH_THRESH = 25;
    public static final int DEPTH_LIMIT = 17;

    protected Snippet templateRoot = null;
    private String[] firstTags = new String[HASH_THRESH];
    private Object[] firstValues = new Object[HASH_THRESH];
    private int tagCount = 0;
    protected Vector<Object> template = null;
    private Hashtable<String,Object> tags = null;
    protected String tagStart = TemplateSet.DEFAULT_TAG_START;
    protected String tagEnd = TemplateSet.DEFAULT_TAG_END;

    private String delayedFilter = null;

    private ContentSource macroLibrary = null;
    private ChunkFactory chunkFactory = null;

    public void setTagBoundaries(String tagStart, String tagEnd)
    {
        this.tagStart = tagStart;
        this.tagEnd = tagEnd;
    }

    public void setMacroLibrary(ContentSource repository, ChunkFactory factory)
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
                template = new Vector<Object>();
                template.addElement(templateRoot);
            }
            template.addElement(toAdd);
        }
    }

    /**
     * Add a String on to the end a Chunk's template.
     */
    public void append(String toAdd)
    {
    	if (toAdd == null) return;
    	
    	// cut string into snippets-to-process and literals
    	Snippet snippet = new Snippet(toAdd);
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
            template = new Vector<Object>();
            if (templateRoot != null) template.addElement(templateRoot);
        }
        template.addElement(toAdd);
    }

    /**
     * Creates a find-and-replace rule for tag replacement.  Overwrites any
     * previous rules for this tagName.  Do not include the tag boundary
     * markers in the tagName, ie
     * GOOD: set("this","that")
     * BAD: set("{~this}","that")
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
     * @see append(Chunk c)
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
        }
        String ifNull = null;
        set(tagName, tagValue, ifNull);
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
        	if (!(tagValue instanceof String
        			|| tagValue instanceof Snippet
        			|| tagValue instanceof Chunk
        			|| tagValue instanceof TableData
        			|| tagValue instanceof Object[])) {
        		// bail...
        		return;
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
     * Create a tag replacement rule, supplying a default value in case
     * the value passed is null.  If both the tagValue and the fallback
     * are null, a translate-to-empty-string rule is created.
     * @param tagName tag to replace
     * @param tagValue replacement value -- no-op unless this is of type String or Chunk.
     * @param ifNull fallback replacement value in case tagValue is null
     */
    public void set(String tagName, Object tagValue, Chunk ifNull)
    {
        if (tagName == null) return;
        if (tagValue == null) {
            if (ifNull == null) {
                tagValue = "";
            } else {
                tagValue = ifNull;
            }
        }
        if (tagValue instanceof Chunk || tagValue instanceof String || tagValue instanceof TableData) {
            set(tagName, tagValue, "");
        }
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
        return explodeForParent(null);
    }

    public String toString(Chunk context)
    {
        // sometimes orphaned chunks need to be reunited with their ancestry
        Vector<Chunk> ancestors = new Vector<Chunk>();
        if (context.ancestorStack != null) {
            ancestors.addAll(context.ancestorStack);
        }
        ancestors.addElement(context);
        return explodeForParent(ancestors);
    }

    private String explodeForParent(Vector<Chunk> ancestors)
    {
        if (template == null && templateRoot == null) return "";
        StringBuilder buf = new StringBuilder();
        if (template == null) {
    		explodeAndAppend(templateRoot, buf, ancestors, 1);
        } else {
            for (int i=0; i < template.size(); i++) {
                Object obj = template.elementAt(i);
                explodeAndAppend(obj, buf, ancestors, 1);
            }
        }
        //        return TextFilter.applyTextFilter(delayedFilter, buf.toString());

        // not sure if this is right, but it had the desired effect...
        if (delayedFilter == null) {
            return buf.toString();
        } else {
            // ick, the post-filter output may contain explodable tags
            String postFilter = TextFilter.applyTextFilter(this, delayedFilter, buf.toString());
            StringBuilder buf2 = new StringBuilder();
            // re-process (hopefully this won't have any weird side-effects)
            explodeAndAppend(postFilter, buf2, ancestors, 1);
            return buf2.toString();
        }
    }

    private void explodeAndAppend(Object obj, StringBuilder buf, Vector<Chunk> ancestors, int depth)
    {
        if (depth >= DEPTH_LIMIT) {
            buf.append("[**ERR** max template recursions: "+DEPTH_LIMIT+"]");
        } else if (obj instanceof Snippet) {
        	Snippet snippet = (Snippet)obj;
        	if (snippet.isSimple()) {
        		// most snippets are simple (ie don't contain literals)
        		buf.append(explodeString(snippet.getSimpleText(), ancestors, depth));
        	} else {
            	ArrayList<SnippetPart> parts = snippet.getParts();
            	if (parts == null) return;
        		for (SnippetPart part : parts) {
                	if (part.isLiteral()) {
                		buf.append(part.getText()); // DO NOT INTERPOLATE literal block
                	} else {
                        buf.append(explodeString(part.getText(), ancestors, depth));
                	}
        		}
        	}
        } else if (obj instanceof String) {
        	// snippet-ify to catch/skip literal blocks
        	Snippet snippet = new Snippet((String)obj);
			explodeAndAppend(snippet,buf,ancestors,depth);
            ///buf.append(explodeString((String)obj, ancestors, depth));
        } else if (obj instanceof Chunk) {
            if (ancestors == null) ancestors = new Vector<Chunk>();
            ancestors.addElement(this);
            Chunk c = (Chunk) obj;
            buf.append(c.explodeForParent(ancestors));
            // pull self off the stack (wasn't doing this before, amazing no bugs until now)
            if (ancestors.size() > 1) ancestors.removeElementAt(ancestors.size()-1);
        } else if (obj instanceof DataCapsule[]) {
        	// auto-expand?
        	DataCapsuleReader reader = DataCapsuleReader.getReader((DataCapsule[])obj);
        	buf.append("[LIST("+reader.getDataClassName()+") - Use a loop construct such as ^loop or ^grid to display list data.]");
        } else if (obj instanceof String[]) {
        	buf.append("[LIST(java.util.String) - Use a loop construct such as ^loop to display list data, or pipe to join().]");
        }
    }

    /**
     * Retrieves a tag replacement rule.  getTag responds outside the context
     * of recursive tag replacement, so the return value may include unresolved
     * tags.
     * @return The String or Chunk that this tag will resolve to, or null
     * if no rule yet exists.
     */
    public Object getTag(String tagName)
    {
        if (tags != null) {
            return tags.get(tagName);
        } else {
            for (int i=0; i<tagCount; i++) {
                if (firstTags[i].equals(tagName)) {
                    return firstValues[i];
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

    private static final java.util.regex.Pattern includeIfPattern =
        java.util.regex.Pattern.compile("^\\.include(If|\\.\\()");

    private Object altFetch(String tagName, Vector<Chunk> ancestors)
    throws BlockTagException
    {
        String tagValue = null;

       
        /**
         * literals are intercepted higher up so theoretically we do not
         * need to handle literals here...
        // a ^literal block
        if (tagName.startsWith(".^") || tagName.startsWith(".literal")) {
        	return tagName;
        }
         */
        
        // the ^calc(...) fn
        if (tagName.startsWith(".calc(")) {
            // FIXME this is not threadsafe (synchronize call/block?)
            this.ancestorStack = ancestors;
            String eval = null;
            try {
                eval = Calc.evalCalc(tagName,this);
            } catch (NoClassDefFoundError e) {
            	eval = "[ERROR: jeplite jar missing from classpath! ^calc special requires jeplite library]";
            }
            this.ancestorStack = null;

            return eval;
        }
        
        if (tagName.startsWith(".if")) {
            this.ancestorStack = ancestors;
            String result = IfTag.evalIf(tagName, this);
            this.ancestorStack = null;
            
            return result;
        }

        // the ^loop(...) fn
        if (tagName.startsWith(".loop")) {
            // FIXME this is not threadsafe (synchronize call/block?)
            this.ancestorStack = ancestors;
            String table = Loop.expandLoop(tagName,this);
            this.ancestorStack = null;

            return table;
        }

        // the ^grid(...) fn
        if (tagName.startsWith(".grid")) {
            // FIXME this is not threadsafe (synchronize call/block?)
            this.ancestorStack = ancestors;
            String table = Grid.expandGrid(tagName,this);
            this.ancestorStack = null;

            return table;
        }
        
        // the ^tagStack fn
        if (tagName.startsWith(".tagStack")) {
    		String format = "text";
    		if (tagName.contains("html")) {
    			format = "html";
    		}
        	return this.formatTagStack(format,ancestors);
        }

        if (altSources == null && macroLibrary == null && ancestors == null) {
            // it ain't there to fetch
            return null;
        }

        // the includeIfPattern (defined above)
        // matches ".includeIf" and ".include.(" <-- ie from +(cond) expansion
        if (TextFilter.matches(tagName,includeIfPattern)) {
            // this is either lame or very sneaky
            this.ancestorStack = ancestors;
            String translation = TextFilter.translateIncludeIf(tagName,tagStart,tagEnd,this);
            this.ancestorStack = null;

            return translation;
        }
        
        // parse content source "protocol"
        int delimPos = tagName.indexOf(".",1);
        int spacePos = tagName.indexOf(" ",1); // {^include abc#xyz} is ok too
        if (delimPos < 0 && spacePos < 0) {
            return "[malformed content reference: '"+tagName+"' -- missing argument]";
        }
        if (spacePos > 0 && (delimPos < 0 || spacePos < delimPos)) delimPos = spacePos;
        String srcName = tagName.substring(1,delimPos);
        String itemName = tagName.substring(delimPos+1);

        // for this to work, caller must have already provided an object which
        // implements com.x5.template.ContentSource
        //  -- then templates can delegate to this source using the syntax
        // {^protocol.itemName}   eg {^wiki.About_Us}  or {^include.#some_template}

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
            	Snippet s = theme.getSnippet(cleanItemName);
            	if (s != null) return s;
            } else {
            	tagValue = fetcher.fetch(cleanItemName);
            }
        }

        if (tagValue == null && ancestors != null) {
            // still null? maybe an ancestor knows how to grok
            for (int i=ancestors.size()-1; i>=0; i--) {
                Chunk ancestor = (Chunk)ancestors.elementAt(i);
                // lazy... should repeat if/else above to avoid re-parsing the tag
                Object x = ancestor.altFetch(tagName, null);
                if (x != null) return x;
            }
        }

        return tagValue;
    }

    /**
     * Don't interpret pipes from an includeIf(...) regex as filter markers
     */
    private int confirmPipe(String tagName, int pipePos)
    {
        int doesntCountParen = tagName.indexOf("includeIf(");
        if (doesntCountParen < 0) {
            // also have to check for expanded {+(...)} syntax
            doesntCountParen = tagName.indexOf("include.(");
        }
        if (doesntCountParen < 0) return pipePos;
        // skip to the end-paren and search from there
        int nextSlash = tagName.indexOf("/",doesntCountParen+7);
        int nextParen = tagName.indexOf(")",doesntCountParen+7);
        // for the pipe not to count, has to be in /reg|ex/
        if (nextSlash < 0 || nextParen < 0) return pipePos;
        if (nextParen < nextSlash) return pipePos;
        // okay, we found a regex. find the end of the regex.
        int regexEnd = TextFilter.nextRegexDelim(tagName,nextSlash+1);
        nextParen = tagName.indexOf(")",regexEnd+1);
        if (nextParen < 0 || nextParen < pipePos) return pipePos;
        return tagName.indexOf("|",nextParen+1);
    }
    
    private String resolveBackticks(String lookupName, Vector<Chunk> ancestors)
    {
    	int backtickA = lookupName.indexOf('`');
    	if (backtickA < 0) return lookupName;
    	int backtickB = lookupName.indexOf('`',backtickA+1);
    	if (backtickB < 0) return lookupName;
    	
    	String embeddedTag = lookupName.substring(backtickA+2,backtickB);
    	char typeChar = lookupName.charAt(backtickA+1);
    	if (typeChar == '^') {
    		embeddedTag = '.'+embeddedTag;
    	} else if (typeChar != '~') {
    		// only ^ and ~ are legal for now
    		return lookupName;
    	}
    	
    	try {
        	String dynLookupName = lookupName.substring(0,backtickA)
        	  + resolveTagValue(embeddedTag,ancestors)
        	  + lookupName.substring(backtickB+1);
        	
        	// there may be more...
        	return resolveBackticks(dynLookupName,ancestors);
    	} catch (BlockTagException e) {
    	    // should never, ever happen.
    	    e.printStackTrace(System.err);
    	    return null;
    	}
    }

    // resolveTagValue responds in the context of an explosion tree.
    // ie, if the tag has not been set in this chunk, it goes up the
    // chain of parent chunks for the first one able to resolve the
    // tag into a value.  For example, several row chunks might share
    // a whole-table parent chunk.  Some of the tags in the row are
    // set differently in each row but some will always resolve the
    // same throughout the whole table -- rather than set it over and
    // over the same in each row, the tag is given a value once at the
    // table level.
    protected Object resolveTagValue(String tagName)
    throws BlockTagException
    {
        return resolveTagValue(tagName, null);
    }

    protected Object resolveTagValue(String tagName, Vector<Chunk> ancestors)
    throws BlockTagException
    {
    	if (tagName.indexOf('`') > -1) {
    		tagName = resolveBackticks(tagName, ancestors);
    	}
        String lookupName = tagName;

        //strip off the default if provided eg {~tagName:333} means use 333
        // if no specific value is provided.
        //strip filters as well eg {~tagName|s/xx/yy/}
        int colonPos = tagName.indexOf(':');
        int pipePos = tagName.indexOf('|');
        pipePos = confirmPipe(tagName,pipePos);

        if (colonPos > 0 || pipePos > 0) {
            int firstMod = (colonPos > 0) ? colonPos : pipePos;
            if (pipePos > 0 && pipePos < colonPos) firstMod = pipePos;
            lookupName = tagName.substring(0,firstMod);
        }
        
        Object tagValue = null;

        if (lookupName.charAt(0) == '.') {
            // if the tag starts with a period, we need to delegate
            tagValue = altFetch(tagName,ancestors);
        } else if (hasValue(lookupName)) {
            // first look in this chunk's own tags
            tagValue = getTag(lookupName);
        } else if (ancestors != null) {
            // now look in ancestors (iteration, not recursion, so sue me)
            for (int i=ancestors.size()-1; i>=0 && tagValue == null; i--) {
                Chunk ancestor = (Chunk)ancestors.elementAt(i);
                tagValue = ancestor.resolveTagValue(lookupName, null);
            }
        }

        // apply filter if provided
        if (tagValue != null) {
            if (pipePos > 0) {
                /*
                String filters = parseTagTokens(tagName, pipePos, colonPos)[0];
                // ack! should do this post-expansion
                return TextFilter.applyTextFilter(filters, (String)tagValue);
                */

            	// tagValue could be some complex entity --
                // delay filter application until it has been expanded into a string
                Chunk filterMeLater = (chunkFactory == null) ? new Chunk() : chunkFactory.makeChunk();

                // set up filters to be applied from the inside out
                String filter = parseTagTokens(tagName, pipePos, colonPos)[0];
                String[] filters = TextFilter.splitFilters(filter);
                // innermost first...
                // 3rd arg to set is ignored if 2nd arg is non-null,
                // so I'm just passing the filters string to hit the right method
                filterMeLater.set("oneTag",tagValue,filter);
                filterMeLater.append("{~oneTag}");
                filterMeLater.delayedFilter = filters[0];
                
                return makeFilterOnion(tagValue, filterMeLater, filters);
            } else {
                // no filter, no need to subchunk
                return tagValue;
            }
        }

        // reached here? no value supplied.  template might contain a default...
        if (colonPos > 0) {
            String defValue = null;
            String filter = null;
            String order = TextFilter.FILTER_LAST;
            if (pipePos > 0) {
                // apply filter if provided
                String[] tokens = parseTagTokens(tagName, pipePos, colonPos);
                filter   = tokens[0];
                defValue = tokens[1];
                order    = tokens[2];
            } else {
                // everything after the colon is a default value
                defValue = tagName.substring(colonPos+1);
            }

            if (defValue != null && defValue.length() > 0) {
                // now allowing tag/include syntax in the default value
                //
                // eg: {~my_unsupplied_tag:~some_other_tag} morph into another tag
                //     {~my_unsupplied_tag:~.include.some.template} replace w/template
                //     {~my_unsupplied_tag:+some.template} same as above but discouraged (cryptic)
                //
                // or, handled below, nothing fancy
                //     {~my_unsupplied_tag:simple default} default to "simple default"
                //     {~my_unsupplied_tag:} default to empty string
                //
                // but nested tags in the default area are still not allowed:
                //     {~my_unsupplied_tag:not {~other_tag}} NOT VALID
                //     {~my_unsupplied_tag:{~other_tag}} NOT VALID
                char firstChar = defValue.charAt(0);
                if (firstChar == '~' || firstChar == '+' || firstChar == '^') {
                    if (filter == null) {
                        return '{'+defValue+'}';
                    } else if (order.equals(TextFilter.FILTER_FIRST)) {
                        String filtered = TextFilter.applyTextFilter(this, filter, null);
                        if (filtered != null) {
                            return filtered;
                        } else {
                            return '{'+defValue+'}';
                        }
                    } else {
                        return '{'+defValue+'|'+filter+'}';
                    }
                }
            }
            // reached here?  simple case: no funny chained replacement business
            if (filter != null) {
                if (order.equals(TextFilter.FILTER_FIRST)) {
                    String filtered = TextFilter.applyTextFilter(this, filter, null);
                    return (filtered != null) ? filtered : defValue;
                } else {
                    return TextFilter.applyTextFilter(this, filter, defValue);
                }
            } else {
                return defValue;
            }
        } else {
            if (pipePos > 0) {
                // apply filter if provided
                String filter = tagName.substring(pipePos+1);
                return TextFilter.applyTextFilter(this, filter, null);
            } else {
                return null;
            }
        }
    }
    
    private Object makeFilterOnion(String[] tagValue, Chunk filterMeLater, String[] filters)
    {
        // String[] is only really legal here if the very first
        // filter is join -- must pre-apply the filter here
        if (filters[0].startsWith("join")) {
            String joinedString = TextFilter.joinStringArray((String[])tagValue,filters[0]);
            filterMeLater.set("oneTag", joinedString);
            filterMeLater.delayedFilter = null;
        } else if (filters[0].startsWith("ondefined")) {
            // well, it *is* non-null... give it a dummy string value
            // so it will pass the ondefined test.
            filterMeLater.set("oneTag", "DEFINED");
        }
        
        return wrapRemainingFilters(filterMeLater,filters);
    }
    
    private Chunk wrapRemainingFilters(Chunk filterMeLater, String[] filters)
    {
        // then subsequent filters each wrap a new layer
        for (int i=1; i<filters.length; i++) {
            Chunk wrapper = (chunkFactory == null) ? new Chunk() : chunkFactory.makeChunk();
            wrapper.set("oneTag",filterMeLater,"");
            wrapper.append("{~oneTag}");
            wrapper.delayedFilter = filters[i];
            filterMeLater = wrapper;
        }
    
        return filterMeLater;
    }
    
    private Object makeFilterOnion(Object tagValue, Chunk filterMeLater, String[] filters)
    {
        if (tagValue instanceof String[]) {
            return makeFilterOnion((String[])tagValue, filterMeLater, filters);
        } else if (tagValue instanceof String || tagValue instanceof Chunk || tagValue instanceof Snippet) {
            return wrapRemainingFilters(filterMeLater, filters);
        }

        // not a String/Snippet/Chunk, and not a String array -- the only legal filter here
        // is ondefined(...)
        if (filters[0].startsWith("ondefined")) {
            // got this far? it *is* defined...
            filterMeLater.set("oneTag", "DEFINED");
            return wrapRemainingFilters(filterMeLater, filters);
        } else {
            // no valid filters
            return tagValue;
        }
    }
    
    // pipe denotes a request to apply a filter
    // colon denotes a default value
    // they may come in either order {~tag_name:hello there|url} or {~tag_name|url:hello there}
    //
    // In retrospect, I probably should have considered a nice legible syntax like
    // {~tag_name default="hello there" filter="url"}
    private String[] parseTagTokens(String tagName, int pipePos, int colonPos)
    {
        String filter = null;
        String defValue = null;

        String order = TextFilter.FILTER_LAST;

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
            int finalPipe = TextFilter.grokFinalFilterPipe(tagName,pipePos);
            int nextColon = tagName.indexOf(":",finalPipe+1);
            if (nextColon < 0) {
                // lucked out, colon was fake-out, embedded in earlier filter
                filter = tagName.substring(pipePos+1);
            } else {
                int startScan = TextFilter.grokValidColonScanPoint(tagName,finalPipe+1);
                nextColon = tagName.indexOf(":",startScan);
                if (nextColon < 0) {
                    // colon was fake-out
                    filter = tagName.substring(pipePos+1);
                } else {
                    filter = tagName.substring(pipePos+1,nextColon);
                    defValue = tagName.substring(nextColon+1);
                    order = TextFilter.FILTER_FIRST;
                }
            }
        } else {
            // both tokens, colon before pipe
            filter = tagName.substring(pipePos+1);
            defValue = tagName.substring(colonPos+1,pipePos);
        }

        return new String[]{ filter, defValue, order };
    }

    private String expandIncludes(String template)
    {
        // presto change-o {+template} into {~.include.template}
        // also handles {^protocol.arg} into {~.protocol.arg}
        //
        // is supporting this worth the cost of another pass through the string?
        //
    	if (template == null) return null;

    	StringBuilder expanded = new StringBuilder();
    	
        // restrict search to inside tags
        int cursor = template.indexOf("{");
        if (cursor < 0) return template;
        
        int marker = 0;

        while (cursor > -1) {
            expanded.append(template.substring(marker,cursor));
            marker = cursor;
            
            if (template.length() == cursor+1) {
            	// kick out at first sign of trouble
            	break;
            }
            char afterBrace = template.charAt(cursor+1);
            if (afterBrace == '+') {
            	expanded.append("{~.include.");
            	cursor += 2;
            	marker += 2;
            } else if (afterBrace == '^') {
            	// check for literal block, and do not perform expansions
            	// inside any literal blocks.
            	int afterLiteralBlock = skipLiterals(template,cursor);
            	if (afterLiteralBlock == cursor) {
	                // ^ is shorthand for ~. eg {^include.#xyz} or {^wiki.External_Content}
            		expanded.append("{~.");
            		cursor += 2;
            		marker += 2;
            	} else {
            		// make sure literal starts with {~. or else expansion
            		// won't catch it and skip it properly
            		expanded.append("{~.");
            		expanded.append(template.substring(marker+2,afterLiteralBlock-TemplateSet.LITERAL_END.length()));
            		expanded.append("{~.}");
            		cursor = afterLiteralBlock;
            		marker = afterLiteralBlock;
            	}
            } else {
                cursor += 2;
            }
            // on to the next tag...
            if (cursor > -1) {
            	cursor = template.indexOf("{",cursor);
            }
        }
        
    	expanded.append(template.substring(marker));
        return expanded.toString();
        
    	/*
        String p1 = findAndReplace(template, TemplateSet.INCLUDE_SHORTHAND, "{~.include.");
        String p2 = findAndReplace(p1, TemplateSet.PROTOCOL_SHORTHAND, "{~.");
        return p2;
        */
    }

    private static int skipLiterals(String template, int cursor)
    {
    	int wall = template.length();
    	int shortLen = TemplateSet.LITERAL_SHORTHAND.length();
    	int scanStart = cursor;
    	if (cursor + shortLen <= wall && template.substring(cursor,cursor+shortLen).equals(TemplateSet.LITERAL_SHORTHAND)) {
    		scanStart = cursor + shortLen;
    	} else {
    		int longLen = TemplateSet.LITERAL_START.length();
    		if (cursor + longLen <= wall && template.substring(cursor,cursor+longLen).equals(TemplateSet.LITERAL_START)) {
    			scanStart = cursor + longLen;
    		}
    	}
    	
    	if (scanStart > cursor) {
    		int tail = template.indexOf(TemplateSet.LITERAL_END, scanStart);
    		if (tail < 0) {
    			return wall;
    		} else {
    			return tail + TemplateSet.LITERAL_END.length();
    		}
    	} else {
    		return cursor;
    	}
    }
    
    private String expandMacros(String template)
    {
        template = expandIncludes(template);

        int macroTagBegin = template.indexOf(TemplateSet.MACRO_START);
        if (macroTagBegin < 0) return template;

        // found a macro.  expand!
        StringBuilder expanded = new StringBuilder();
        expanded.append(template.substring(0,macroTagBegin));

        macroTagBegin += TemplateSet.MACRO_START.length();
        int macroTagEnd = template.indexOf(TemplateSet.MACRO_NAME_END,macroTagBegin);
        int macroSectionEnd = template.indexOf(TemplateSet.MACRO_END,macroTagEnd+1);
        
        // This little piece makes nested Macros possible.  I think.
        int nestedMacro = template.indexOf(TemplateSet.MACRO_START,macroTagBegin);
        
        while (nestedMacro > -1 && nestedMacro < macroSectionEnd) {
            // this end does not match our beginning -- keep looking for real end
            macroSectionEnd = template.indexOf(TemplateSet.MACRO_END,macroSectionEnd+1);
            nestedMacro = template.indexOf(TemplateSet.MACRO_START,nestedMacro+1);
            // don't mistake a macro-end {*} for a macro-beginning {*ASDF*}
            while (nestedMacro == template.indexOf(TemplateSet.MACRO_END,nestedMacro)) {
                nestedMacro = template.indexOf(TemplateSet.MACRO_START,nestedMacro+1);
            }
        }

        if (macroSectionEnd < macroTagEnd+1) {
            return "[Template syntax error -- missing macro-end tag?  Please close off all macros with "
            +TemplateSet.MACRO_END+"]";
        }

        String templateRef = template.substring(macroTagBegin,macroTagEnd).trim();
        while (templateRef.endsWith("*")) {
            // {*BOX} and {*BOX*} and {* BOX *} are all allowed -- 3rd form is preferred
            templateRef = templateRef.substring(0,templateRef.length()-1).trim();
        }

        String macroVars = template.substring(macroTagEnd+1, macroSectionEnd);
        int letBegin = macroVars.indexOf(this.tagStart);
        if (letBegin < 0) {
            // no assignments
        	// altFetch returns String or Snippet
            try {
                Object theTemplate = altFetch(".include."+templateRef,null);
                if (theTemplate != null) {
                	expanded.append( expandMacros(theTemplate.toString()) );
                }
            } catch (BlockTagException e) {
                // won't ever happen.
                e.printStackTrace(System.err);
            }
        } else {
            // make chunk from templateRef and delegate assignments
            if (letBegin > 0) macroVars = macroVars.substring(letBegin);
            expanded.append( expandMacros( expandMacros2(templateRef, macroVars) ) );
        }

        int allTheRest = macroSectionEnd + TemplateSet.MACRO_END.length();
        expanded.append( expandMacros(template.substring(allTheRest)) );

        return expanded.toString();
    }
    
	private static final Pattern INVALID_ASSIGNMENT = Pattern.compile("[\\.\\(\\:\\|]");

    /* if the macro includes variable assignments a la {~var=}value{=}, expandMacros2
     * parses them out and handles the expansion.
     * {~var = simple value} is also permitted (eg, if value contains no tags) */
    private String expandMacros2(String templateRef, String macroVars)
    {
        if (this.chunkFactory == null) return "";

        Chunk macro = chunkFactory.makeChunk(templateRef);

        int marker = 0;
        int delimPos;
        int nextTag,nextTagEnd,nextEq,nextMarker;
        int nestedMacro;
        int closeLen = TemplateSet.MACRO_LET_END.length();
        boolean jumpMarker = false;
        String closeMarker = TemplateSet.MACRO_LET + TemplateSet.MACRO_LET_END;
        String varName;
        String varValue;
        while (marker < macroVars.length()) {
        	// advance playhead to next track...
        	marker = macroVars.indexOf(this.tagStart,marker);
        	if (marker < 0) break;
        	
            marker += this.tagStart.length();
            delimPos = macroVars.indexOf(TemplateSet.MACRO_LET_END,marker);
            varName = macroVars.substring(marker,delimPos);
            while (varName.endsWith("=")) {
                // {~var=}xyz{=} and {~var = xyz} are both allowed
                varName = varName.substring(0,varName.length()-1);
            }
            // inline/simple definition?  a la  {~var=20} or {~var = 20}
            int eqPos = varName.indexOf('=');
            if (eqPos > -1) {
            	marker = delimPos + closeLen;
            	String[] assignment = varName.split(" *= *");
            	varName = assignment[0];
            	varValue = assignment[1];
            	macro.set(varName,varValue);
            	continue;
            }
            // got here? var not defined inline.
            // looking for {~var=} (lots of stuff...) {=}
            // end delimiter is optional but its use strongly preferred
            nextTagEnd = delimPos;
            delimPos += closeLen;
            // scan for start of next definition OR closeMarker {=}
            nextMarker = macroVars.indexOf(closeMarker,delimPos);
      
            // find the end of this def or the start of the next def
            do {
                nextTag = macroVars.indexOf(this.tagStart,nextTagEnd+closeLen);
                if (nextTag < 0) {
                	// no more tags, def runs to end/closeMarker
                	nextTag = (nextMarker > -1 ? nextMarker : macroVars.length());
                	break;
                } else if (nextMarker > -1 && nextTag > nextMarker) {
                	// found closeMarker
                	jumpMarker = true;
                	nextTag = nextMarker;
                	break;
                }
                nextTagEnd = macroVars.indexOf(TemplateSet.MACRO_LET_END,nextTag);
                if (nextTagEnd < 0) {
                	nextTag = macroVars.length();
                	break;
                }
                nextEq = macroVars.lastIndexOf('=',nextTagEnd);
                if (nextEq > nextTag) {
                	// possible assignment tag.  check for paren or dot to left of =
                	String assignLeftHandSide = macroVars.substring(nextTag,nextEq);
                	if (INVALID_ASSIGNMENT.matcher(assignLeftHandSide).find()) {
                		// not a valid assignment tag, false alarm
                		nextEq = -1;
                	}
                }
                // if this tag is not an assignment tag, keep looking
            } while (nextEq < nextTag);
		            
            marker = nextTag;
            
            if (marker < delimPos) marker = macroVars.length();
            
            nestedMacro = macroVars.indexOf(TemplateSet.MACRO_START,delimPos);
            while (nestedMacro > -1 && nestedMacro < marker) {
                // skip ahead to matching macro end
                int nestedMacroEnd = macroVars.indexOf(TemplateSet.MACRO_END,nestedMacro+1);
                // make sure this is the end
                int doubleNested = macroVars.indexOf(TemplateSet.MACRO_START,nestedMacro+1);
                while (doubleNested > -1 && doubleNested < nestedMacroEnd) {
                    // this end does not match our beginning -- keep looking for real end
                    nestedMacroEnd = macroVars.indexOf(TemplateSet.MACRO_END,nestedMacroEnd+1);
                    doubleNested = template.indexOf(TemplateSet.MACRO_START,doubleNested+1);
                    // don't mistake a macro-end {*} for a macro-beginning {*ASDF*}
                    while (doubleNested == template.indexOf(TemplateSet.MACRO_END,doubleNested)) {
                        doubleNested = template.indexOf(TemplateSet.MACRO_START,doubleNested+1);
                    }
                }
                // now look for the end of the value, starting after the end of the nested macro
                marker = macroVars.indexOf(TemplateSet.MACRO_LET,nestedMacroEnd+1);
                if (marker < delimPos) marker = macroVars.length();
                
                // keep checking, there might be another one we need to skip.
                nestedMacro = macroVars.indexOf(TemplateSet.MACRO_START,nestedMacroEnd+1);
            }
            
            varValue = macroVars.substring(delimPos,marker);
            macro.set(varName,varValue);
            
            if (jumpMarker) {
            	jumpMarker = false;
            	marker += closeMarker.length();
            }
        }

        // this will trigger a standard expansion which will
        // properly reference all ancestor tag values.
        // otherwise defaults will be used incorrectly when there
        // are actually expansion values available up/down the tree.
        String macroTag = getNextMacroTag();
        this.set(macroTag, macro);
        return this.tagStart + macroTag + this.tagEnd;
    }

    private int macroCounter = 0;

    private String getNextMacroTag()
    {
        // these tags are unlikely to collide with application-supplied tags
        String macroTag = "CHUNK_-_MACRO_-_"+macroCounter;
        macroCounter++;
        return macroTag;
    }

    private int findMatchingEndBrace(String template, int searchFrom)
    {
        int endPos = template.indexOf(tagEnd,searchFrom);
        if (endPos < 0) return endPos;

        // tricky business: ignore all tag markers inside a regex.
        // also, preserve literals.

        // search backwards for regex start
        int x = endPos;
        
        /**
         * literals are intercepted higher up the chain now.
         * no need to scan for them here.
        // {~.^} or {~.literal}
        if (x - searchFrom == 1 && template.substring(searchFrom,x).equals(".")
       		|| x - searchFrom == 8 && template.substring(searchFrom,x).equals(".literal")) {
        	x = template.indexOf(TemplateSet.LITERAL_END_EXPANDED,x+1);
        	if (x < 0) return x;
        	return x + TemplateSet.LITERAL_END_EXPANDED.length() - 1;
        }
         */
        
        int regexPos = 0;
        boolean isMatchOnly = false;
        while (regexPos == 0 && x > searchFrom+1) {
            char c = template.charAt(x);
            if (c == '/') {
                char preC = template.charAt(x-1);
                if (preC == 's' && template.charAt(x-2) == '|') {
                    regexPos = x-1;
                } else {
                    if (preC == 'm') preC = template.charAt(x-2); // skip over optional m
                    if (preC == ',' || preC == '(') {
                        regexPos = x-1;
                        isMatchOnly = true;
                    }
                }
            }
            x--;
        }
        // no regex? valid endPos
        if (regexPos == 0) return endPos;

        // found regex? find end, make sure this end brace is not inside the regex.
        int regexMid = TextFilter.nextRegexDelim(template,regexPos+2);
        int regexEnd = (isMatchOnly) ? regexMid : TextFilter.nextRegexDelim(template,regexMid+1);

        if (endPos > regexEnd) {
            // brace is outside the regex, valid endpoint
            return endPos;
        } else {
            // invalid brace, is inside regex.  recurse from here.
            return findMatchingEndBrace(template, regexEnd+1);
        }
    }

    // the core search-and-replace routine
    private String explodeString(String template, Vector<Chunk> ancestors, int depth)
    {
        template = expandMacros(template);

        StringBuilder buf = new StringBuilder();

        int begin, end;
        int tagStartLen = tagStart.length();
        int tagEndLen = tagEnd.length();

        int marker = 0;
        while ((begin = template.indexOf(tagStart,marker)) > -1) {
            // found a tag.  everything up to here has no more tags,
            // so... put in the can!
            if (begin > marker) buf.append(template.substring(marker, begin));

            begin += tagStartLen;
            // find end of tag
            if ((end = findMatchingEndBrace(template,begin)) > -1) {
                String tagName = template.substring(begin,end);
                try {
                    Object tagValue = resolveTagValue(tagName, ancestors);
                    // unresolved tags get put back the way we found
                    // them in case the final String which explode() returns
                    // is then fed into another Chunk which *does* have
                    // a value.
                    if (tagValue == null) {
                        buf.append(tagStart);
                        buf.append(tagName);
                        buf.append(tagEnd);
                    } else {
                        explodeAndAppend(tagValue, buf, ancestors, depth+1);
                    }
                    marker = end + tagEndLen;
                } catch (BlockTagException e) {
                    BlockTagHelper helper = e.getHelper();
                    int[] blockEnd = findBlockEnd(template,end,helper);
                    if (blockEnd == null) {
                        // FAIL...
                        buf.append("<!-- [tag expansion error! "+e.getTagFunction()+" block with no matching end marker! ] -->");
                        marker = end + tagEndLen;
                    } else {
                        String blockBody = template.substring(end+1,blockEnd[0]);
                        String cooked = helper.cookBlock(blockBody);
                        explodeAndAppend(cooked, buf, ancestors, depth+1);
                        marker = blockEnd[1];
                    }
                }
            } else {
                // somebody didn't end a tag...
                // leave broken tagstart and move along
                buf.append(tagStart);
                marker = begin;
            }
        }
        if (marker == 0) {
            // no tags found
            return template;
        } else {
            buf.append(template.substring(marker));
            return buf.toString();
        }
    }
    
    private int[] findBlockEnd(String template, int blockStartPos, BlockTagHelper helper)
    {
        String endBlock = helper.getBlockEndMarker();
        String scanFor = tagStart + "." + endBlock + tagEnd;
        
        int endMarkerPos = template.indexOf(scanFor, blockStartPos);
        if (endMarkerPos > 0) {
            return new int[]{endMarkerPos,endMarkerPos+scanFor.length()};
        } else {
            return null;
        }
    }
    
    /**
     * literals are now caught higher up the chain
     * no need to process them and then unprocess them anymore.
    private String makePrettyLiteral(String literal)
    {
    	// turn {~.literal}...{~.} back into {^literal}...{^}
    	// and turn {~.^}...{~.} back into {^^}...{^}
    	String pretty = tagStart + literal + tagEnd;
    	if (pretty.startsWith("{~.")) {
    		pretty = TemplateSet.PROTOCOL_SHORTHAND + pretty.substring(3);
    	}
    	int endMarker = pretty.lastIndexOf(TemplateSet.LITERAL_END_EXPANDED);
    	if (endMarker > -1) {
    		pretty = pretty.substring(0,endMarker) + TemplateSet.LITERAL_END;
    	}
    	return pretty;
    }
     */

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

    private Vector<Chunk> ancestorStack = null;

    public Object get(Object key)
    {
        try {
            return resolveTagValue((String)key,ancestorStack);
        } catch (BlockTagException e) {
            return null;
        }
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
        Object x = getTag(key);
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
     * Assumes keys are strings and values are either type String
     * or type Chunk.
     */
    public void setMultiple(Hashtable<String,Object> rules)
    {
        if (rules == null || rules.size() <= 0) return;
        Enumeration<String> e = rules.keys();
        while (e.hasMoreElements()) {
            String tagName = e.nextElement();
            set(tagName,rules.get(tagName),"");
        }
    }

    /**
     * Adds multiple find-and-replace rules using all rules from the passed
     * Chunk.  Replaces any existing rules with the same tagName.
     */
    public void setMultiple(Chunk copyFrom)
    {
        if (copyFrom != null) {
            Hashtable<String,Object> h = copyFrom.getTagsTable();
            setMultiple(h);
        }
    }

    /**
     * Retrieve all find-and-replace rules.  Alterations to the returned
     * Hashtable WILL AFFECT the tag replacement rules of the Chunk directly.
     * Does not return a clone.
     * @return a Hashtable containing the Chunk's find-and-replace rules.
     */
    public Hashtable<String,Object> getTagsTable()
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
     * {^tagStack} debug feature, for template writers
     * to see what tags are available during development.
     * use {^tagStack(html)} to make output more readable in a web page.
     * 
     * @param format
     * @return
     */
    private String formatTagStack(String format, Vector<Chunk> ancestors)
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
		
		if (ancestors != null) {
			for (int i=ancestors.size()-1; i>=0; i--) {
				Chunk ancestor = ancestors.get(i);
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
			output.append('~');
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
    	addData(smartObj, null);
    }
    
    /**
     * Two smart objects of the same type in a single template?
     * 
     * No problem. Provide a unique altPrefix for each one
     * to avoid tag namespace collisions.
     * 
     * @param smartObj
     * @param altPrefix
     */
    public void addData(DataCapsule smartObj, String altPrefix)
    {
    	if (smartObj == null) return;
    	
    	DataCapsuleReader reader = DataCapsuleReader.getReader(smartObj);
    	
    	String[] tags = reader.getColumnLabels(altPrefix);
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
    
    public String makeTag(String tagName)
    {
        return tagStart + tagName + tagEnd;
    }
    
    public boolean isConforming()
    {
    	if (tagStart.equals(TemplateSet.DEFAULT_TAG_START)) return true;
    	return false;
    }
    
    /**
     * Useful utility function.  An efficient find-and-replace-all algorithm
     * for simple cases when regexp would be overkill.  IMO they should have
     * included this with String.
     * @param x text body.
     * @param find text to search for in x.
     * @param replace text to insert in place of "find" -- defaults to empty String if null is passed.
     * @return a new String based on x with all instances of "find" replaced with "replace"
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

}
