package com.x5.template.providers;

import java.io.InputStream;
import android.content.Context;

public class AndroidTemplates extends TemplateProvider
{
    private Context context = null;
    private String themeFolder = "themes";
    
    /**
     * Place templates in assets/themes/*.chtml and this
     * template loader will be able to find them.
     * 
     * Provide a context to the constructor, eg:
     * 
     * AndroidTemplates loader = new AndroidTemplates(getBaseContext());
     * Theme = new Theme(loader);
     * 
     * @param androidContext
     */
    public AndroidTemplates(Context androidContext)
    {
        this.context = androidContext;
    }
    
    /**
     * Place templates in assets/themes/[themeFolder]/*.chtml and this
     * template loader will be able to find them.
     * 
     * Provide a context to the constructor, eg:
     * 
     * AndroidTemplates loader = new AndroidTemplates(getBaseContext(),"webview");
     * Theme = new Theme(loader);
     * 
     * @param androidContext
     * @param themeFolder
     */
    public AndroidTemplates(Context androidContext, String themeFolder)
    {
        this.context = androidContext;
        this.themeFolder = themeFolder;
    }
    
    public String getProtocol()
    {
        return "android";
    }

    public String loadContainerDoc(String docName)
    throws java.io.IOException
    {
        String path = themeFolder + "/" + docName;
        InputStream in = context.getAssets().open(path);
        java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
