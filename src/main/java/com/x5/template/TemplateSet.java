package com.x5.template;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.x5.template.filters.ChunkFilter;
import com.x5.template.filters.RegexFilter;
import com.x5.template.providers.TranslationsProvider;
import com.x5.util.JarResource;
import com.x5.util.Path;

// Project Title: Chunk
// Description: Template Util
// Copyright: Copyright (c) 2007
// Author: Tom McClure

/**
 * TemplateSet is a Chunk "factory" and an easy way to parse
 * template files into Strings.  The default caching behavior is
 * great for high traffic applications.
 *
 * <PRE>
 * // Dynamic content in templates is marked with {~...}
 * //
 * // Previously the syntax was {$...} but then I had a project where
 * // some of the templates were shared by a perl script and I got
 * // tired of escaping the $ signs in inline templates in my perl code.
 * //
 * // Before that there was an escaped-HTML-inspired syntax which
 * // looked like &tag_...; since this was thought to be most
 * // compatible with HTML editors like DreamWeaver but it was hard
 * // to read and as it turned out DreamWeaver choked on it.
 * //
 * // See TemplateSet.convertTags(...) and .convertToMyTags(...) for
 * // quick search-and-replace routines for updating tag syntax on
 * // old templates...
 * //
 * // ...Or, if an entire template set uses another syntax just call
 * // .setTagBoundaries("{$", "}") on the TemplateSet object before
 * // you use it to make any chunks.  All subsequent chunks made from
 * // that TemplateSet will find and replace the {$...} style tags.
 * //
 * // Be careful: for interoperability you will need to call
 * // .setTagBoundaries() individually on any blank chunks you make
 * // without the aid of the TemplateSet object, ie with the Chunk
 * // constructor -- better to use the no-arg .makeChunk() method of
 * // the TemplateSet instead, since that automatically coerces the
 * // blank Chunk's tag boundaries correctly.
 * //
 * ///// In summary, for back-compatibility:
 * //
 * // TemplateSet templates = new TemplateSet(...);
 * // templates.setTagBoundaries("{$", "}");
 * // ...
 * //
 * // *** (A) BAD ***
 * // ...
 * // Chunk c = new Chunk(); // will only explode tags like default {~...}
 * //
 * // *** (B) NOT AS BAD ***
 * // ...
 * // Chunk c = new Chunk();
 * // c.setTagBoundaries("{$", "}"); // manually match tag format :(
 * //
 * // *** (C) BEST ***
 * // ...
 * // Chunk c = templates.makeChunk(); // inherits TemplateSet's tag format :)
 * //
 * </PRE>
 *
 * Copyright: waived, free to use<BR>
 * Company: <A href="http://www.x5dev.com/">X5 Software</A><BR>
 * Updates: <A href="http://www.x5dev.com/chunk/wiki/">Chunk Documentation</A><BR>
 *
 * @author Tom McClure
 */

public class TemplateSet implements ContentSource, ChunkFactory
{

    public static String DEFAULT_TAG_START = "{$";
    public static String DEFAULT_TAG_END = "}";
    public static final String INCLUDE_SHORTHAND = "{+";
    public static final String PROTOCOL_SHORTHAND = "{.";

    // allow {^if}...{/if} and {^loop}...{/loop} by auto-expanding these
    public static final String BLOCKEND_SHORTHAND = "{/";
    public static final String BLOCKEND_LONGHAND = "{~./"; // ie "{^/"

    private static final long oneMinuteInMillis = 60 * 1000;
    // having a minimum cache time of five seconds improves
    // performance by avoiding typical multiple parses of a
    // file for its subtemplates in a short span of code.
    private static final long MIN_CACHE = 5 * 1000;

    private Hashtable<String,Snippet> cache = new Hashtable<String,Snippet>();
    private Hashtable<String,Long> cacheFetch = new Hashtable<String,Long>();
    private int dirtyInterval = 0; // minutes
    private String defaultExtension = null;
    private String tagStart = DEFAULT_TAG_START;
    private String tagEnd = DEFAULT_TAG_END;
    private String classpathThemesFolder = Path.ensureTrailingPathSeparator("/" + Theme.DEFAULT_THEMES_FOLDER);
    private String templatePath = System.getProperty("templateset.folder","");
    private String layerName = null;

    private Class<?> classInJar = null;
    private Object resourceContext = null;

    private boolean prettyFail = true;
    private boolean hardFail = false;
    private String expectedEncoding = TemplateDoc.getDefaultEncoding();

