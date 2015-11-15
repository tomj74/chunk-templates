package com.x5.template.filters;

import java.util.List;

import com.x5.template.Chunk;

public class JoinFilter extends ListFilter
{
    public String getFilterName()
    {
        return "join";
    }

    @SuppressWarnings("rawtypes")
    public Object transformList(Chunk chunk, List list, FilterArgs args)
    {
        // the only arg is the divider
        String divider = args.getUnparsedArgs();

        return join(list, divider);
    }

    public static Object join(List list, String divider)
    {
        if (list == null) return "";
        if (list.size() == 1) return list.get(0);

        StringBuilder x = new StringBuilder();
        int i = 0;
        for (Object s : list) {
            if (i>0 && divider != null) x.append(divider);
            if (s != null) x.append(s.toString());
            i++;
        }
        return x.toString();
    }

}
