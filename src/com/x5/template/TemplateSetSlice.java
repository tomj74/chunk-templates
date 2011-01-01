package com.x5.template;

// Project Title: Chunk
// Description: Template Util
// Copyright: Copyright (c) 2003
// Author: Tom McClure

/**
 * TemplateSetSlice enables the formation of a TemplateSet from a single file<BR>
 * with multiple subtemplate definitions a la {#optional_part}bla bla bla{#}.
 *
 * <P>
 * It maps the concept of fetching a template from within a folder of templates<BR>
 * to fetching a subtemplate from within a file of subtemplates.
 *
 * <P>
 * Copyright: Copyright (c) 2003<BR>
 * Company: <A href="http://www.x5software.com/">X5 Software</A><BR>
 * Updates: <A href="http://www.dagblastit.com/">www.dagblastit.com</A><BR>
 *
 * @author Tom McClure
 * @version 2.0
 */

public class TemplateSetSlice extends TemplateSet
{
    private String context;
    private String extension = null;
    private TemplateSet parent;

    public TemplateSetSlice(TemplateSet parent, String templateContext)
    {
        this.parent = parent;
        this.context = templateContext;
    }

    public TemplateSetSlice(TemplateSet parent, String templateContext, String ext)
    {
        this.parent = parent;
        this.context = templateContext;
        this.extension = ext;
    }

    public String getSnippet(String templateName)
    {
        String fullTemplateName = putInContext(templateName);
        if (extension == null) {
            return parent.getSnippet(fullTemplateName);
        } else {
            return parent.getSnippet(fullTemplateName, extension);
        }
    }

    private String putInContext(String templateName)
    {
        if (templateName == null) return null;
        if (templateName.startsWith("#")) {
            return context + "." + templateName.substring(1);
        } else {
            return context + "." + templateName;
        }
    }

    public Chunk makeChunk()
    {
        return parent.makeChunk();
    }

    public Chunk makeChunk(String templateName)
    {
        if (extension == null) {
            return parent.makeChunk(putInContext(templateName));
        } else {
            return parent.makeChunk(putInContext(templateName),extension);
        }
    }

}