    public TemplateSet() {}

    /**
     * Makes a template "factory" which reads in template files from the
     * file system in the templatePath folder.  Caches for refreshMins.
     * Uses "extensions" for the default file extension (do not include dot).
     * @param classpathThemesFolder folder where template files are located on classpath.
     * @param templatePath  folder where template files are located.
     * @param extension     appends dot plus this String to a template name stub to find template files.
     * @param refreshMins   returns template from cache unless this many minutes have passed.
     */
    public TemplateSet(String classpathThemesFolder, String templatePath, String extension, int refreshMins)
    {
        this(templatePath, extension, refreshMins);
        this.classpathThemesFolder = Path.ensureTrailingPathSeparator(classpathThemesFolder);
    }

    public TemplateSet(String templatePath, String extension, int refreshMins)
    {
        this.templatePath = Path.ensureTrailingFileSeparator(templatePath);
        this.dirtyInterval = refreshMins;
        this.defaultExtension = extension;
    }

    /**
     * Retrieve as String the template specified by name.
     * If name contains one or more dots it is assumed that the template
     * definition is nested inside another template.  Everything up to the
     * first dot is part of the filename (appends the DEFAULT extension to
     * find the file) and everything after refers to a location within the
     * file where the template contents are defined.
     * <P>
     * For example: String myTemplate = templateSet.get("outer_file.inner_template");
     * <P>
     * will look for {#inner_template}bla bla bla{#} inside the file
     * "outer_file.html" or "outer_file.xml" ie whatever your TemplateSet extension is.
     * @param name the location of the template definition.
     * @return the template definition from the file as a String
     */
    public Snippet getSnippet(String name)
    {
        if (name.charAt(0) == ';') {
            int nextSemi = name.indexOf(';',1);
            if (nextSemi < 0) {
                // missing delimiter
                return getSnippet(name, defaultExtension);
            } else {
                String tpl = name.substring(nextSemi+1);
                String ext = name.substring(1,nextSemi);
                return getSnippet(tpl, ext);
            }
        } else {
            return getSnippet(name, defaultExtension);
        }
    }

    public String fetch(String name)
    {
        Snippet s = getCleanTemplate(name);
        if (s == null) return null;
        // otherwise...
        return s.toString();
    }

    public String getProtocol()
    {
        return "include";
    }

    private Snippet getCleanTemplate(String name)
    {
        return getSnippet(name, "_CLEAN_:"+defaultExtension);
    }

    /**
     * Retrieve as String the template specified by name and extension.
     * If name contains one or more dots it is assumed that the template
     * definition is nested inside another template.  Everything up to the
     * first dot is part of the filename (appends the PASSED extension to
     * find the file) and everything after refers to a location within the
     * file where the template contents are defined.
     * @param name the location of the template definition.
     * @param extension the nonstandard extension which forms the template filename.
     * @return the template definition from the file as a String
     */
    public Snippet getSnippet(String name, String extension)
    {
        return _get(name, extension, this.prettyFail);
    }

    private void importTemplates(InputStream in, String stub, String extension)
    throws IOException
    {
        TemplateDoc doc = new TemplateDoc(stub, in);
        for (TemplateDoc.Doclet doclet : doc.parseTemplates(expectedEncoding)) {
            cacheTemplate(doclet, extension);
        }
    }

