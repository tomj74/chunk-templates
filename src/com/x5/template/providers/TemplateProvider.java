package com.x5.template.providers;

import java.io.IOException;
import java.util.HashMap;

import com.x5.template.Snippet;
import com.x5.template.TemplateDoc;

public abstract class TemplateProvider implements com.x5.template.ContentSource
{
    private static String DEFAULT_EXTENSION = "chtml";
    private static String DEFAULT_ENCODING = "UTF-8";

    private String extension = DEFAULT_EXTENSION;
    private String encoding = DEFAULT_ENCODING;

    // cache compiled templates
    HashMap<String,Snippet> snippetCache = new HashMap<String,Snippet>();

    public String fetch(String templateName)
    {
        Snippet s = getSnippet(templateName);
        if (s == null) return null;
        return s.toString();
    }

    public boolean provides(String itemName)
    {
        Snippet x = getSnippet(itemName);
        return x != null;
    }

    public abstract String getProtocol();

    public Snippet getSnippet(String templateName)
    {
        if (snippetCache.containsKey(templateName)) {
            return snippetCache.get(templateName);
        }

        String rawTemplate = null;
        try {
            rawTemplate = loadItemDoc(templateName);
        } catch (IOException e) {
        	e.printStackTrace(System.err);
        }
        if (rawTemplate == null) {
            snippetCache.put(templateName, null);
            return null;
        }

        TemplateDoc doc = new TemplateDoc(templateName, rawTemplate);
        try {
            return parseSnippet(doc, templateName);
        } catch (IOException e) {
            return null;
        }
    }

    private Snippet parseSnippet(TemplateDoc doc, String snippetName)
    throws IOException
    {
        Snippet r = null;
        // pre-parse all contained templates into snippets for speed
        for (TemplateDoc.Doclet doclet : doc.parseTemplates(encoding)) {
            String templateKey = doclet.getName();
            Snippet s = doclet.getSnippet();
            if (templateKey.equals(snippetName)) {
                // this is the requested subtemplate, set aside for return
                r = s;
            }
            snippetCache.put(templateKey, s);
        }
        return r;
    }
    
    public String loadItemDoc(String itemName)
    throws IOException
    {
    	return loadContainerDoc(resourceName(itemName));    	
    }

    public abstract String loadContainerDoc(String docName)
    throws IOException;

    private String resourceName(String itemName)
    {
        if (extension == null || extension.length() < 1) {
            return itemName;
        }
        int hashPos = itemName.indexOf('#');
        if (hashPos < 0) {
            return itemName + '.' + extension;
        } else {
            String filename = itemName.substring(0,hashPos) + '.' + extension;
            return filename; // + itemName.substring(hashPos);
        }
    }

    public void clearCache()
    {
        snippetCache.clear();
    }

    public void clearCache(String itemName)
    {
        snippetCache.remove(itemName);
    }

}
