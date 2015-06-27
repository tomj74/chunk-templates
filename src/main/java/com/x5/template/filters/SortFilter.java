package com.x5.template.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.x5.template.Chunk;

public class SortFilter extends ListFilter
{
    public String getFilterName()
    {
        return "sort";
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object transformList(Chunk chunk, List list, FilterArgs args)
    {
        if (list == null || list.size() < 2) return list;
        try {
            if (isInOrder(list)) return list;
        } catch (ClassCastException e) {
            return list;
        } catch (NullPointerException e) {
            return list;
        }

        // sort a copy of the list.
        List sorted = new ArrayList(list);
        Collections.sort(sorted);
        return sorted;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean isInOrder(List list)
    throws ClassCastException
    {
        boolean inOrder = true;
        for (int i=1; i<list.size(); i++) {
            Comparable a = (Comparable)list.get(i-1);
            Comparable b = (Comparable)list.get(i);
            if (a.compareTo(b) > 0) inOrder = false;
        }
        return inOrder;
    }

}