    private Snippet _get(String name, String extension, boolean prettyFail)
    {
        Snippet template = getFromCache(name, extension);
        String filename = null;

        // if not in cache, parse file and place all pieces in cache
        if (template == null) {
            String stub = TemplateDoc.truncateNameToStub(name);

            filename = getTemplatePath(name,extension);
            char fs = System.getProperty("file.separator").charAt(0);
            filename = filename.replace('\\',fs);
            filename = filename.replace('/',fs);
            try {
                File templateFile = new File(filename);
                if (templateFile.exists()) {
                    FileInputStream in = new FileInputStream(templateFile);
                    importTemplates(in, stub, extension);
                    in.close();
                    template = getFromCache(name, extension);
                } else {
                    // file does not exist, check around in classpath/jars
                    String resourcePath = getResourcePath(name,extension);
                    InputStream inJar = null;

                    if (classInJar == null) {
                        // theme resource is probably in same
                        // vicinity as calling class.
                        classInJar = grokCallerClass();
                    }

                    // ideally, somebody called Theme.setJarContext(this.getClass())
                    // and we have a pointer to the jar where the templates live.
                    if (classInJar != null) {
                        inJar = classInJar.getResourceAsStream(resourcePath);
                    }

                    // last ditch effort, check in surrounding jars in classpath...
                    if (inJar == null) inJar = fishForTemplate(resourcePath);

                    if (inJar != null) {
                        importTemplates(inJar, stub, extension);
                        template = getFromCache(name, extension);
                        inJar.close();
                    }
                }
            } catch (java.io.IOException e) {
                StringBuilder errmsg = new StringBuilder("error fetching ");
                errmsg.append(extension);
                errmsg.append(" template '");
                errmsg.append(name);
                errmsg.append("'");

                if (hardFail) {
                    throw new TemplateNotFoundException(errmsg.toString(), e);
                }

                if (!prettyFail) return null;

                StringWriter w = new StringWriter();
                e.printStackTrace(new PrintWriter(w));
                StringBuilder trace = new StringBuilder();
                trace.append("<!-- ");
                trace.append(w.toString());
                trace.append(" -->");

                template = Snippet.getSnippet("[" + errmsg.toString() + "]" + trace.toString());
            }
        }

        if (template == null) {
            StringBuilder errmsg = new StringBuilder();
            errmsg.append(extension);
            errmsg.append(" template '");
            errmsg.append(name);
            errmsg.append("' not found");

            if (hardFail) {
                throw new TemplateNotFoundException(errmsg.toString() + ". Looked in: " + filename);
            }

            if (!prettyFail) return null;

            StringBuilder details = new StringBuilder();
            details.append("<!-- looked in [");
            details.append(filename);
            details.append("] -->");

            template = Snippet.getSnippet("[" + errmsg.toString() + "]" + details.toString());
        }

        return template;
    }

    // default (package) visibility intentional
    static Class<?> grokCallerClass()
    {
        Throwable t = new Throwable();
        StackTraceElement[] stackTrace = t.getStackTrace();
        if (stackTrace == null) return null;

        // calling class is at least four call levels back up the stack trace.
        // makes an excellent candidate for where to look for theme resources.
        for (int i=4; i<stackTrace.length; i++) {
            StackTraceElement e = stackTrace[i];
            if (e.getClassName().matches("^com\\.x5\\.template\\.[^\\.]*$")) {
                continue;
            }
            try {
                return Class.forName(e.getClassName());
            } catch (ClassNotFoundException e2) {}
        }
        return null;
    }

    // should run a benchmark and see how expensive this is...
    private InputStream fishForTemplate(String resourcePath)
    {
        if (resourceContext != null) {
            InputStream in = fishForTemplateInContext(resourcePath);
            if (in != null) return in;
        }

        // fish around for this resource in other jars in the classpath
        String cp = System.getProperty("java.class.path");
        if (cp == null) return null;

        String[] jars = cp.split(":");
        if (jars == null) return null;

        for (String jar : jars) {
            if (jar.endsWith(".jar")) {
                InputStream in = JarResource.peekInsideJar("jar:file:"+jar, resourcePath);
                if (in != null) return in;
            }
        }

        return null;
    }

    /**
     * fishForTemplateInContext is able to use reflection to call methods
     * in javax.http.ResourceContext without actually requiring the class
     * to be present/loaded during compile.
     */
    @SuppressWarnings("rawtypes")
    private InputStream fishForTemplateInContext(String resourcePath)
    {
        // call getResourceAsStream via reflection
        Class<?> ctxClass = resourceContext.getClass();
        Method m = null;
        try {
            final Class[] oneString = new Class[]{String.class};
            m = ctxClass.getMethod("getResourceAsStream", oneString);
            if (m != null) {
                InputStream in = (InputStream)m.invoke(resourceContext, new Object[]{resourcePath});
                if (in != null) return in;
            }

            // no dice, start peeking inside jars in WEB-INF/lib/
            m = ctxClass.getMethod("getResourcePaths", oneString);
            if (m != null) {
                Set paths = (Set)m.invoke(resourceContext, new Object[]{"/WEB-INF/lib"});
                if (paths != null) {
                    for (Object urlString : paths) {
                        String jar = (String)urlString;
                        if (jar.endsWith(".jar")) {
                            m = ctxClass.getMethod("getResource", oneString);
                            URL jarURL = (URL)m.invoke(resourceContext, new Object[]{jar});
                            InputStream in = JarResource.peekInsideJar("jar:"+jarURL.toString(), resourcePath);
                            if (in != null) return in;
                        }
                    }
                }
            }

        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }

        return null;
    }

