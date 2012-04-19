package com.x5.template.filters;

import com.x5.template.Chunk;
import com.x5.template.LocaleTag;

public class TranslateFilter extends BasicFilter implements ChunkFilter
{

    @Override
    public String transformText(Chunk chunk, String text, String[] args)
    {
        return markForTranslation(text);
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
