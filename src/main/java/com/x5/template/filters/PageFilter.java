package com.x5.template.filters;

import java.util.List;

import com.x5.template.Chunk;

public class PageFilter extends ListFilter
{
    public String getFilterName()
    {
        return "page";
    }

    @SuppressWarnings("rawtypes")
    public Object transformList(Chunk chunk, List list, FilterArgs arg)
    {
        if (list == null) return list;

        int page = 1;
        int pageSize = 10;
        int from, to;

        String[] args = arg.getFilterArgs();

        if (args.length > 0) {
            page = resolveArg(chunk, args[0], page);
            if (args.length > 1) {
                pageSize = resolveArg(chunk, args[1], pageSize);
            }
        }

        from = (page-1) * pageSize;
        to = page * pageSize;

        from = Math.max(from, 0);
        to = Math.min(to, list.size());

        if (from > to) {
            from = 0;
            to = 0;
        }

        return SliceFilter.slice(list, from, to, 1);
    }

    private int resolveArg(Chunk context, String arg, int fallback)
    {
        int n = fallback;
        if (arg.charAt(0) == '$' || arg.charAt(0) == '~') {
            Object obj = context.get(arg.substring(1));
            arg = (obj instanceof String) ? (String)obj : obj.toString();
        }
        try {
            n = Integer.parseInt(arg.trim());
        } catch (NumberFormatException e) {}

        return n;
    }
}