    /**
     * Creates a Chunk with no starter template and sets its tag boundary
     * markers to match the other templates in this set.  The Chunk will need
     * to obtain template pieces via its .append() method.
     * @return blank Chunk.
     */
    public Chunk makeChunk()
    {
        Chunk c = new Chunk();
        c.setMacroLibrary(this,this);
        shareContentSources(c);
        return c;
    }

    /**
     * Creates a Chunk with a starting template.  If templateName contains one
     * or more dots it is assumed that the template definition is nested inside
     * another template.  Everything up to the first dot is part of the filename
     * (appends the DEFAULT extension to find the file) and everything after
     * refers to a location within the file where the template contents are
     * defined.
     *
     * @param templateName the location of the template definition.
     * @return a Chunk pre-initialized with a snippet of template.
     */
    public Chunk makeChunk(String templateName)
    {
        Chunk c = new Chunk();
        c.setMacroLibrary(this,this);
        c.append( getSnippet(templateName) );
        shareContentSources(c);
        return c;
    }

    /**
     * Creates a Chunk with a starting template.  If templateName contains one
     * or more dots it is assumed that the template definition is nested inside
     * another template.  Everything up to the first dot is part of the filename
     * (appends the PASSED extension to find the file) and everything after
     * refers to a location within the file where the template contents are
     * defined.
     *
     * @param templateName the location of the template definition.
     * @param extension the nonstandard extension which forms the template filename.
     * @return a Chunk pre-initialized with a snippet of template.
     */
    public Chunk makeChunk(String templateName, String extension)
    {
        Chunk c = new Chunk();
        c.setMacroLibrary(this,this);
        c.append( getSnippet(templateName, extension) );
        shareContentSources(c);
        return c;
    }

    private void cacheTemplate(TemplateDoc.Doclet doclet, String extension)
    {
        String name = doclet.getName().replace('#','.');
        String ref = extension + "." + name;
        String cleanRef = "_CLEAN_:" + ref;
        String template = doclet.getTemplate();
        cache.put(cleanRef, Snippet.makeLiteralSnippet(template));
        cacheFetch.put(cleanRef, System.currentTimeMillis());

        StringBuilder tpl = TemplateDoc.expandShorthand(name,new StringBuilder(template));
        if (tpl == null) return;
        String fastTpl = removeBlockTagIndents(tpl.toString());
        cache.put(ref, Snippet.getSnippet(fastTpl, doclet.getOrigin()));
        cacheFetch.put(ref, System.currentTimeMillis());
    }

    public static String removeBlockTagIndents(String template)
    {
        // this regex: s/^\s*({^\/?(...)[^}]*})\s*/$1/g removes leading and trailing whitespace
        // from lines that only contain {^loop} ...
        // NB: this regex will not catch {^if (~tag =~ /\/x{1,3}/)} but it's already nigh-unreadable...
        return RegexFilter.applyRegex(template, "s/^[ \\t]*(\\{(\\% *(\\~\\.)?(end)?|(\\^|\\~\\.)\\/?)(loop|exec|if|else|elseIf|divider|onEmpty|body|data)([^\\}]*|[^\\}]*\\/[^\\/]*\\/[^\\}]*)\\})[ \\t]*$/$1/gmi");
    }

    protected Snippet getFromCache(String name, String extension)
    {
        String ref = extension + "." + name.replace('#','.');
        Snippet template = null;

        long cacheHowLong = dirtyInterval * oneMinuteInMillis;
        if (cacheHowLong < MIN_CACHE) cacheHowLong = MIN_CACHE;

        if (cache.containsKey(ref)) {
            long lastFetch = cacheFetch.get(ref); // millis
            long expireTime = lastFetch + cacheHowLong;
            if (System.currentTimeMillis() < expireTime) {
                template = cache.get(ref);
            }
        }
        return template;
    }

    /**
     * Forces subsequent template fetching to re-read the template contents
     * from the filesystem instead of the cache.
     */
    public void clearCache()
    {
        cache.clear();
        cacheFetch.clear();
    }

    /**
     * Controls caching behavior.  Set to zero to minimize caching.
     * @param minutes how long to keep a template in the cache.
     */
    public void setDirtyInterval(int minutes)
    {
        dirtyInterval = minutes;
    }

    /**
     * Converts a template with an alternate tag syntax to one that matches
     * this TemplateSet's tags.
     * @param withOldTags Template text which contains tags with the old syntax
     * @param oldTagStart old tag beginning marker
     * @param oldTagEnd old tag end marker
     * @return template with tags converted
     */
    public String convertToMyTags(String withOldTags, String oldTagStart, String oldTagEnd)
    {
        return convertTags(withOldTags, oldTagStart, oldTagEnd, this.tagStart, this.tagEnd);
    }

