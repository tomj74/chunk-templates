package com.x5.template;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.x5.template.filters.ChunkFilter;

public class Theme implements ContentSource, ChunkFactory
{
    private ArrayList<ContentSource> themeLayers = new ArrayList<ContentSource>();

    private String themesFolder;
    private String themeLayerNames;
    private String fileExtension;

    private static final String DEFAULT_THEMES_FOLDER = "themes";

    private String localeCode = null;
    private boolean renderErrs = true;
    private PrintStream errLog = null;

    public Theme()
    {
        this(null, null, null);
    }

    public Theme(ContentSource templates)
    {
        themeLayers.add(templates);
    }
    
    public Theme(String themeLayerNames)
    {
        this(null, themeLayerNames, null);
    }

    public Theme(String themesFolder, String themeLayerNames)
    {
        this(themesFolder, themeLayerNames, null);
    }

    public Theme(String themesFolder, String themeLayerNames, String ext)
    {
        this.themesFolder = themesFolder;
        this.themeLayerNames = themeLayerNames;
        this.fileExtension = ext;
    }

    public void setDefaultFileExtension(String ext)
    {
        if (this.themeLayers.size() > 0) {
            throw new java.lang.IllegalStateException("Must specify extension before lazy init.");
        } else {
            this.fileExtension = ext;
        }
    }

    public void setLocale(String localeCode)
    {
        this.localeCode = localeCode;
    }

    public String getLocale()
    {
        return this.localeCode;
    }

    /**
     * What encoding do this theme's template files use?
     * If not UTF-8, make sure to set this explicitly.
     */
    public void setEncoding(String encoding)
    {
        ArrayList<TemplateSet> templateSets = getTemplateSets();
        if (templateSets != null) {
            for (TemplateSet layer : templateSets) {
                layer.setEncoding(encoding);
            }
        }
    }

    private void init()
    {
        if (themesFolder == null) themesFolder = DEFAULT_THEMES_FOLDER;
        // ensure trailing fileseparator
        char lastChar = themesFolder.charAt(themesFolder.length()-1);
        char fs = System.getProperty("file.separator").charAt(0);
        if (lastChar != '\\' && lastChar != '/' && lastChar != fs) {
            themesFolder += fs;
        }

        String[] layerNames = parseLayerNames(themeLayerNames);
        if (layerNames == null) {
            TemplateSet simple = new TemplateSet(themesFolder,fileExtension,0);
            if (!renderErrs) simple.signalFailureWithNull();
            themeLayers.add(simple);
        } else {
            for (int i=0; i<layerNames.length; i++) {
                TemplateSet x = new TemplateSet(this.themesFolder + layerNames[i],fileExtension,0);
                x.setLayerName(layerNames[i]);
                // important: do not return pretty HTML-formatted error strings
                // when template can not be located.
                x.signalFailureWithNull();
                themeLayers.add(x);
            }
        }
    }
    
    public void addLayer(ContentSource templates)
    {
        themeLayers.add(templates);
    }
    
    private ArrayList<ContentSource> getThemeLayers()
    {
        // funneling all access through here to enable lazy init.
        if (themeLayers.size() < 1) { init(); }
        return themeLayers;
    }

    private String[] parseLayerNames(String themeLayerNames)
    {
        if (themeLayerNames == null) return null;

        return themeLayerNames.split(" *, *");
    }

    public void setDirtyInterval(int minutes)
    {
        // propagate setting down to each layer
        ArrayList<TemplateSet> templateSets = getTemplateSets();
        if (templateSets != null) {
            for (TemplateSet layer : templateSets) {
                layer.setDirtyInterval(minutes);
            }
        }
    }

    public Snippet getSnippet(String templateName, String ext)
    {
        ArrayList<ContentSource> layers = getThemeLayers();
        // later layers have precedence if they provide the item
        for (int i=layers.size()-1; i>=0; i--) {
            ContentSource x = layers.get(i);
            Snippet template = x.getSnippet(";" + ext + ";" + templateName);
            if (template != null) {
                return template;
            }
        }
        return prettyFail(templateName, ext);
    }

    public Snippet getSnippet(String itemName)
    {
        ArrayList<ContentSource> layers = getThemeLayers();
        // later layers have precedence if they provide the item
        for (int i=layers.size()-1; i>=0; i--) {
            ContentSource x = layers.get(i);
            if (x.provides(itemName)) {
                return x.getSnippet(itemName);
            }
            /*
            String template = x.fetch(itemName);
            if (template != null) {
                return template;
            }*/
        }
        return prettyFail(itemName, null);
    }

    public boolean provides(String itemName)
    {
        for (int i=themeLayers.size()-1; i>=0; i--) {
            ContentSource x = themeLayers.get(i);
            if (x.provides(itemName)) return true;
        }
        return false;
    }

