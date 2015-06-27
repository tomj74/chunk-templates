package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.ChunkLocale;

public class TranslateFilter extends BasicFilter implements ChunkFilter
{

    @Override
    public String transformText(Chunk context, String text, FilterArgs args)
    {
        if (text == null) return null;
        if (context == null) return text;
        ChunkLocale translator = context.getLocale();
        if (translator == null) return text;

        // should check for args and pass args[1:] to translator
        // in case of "asdf %s asdf" format string?
        // otherwise, not really necessary to pass context.
        return translator.translate(text, null, context);
    }

    @Override
    public String getFilterName()
    {
        return "_";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"__","translate","xlate"};
    }

}
