package com.x5.template;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Attributes
{
    private static final Pattern PARAM_AND_VALUE =
        Pattern.compile(" ([a-zA-Z0-9_-]+)=(\"([^\"]*)\"|'([^\']*)'|([^ \"\']+))");

    // find and save all xyz="abc" style attributes
    // alternate styles xyz='abc' and even unquoted xyz=abc are also allowed
    public static Map<String,Object> parse(String params)
    {
        Matcher m = PARAM_AND_VALUE.matcher(params);
        HashMap<String,Object> opts = null;
        while (m.find()) {
            m.group(0); // need to do this for subsequent number to be correct?
            String paramName = m.group(1);
            if (paramName != null) {
                if (opts == null) opts = new HashMap<String,Object>();
                String doubleQuoted = m.group(3);
                String singleQuoted = m.group(4);
                String unQuoted = m.group(5);
                String paramValue = doubleQuoted;
                if (paramValue == null) paramValue = singleQuoted;
                if (paramValue == null) paramValue = unQuoted;
                opts.put(paramName, paramValue);
            }
        }
        return opts;
    }


}
