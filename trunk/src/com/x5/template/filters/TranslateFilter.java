package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.ChunkLocale;
import com.x5.template.LocaleTag;

public class TranslateFilter extends BasicFilter implements ChunkFilter
{

    @Override
    public String transformText(Chunk context, String text, String[] args)
    {
        if (text == null) return null;
        if (context == null) return text;
        ChunkLocale translator = context.getLocale();
        if (translator == null) return text;
        
        // should check for args and pass args[1:] to translator
        // in case of "asdf %s asdf" format string?
        // otherwise, not really necessary to pass context.
        return translator.translate(text, null, context);
        //return markForTranslation(text);
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

    private static String markForTranslation(String text)
    {
        // TODO check for args and encase in {_[...],~arg1,~arg2} style markup
        if (text == null) return null;
        text = Chunk.findAndReplace(text,"[","\\[");
        text = Chunk.findAndReplace(text,"]","\\]");
        return LocaleTag.LOCALE_SIMPLE_OPEN + text + LocaleTag.LOCALE_SIMPLE_CLOSE;
    }
    
}
