package com.x5.template.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.x5.template.Chunk;

public abstract class ListFilter implements ChunkFilter
{
    public Object applyFilter(Chunk chunk, String text, FilterArgs args)
    {
        // transform text input into a char array
        Object o = applyFilter(chunk, text == null ? (Object)null : listifyString(text), args);
        if (o instanceof List) {
            return JoinFilter.join((List)o, null);
        } else {
            return o;
        }
    }

    private List<Character> listifyString(String text)
    {
        List<Character> characters = new ArrayList<Character>();
        char[] chars = text.toCharArray();
        for (int i=0; i<chars.length; i++) {
            characters.add(new Character(chars[i]));
        }

        return characters;
    }

    public abstract String getFilterName();
    public String[] getFilterAliases()
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object applyFilter(Chunk chunk, Object object, FilterArgs args)
    {
        List<Object> list = null;
        if (object instanceof List) {
            // preserve List
            list = (List<Object>)object;
        } else if (object instanceof Object[]) {
            // turn arrays into List
            list = Arrays.asList((Object[])object);
        } else if (object instanceof String) {
            return applyFilter(chunk, (String)object, args);
        } else {
            // turn single objects into List of length one
            list = Arrays.asList(object);
        }
        return transformList(chunk, list, args);
    }

    @SuppressWarnings("rawtypes")
    public abstract Object transformList(Chunk chunk, List list, FilterArgs args);

    public ListFilter() {}

}
