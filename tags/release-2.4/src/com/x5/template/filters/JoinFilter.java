package com.x5.template.filters;

import java.util.List;

import com.x5.template.Chunk;
import com.x5.template.Filter;

public class JoinFilter extends ListFilter
{
    public String getFilterName()
    {
        return "join";
    }

    public Object transformList(Chunk chunk, List list, String[] args)
    {
        if (list == null) return "";
        if (list.size() == 1) return list.get(0);

        // the only arg is the divider
        String divider = args.length > 0 && !args[0].equals("join") ? args[0] : null;

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
