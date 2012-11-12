package com.x5.template.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.Chunk;

public class DefangFilter extends BasicFilter implements ChunkFilter
{
    public String transformText(Chunk chunk, String text, String[] args)
    {
        return text == null ? null : defang(text);
    }

    public String getFilterName()
    {
        return "defang";
    }
    
    public String[] getFilterAliases()
    {
        return new String[]{"noxss","neuter"};
    }

    private static final Pattern NOT_HARMLESS_CHAR = Pattern.compile("[^A-Za-z0-9@\\!\\?\\*\\#\\$\\(\\)\\+\\=\\:\\;\\,\\~\\/\\._-]");
    
    private static String defang(String text)
    {
        // keep only a very restrictive set of harmless
        // characters, eg when quoting back user input
        // in a server-generated page, to prevent xss
        // injection attacks.
        if (text == null) return null;
        Matcher m = NOT_HARMLESS_CHAR.matcher(text);
        return m.replaceAll("");
    }
    
}
