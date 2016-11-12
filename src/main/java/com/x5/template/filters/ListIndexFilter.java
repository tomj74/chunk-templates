package com.x5.template.filters;

import java.util.List;
import java.util.Map;

import com.x5.template.Chunk;

public class ListIndexFilter extends ListFilter
{
    public String getFilterName()
    {
        return "get";
    }

    @SuppressWarnings("rawtypes")
    public Object transformList(Chunk chunk, List list, FilterArgs arg)
    {
        if (list == null) return null;

        String[] args = arg.getFilterArgs();
        if (args.length < 1) return null;

        int i = Integer.parseInt(args[0]);
        if (i < 0) {
            // permit negative indices
            i = list.size() + i;
        }
        if (i < 0 || i >= list.size()) {
            return null;
        } else {
            return resolveDeepRefs(list.get(i), arg.getDeepRefPath());
        }
    }

    @SuppressWarnings("rawtypes")
    private Object resolveDeepRefs(Object o, String[] path)
    {
        if (path == null) return o;

        Object deepVal = o;
        int segment = 0;

        while (path.length > segment && deepVal != null) {
            deepVal = Chunk.boxIfAlienObject(deepVal);
            if (deepVal instanceof Map) {
                String segmentName = path[segment];
                Map obj = (Map)deepVal;
                deepVal = obj.get(segmentName);
                segment++;
            } else {
                deepVal = null;
            }
        }

        return deepVal == null ? o : deepVal;
    }

}
