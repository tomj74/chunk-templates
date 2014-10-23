package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.ContentSource;
import com.x5.template.Snippet;

public class ExecFilter extends BasicFilter
{
    public String transformText(Chunk chunk, String text, String[] args)
    {
        String templateName;
        if (args != null && args.length > 0) {
            templateName = args[0];
        } else {
            return null;
        }

        if (chunk == null) return null;
        ContentSource theme = chunk.getTemplateSet();
        if (theme == null) return null;
        Snippet filterBody = theme.getSnippet(templateName);
        if (filterBody == null) {
            return null;
        }

        // exec self-contained chunk with own private context
        Chunk miniMacro = new Chunk();
        miniMacro.append(filterBody);
        miniMacro.setOrDelete("x",text);
        return miniMacro.toString();
    }

    public String getFilterName()
    {
        return "filter";
    }

}