    private Snippet prettyFail(String templateName, String ext)
    {
        if (!renderErrs && errLog == null) return null;

        String prettyExt = ext;
        if (prettyExt == null) {
            ContentSource baseLayer = themeLayers.get(0);
            if (baseLayer instanceof TemplateSet) {
                prettyExt = ((TemplateSet)baseLayer).getDefaultExtension();
            }
        }

        StringBuilder err = new StringBuilder();
        err.append("[");
        if (prettyExt != null) {
            err.append(prettyExt);
            err.append(" ");
        }
        err.append("template '");
        err.append(templateName);
        err.append("' not found]");

        if (prettyExt != null) {
            String places = "";
            ArrayList<TemplateSet> templateSets = getTemplateSets();
            if (templateSets != null) {
                for (int i=templateSets.size()-1; i>=0; i--) {
                    TemplateSet ts = templateSets.get(i);
                    if (places.length() > 0) { places += ","; }
                    places += ts.getTemplatePath(templateName,prettyExt);
                }
            }
            if (places.length() > 0) {
                err.append("<!-- looked in [");
                err.append(places);
                err.append("] -->");
            }
        }

        if (errLog != null) {
            Chunk.logChunkError(errLog, err.toString());
        }

        return renderErrs ? Snippet.getSnippet(err.toString()) : null;
    }

    public String fetch(String itemName)
    {
        ArrayList<ContentSource> layers = getThemeLayers();
        // later layers have precedence if they provide the item
        for (int i=layers.size()-1; i>=0; i--) {
            ContentSource x = layers.get(i);
            if (x.provides(itemName)) {
                return x.fetch(itemName);
            }
        }
        return null;

        /*
        Snippet s = getSnippet(itemName);
        if (s == null) return null;
        return s.toString();
        */
    }

    public String getProtocol()
    {
        return "include";
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
        // make sure chunk inherits theme settings
        shareContentSources(c);
        c.setLocale(localeCode);
        c.setErrorHandling(renderErrs, errLog);
        return c;
    }

    /**
     * Creates a Chunk with a starting template.  If templateName contains one
     * or more hashes (#) it is assumed that the template definition is nested inside
     * another template.  Everything up to the first hash is part of the filename
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
        // make sure chunk inherits theme settings
        shareContentSources(c);
        c.setLocale(localeCode);
        c.setErrorHandling(renderErrs, errLog);
        return c;
    }

    /**
     * Creates a Chunk with a starting template.  If templateName contains one
     * or more hashes (#) it is assumed that the template definition is nested inside
     * another template.  Everything up to the first hash is part of the filename
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
        // make sure chunk inherits theme settings
        shareContentSources(c);
        c.setLocale(localeCode);
        c.setErrorHandling(renderErrs, errLog);
        return c;
    }

    // chunk factory now supports sharing content sources with its factory-created chunks
    private HashSet<ContentSource> altSources = null;

    public void addProtocol(ContentSource src)
    {
        if (altSources == null) altSources = new HashSet<ContentSource>();
        altSources.add(src);
    }

    private void shareContentSources(Chunk c)
    {
        if (altSources == null) return;
        java.util.Iterator<ContentSource> iter = altSources.iterator();
        while (iter.hasNext()) {
            ContentSource src = iter.next();
            c.addProtocol(src);
        }
    }

    /**
     * If your templates are packaged into a jar with your application code,
     * then you should use this method to tell chunk where your templates are.
     *
     * Chunk might still be able to find your templates but it will work a
     * lot harder.  Without this info, it has to peek into every jar in the
     * classpath every time it loads a new template.
     *
     * @param classInSameJar
     */
    public void setJarContext(Class<?> classInSameJar)
    {
        ArrayList<TemplateSet> templateSets = getTemplateSets();
        if (templateSets != null) {
            for (TemplateSet layer : templateSets) {
                layer.setJarContext(classInSameJar);
            }
        }
    }

    public void setJarContext(Object ctx)
    {
        ArrayList<TemplateSet> templateSets = getTemplateSets();
        if (templateSets != null) {
            for (TemplateSet layer : templateSets) {
                layer.setJarContext(ctx);
            }
        }
    }

    private ArrayList<TemplateSet> getTemplateSets()
    {
        ArrayList<TemplateSet> sets = null;
        for (ContentSource x : themeLayers) {
            if (x instanceof TemplateSet) {
                if (sets == null) sets = new ArrayList<TemplateSet>();
                sets.add((TemplateSet)x);
            }
        }
        return sets;
    }

    // now supporting user-contributed filters
    private Map<String,ChunkFilter> customFilters;

    public Map<String,ChunkFilter> getFilters()
    {
        return customFilters;
    }

    public void registerFilter(ChunkFilter filter)
    {
        if (customFilters == null) {
            customFilters = new HashMap<String,ChunkFilter>();
        }
        customFilters.put(filter.getFilterName(),filter);
        String[] aliases = filter.getFilterAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                customFilters.put(alias,filter);
            }
        }
    }

    public void setErrorHandling(boolean renderErrs, PrintStream errLog)
    {
        this.renderErrs = renderErrs;
        this.errLog = errLog;
    }

}