    /**
     * Converts a template with an alternate tag syntax to one that matches
     * the default tag syntax {~myTag}.
     * @param withOldTags Template text which contains tags with the old syntax
     * @param oldTagStart old tag beginning marker
     * @param oldTagEnd old tag end marker
     * @return template with tags converted
     */
    public static String convertTags(String withOldTags, String oldTagStart, String oldTagEnd)
    {
        return convertTags(withOldTags, oldTagStart, oldTagEnd,
                           DEFAULT_TAG_START, DEFAULT_TAG_END);
    }

    /**
     * Converts a template from one tag syntax to another.
     * @param withOldTags Template text which contains tags with the old syntax
     * @param oldTagStart old tag beginning marker
     * @param oldTagEnd old tag end marker
     * @param newTagStart new tag beginning marker
     * @param newTagEnd new tag end marker
     * @return template with tags converted
     */
    public static String convertTags(String withOldTags, String oldTagStart, String oldTagEnd,
                                     String newTagStart, String newTagEnd)
    {
        StringBuilder converted = new StringBuilder();
        int j, k, marker = 0;
        while ((j = withOldTags.indexOf(oldTagStart,marker)) > -1) {
            converted.append(withOldTags.substring(marker,j));
            marker = j + oldTagStart.length();
            if ((k = withOldTags.indexOf(oldTagEnd)) > -1) {
                converted.append(newTagStart);
                converted.append(withOldTags.substring(marker,k));
                converted.append(newTagEnd);
                marker = k + oldTagEnd.length();
            } else {
                converted.append(oldTagStart);
            }
        }
        if (marker == 0) {
            return withOldTags;
        } else {
            converted.append(withOldTags.substring(marker));
            return converted.toString();
        }
    }

    public TemplateSet getSubset(String context)
    {
        return new TemplateSetSlice(this, context);
    }

    // chunk factory now supports sharing content sources with its factory-created chunks
    private HashSet<ContentSource> altSources = null;
    private TranslationsProvider translationsProvider = null;

    public void setTranslationsProvider(TranslationsProvider provider) {
        this.translationsProvider = provider;
    }

    public void addProtocol(ContentSource src)
    {
        if (altSources == null) altSources = new HashSet<ContentSource>();
        altSources.add(src);
    }

    private void shareContentSources(Chunk c)
    {
        if (altSources != null) {
            java.util.Iterator<ContentSource> iter = altSources.iterator();
            while (iter.hasNext()) {
                ContentSource src = iter.next();
                c.addProtocol(src);
            }
        }
        if (translationsProvider != null) {
            c.setTranslationsProvider(translationsProvider);
        }
    }

    public void signalFailureWithNull()
    {
        this.prettyFail = false;
    }

    public void setHardFail(boolean hardFail)
    {
        this.hardFail = hardFail;
    }

    public String getTemplatePath(String templateName, String ext)
    {
        String stub = TemplateDoc.truncateNameToStub(templateName);
        String path = templatePath + stub;
        if (ext != null && ext.length() > 0) {
            path += '.' + ext;
        }
        return path;
    }

    public String getResourcePath(String templateName, String ext)
    {
        String stub = TemplateDoc.truncateNameToStub(templateName);
        String path;
        if (layerName == null) {
            path = classpathThemesFolder + stub;
        } else {
            path = classpathThemesFolder + layerName + stub;
        }
        if (ext != null && ext.length() > 0) {
            path += '.' + ext;
        }
        return path;
    }


    public String getDefaultExtension()
    {
        return this.defaultExtension;
    }

    public boolean provides(String itemName)
    {
        Snippet found = _get(itemName, defaultExtension, false);
        if (found == null) {
            return false;
        } else {
            return true;
        }
    }

    public void setJarContext(Class<?> classInSameJar)
    {
        this.classInJar = classInSameJar;
    }

    public void setJarContext(Object ctx)
    {
        // an object with an InputStream getResourceAsStream(String) method
        this.resourceContext = ctx;
    }

    public void setLayerName(String layerName)
    {
        this.layerName = Path.ensureTrailingFileSeparator(layerName);
    }

    public void setEncoding(String encoding)
    {
        this.expectedEncoding = encoding;
    }

    public Map<String,ChunkFilter> getFilters()
    {
        return null;
    }

}
