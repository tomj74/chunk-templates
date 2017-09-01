package com.x5.template.filters;

import java.util.Map;

import com.x5.template.Chunk;
import com.x5.util.ObjectDataMap;

public abstract class ObjectFilter implements ChunkFilter
{
    public Object applyFilter(Chunk chunk, String text, FilterArgs args)
    {
        return text;
    }

    public abstract String getFilterName();
    public String[] getFilterAliases()
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object applyFilter(Chunk chunk, Object object, FilterArgs args)
    {
        if (object instanceof ObjectDataMap) {
            // expose inner object for direct manipulation
            object = ((ObjectDataMap)object).unwrap();
        }

        Object transformed = transformObject(chunk, object, args);

        String[] deepRefPath = args.getDeepRefPath();
        if (deepRefPath == null || transformed == null) {
            return transformed;
        }

        return resolveDeepRefs(transformed, deepRefPath);
    }

    @SuppressWarnings("rawtypes")
    public abstract Object transformObject(Chunk chunk, Object object, FilterArgs args);

    public ObjectFilter() {}

    @SuppressWarnings("rawtypes")
    static Object resolveDeepRefs(Object o, String[] path)
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
