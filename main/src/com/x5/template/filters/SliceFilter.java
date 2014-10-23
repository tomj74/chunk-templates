package com.x5.template.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.x5.template.Chunk;
import com.x5.template.Filter;

public class SliceFilter extends ListFilter
{
    public String getFilterName()
    {
        return "slice";
    }

    private static int parseSliceArg(String arg, int defaultVal, int len)
    {
        int x = defaultVal;
        if (arg != null && arg.trim().length() > 0) {
            try {
                x = Integer.parseInt(arg.trim());
                // flip negative value
                if (len >= 0 && x < 0) {
                    if (len + x < 0) {
                        x = 0;
                    } else {
                        x = len + x;
                    }
                }
            } catch (NumberFormatException e) {}
        }
        return x;
    }

    public Object transformList(Chunk chunk, List list, String[] args)
    {
        if (list == null) return list;

        int len = list.size();
        int from,to,step;
        String fromArg = null, endArg = null, stepArg = null;

        String firstArg = null;
        if (args.length == 1) {
            // no commas
            firstArg = args[0];
        } else if (args.length > 1) {
            // commas
            firstArg = args[1];
        }

        if (args.length > 0) {
            String[] sliceArgs = SplitFilter.splitNonRegex(firstArg,":");
            boolean colonDelim = sliceArgs.length > 1;
            fromArg = sliceArgs[0];
            if (colonDelim) {
                endArg = sliceArgs[1];
                if (sliceArgs.length > 2) {
                    stepArg = sliceArgs[2];
                }
            } else {
                if (args.length > 2) {
                    endArg = args[2];
                    if (args.length > 3) {
                        stepArg = args[3];
                    }
                }
            }
            step = parseSliceArg(stepArg,1,-1);
            from = parseSliceArg(fromArg, step < 0 ? len-1 : 0, len);
            to = parseSliceArg(endArg, step < 0 ? -1 : len, len);
            // prevent list index exceptions, infinite iterations
            if (from > len) { from = len; }
            if (step == 0) { step = 1; to = from; }
            if ((step > 0 && to < from) || (step < 0 && to > from)) { to = from; }
        } else {
            from = 0;
            to = len;
            step = 1;
        }

        if (step == 1) {
            return list.subList(from, to);
        } else {
            ArrayList<Object> stepped = new ArrayList<Object>();
            for (int i=from; step>0 ? i<to : i>to; i += step) {
                stepped.add(list.get(i));
            }
            return stepped;
        }
    }
}
