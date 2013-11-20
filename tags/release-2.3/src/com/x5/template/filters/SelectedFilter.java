package com.x5.template.filters;

import com.x5.template.Chunk;

public class SelectedFilter extends BasicFilter implements ChunkFilter
{

    public String transformText(Chunk chunk, String text, String[] args)
    {
        // return null or empty-string?  I think null.
        return text == null ? null : selected(chunk, text, args);
    }

    public String getFilterName()
    {
        return "selected";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"select","sel"};
    }

    private static final String SELECTED_TOKEN = " selected=\"selected\" ";
    private static final String CHECKED_TOKEN = " checked=\"checked\" ";

    private static String selected(Chunk context, String text, String[] args)
    {
        return selected(context, text, args, SELECTED_TOKEN);
    }

    protected static String checked(Chunk context, String text, String[] args)
    {
        return selected(context, text, args, CHECKED_TOKEN);
    }

    private static String selected(Chunk context, String text, String[] args, String token)
    {
        // no arg?  so, just return token if text is non-null
        if (args == null) return token;

        String testValue = args[0];
        if (args.length > 1) testValue = args[1];
        if (args.length > 2) token = args[2];

        if (testValue.charAt(0) == '~' || testValue.charAt(0) == '$') {

            Object value = context.get(testValue.substring(1));
            if (value != null && text.equals(value.toString())) {
                return token;
            } else {
                return "";
            }

        }

        // simple case, compare to static text string
        if (text.equals(testValue)) {
            return token;
        } else {
            return "";
        }
    }
}
