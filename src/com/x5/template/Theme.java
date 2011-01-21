package com.x5.template;

import java.util.ArrayList;
import java.util.HashSet;

public class Theme implements ContentSource, ChunkFactory
{
	private ArrayList<TemplateSet> themeLayers = new ArrayList<TemplateSet>();
	private String themesFolder;
	
	private String tagStart = TemplateSet.DEFAULT_TAG_START;
	private String tagEnd = TemplateSet.DEFAULT_TAG_END;
	
	public Theme(String themesFolder, String themeLayerNames)
	{
        // ensure trailing fileseparator
        char lastChar = themesFolder.charAt(themesFolder.length()-1);
        char fs = System.getProperty("file.separator").charAt(0);
        if (lastChar != '\\' && lastChar != '/' && lastChar != fs) {
            themesFolder += fs;
        }
        this.themesFolder = themesFolder;
		
		String[] layerNames = parseLayerNames(themeLayerNames);
		if (layerNames == null) {
			TemplateSet simple = new TemplateSet(themesFolder);
			themeLayers.add(simple);
		} else {
			for (int i=0; i<layerNames.length; i++) {
				TemplateSet x = new TemplateSet(this.themesFolder + layerNames[i]);
				// important: do not return pretty HTML-formatted error strings
				// when template can not be located.
				x.signalFailureWithNull();
				themeLayers.add(x);
			}
		}
	}
	
	public Theme(String themesFolder, String themeLayerNames, String ext)
	{
        // ensure trailing fileseparator
        char lastChar = themesFolder.charAt(themesFolder.length()-1);
        char fs = System.getProperty("file.separator").charAt(0);
        if (lastChar != '\\' && lastChar != '/' && lastChar != fs) {
            themesFolder += fs;
        }
        this.themesFolder = themesFolder;
		
		String[] layerNames = parseLayerNames(themeLayerNames);
		if (layerNames == null) {
			TemplateSet simple = new TemplateSet(themesFolder,ext,0);
			themeLayers.add(simple);
		} else {
			for (int i=0; i<layerNames.length; i++) {
				TemplateSet x = new TemplateSet(this.themesFolder + layerNames[i],ext,0);
				// important: do not return pretty HTML-formatted error strings
				// when template can not be located.
				x.signalFailureWithNull();
				themeLayers.add(x);
			}
		}
	}
	
	private String[] parseLayerNames(String themeLayerNames)
	{
		if (themeLayerNames == null) return null;
		
		return themeLayerNames.split(" *, *");
	}
	
	public void setDirtyInterval(int minutes)
	{
		// propagate setting down to each layer
		for (TemplateSet set: themeLayers) {
			set.setDirtyInterval(minutes);
		}
	}
	
	public String getSnippet(String templateName, String ext)
	{
		// later layers have precedence if they provide the item
		for (int i=themeLayers.size()-1; i>=0; i--) {
			TemplateSet x = themeLayers.get(i);
			String template = x.getSnippet(templateName, ext);
			if (template != null) {
				return template;
			}
		}
		return prettyFail(templateName, ext);
	}

	public String getSnippet(String itemName)
	{
		// later layers have precedence if they provide the item
		for (int i=themeLayers.size()-1; i>=0; i--) {
			TemplateSet x = themeLayers.get(i);
			String template = x.fetch(itemName);
			if (template != null) {
				return template;
			}
		}
		return prettyFail(itemName, null);
	}
	
	public boolean provides(String itemName)
	{
		for (int i=themeLayers.size()-1; i>=0; i--) {
			TemplateSet x = themeLayers.get(i);
			if (x.provides(itemName)) return true;
		}
		return false;
	}
	
	private String prettyFail(String templateName, String ext)
	{
		String prettyExt = ext;
		if (prettyExt == null) {
			TemplateSet baseLayer = themeLayers.get(0);
			prettyExt = baseLayer.getDefaultExtension();
		}
		
		StringBuilder err = new StringBuilder();
	    err.append("[");
	    err.append(prettyExt);
	    err.append(" template '");
	    err.append(templateName);
	    err.append("' not found]<!-- looked in [");
	    for (int i=themeLayers.size()-1; i>=0; i--) {
	    	if (i < themeLayers.size()-1) { err.append(","); }
	    	TemplateSet x = themeLayers.get(i);
	    	err.append(x.getTemplatePath(templateName,prettyExt));
	    }
	    
	    err.append("] -->");
	    return err.toString();
	}
	
	public String fetch(String itemName)
	{
		return getSnippet(itemName);
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
        c.setTagBoundaries(tagStart,tagEnd);
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
        c.setTagBoundaries(tagStart,tagEnd);
        c.setMacroLibrary(this,this);
        c.append( fetch(templateName) );
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
        c.setTagBoundaries(tagStart,tagEnd);
        c.setMacroLibrary(this,this);
        c.append( getSnippet(templateName, extension) );
        shareContentSources(c);
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

	public String makeTag(String tagName)
	{
        return tagStart + tagName + tagEnd;
	}

}