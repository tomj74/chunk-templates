package com.x5.template.filters;

public class PadLeftFilter extends PadRightFilter implements ChunkFilter
{
    public String getFilterName()
    {
        return "lpad";
    }

    public String[] getFilterAliases()
    {
        return new String[]{"prefix"};
    }

    protected String padText(String text, String[] args)
    {
    	if (text.length() == 0) return text;

    	if (args == null) {
    		return " " + text;
    	}

    	String prefix = args[0];
    	int howmany = 1;
    	if (args.length > 1) {
    		try {
    			howmany = Integer.parseInt(args[1]);
    		} catch (NumberFormatException e) {}
    	}
    	if (howmany == 1) return prefix + text;

    	StringBuilder sb = new StringBuilder();
    	for (int i=0; i<howmany; i++) {
    		sb.append(prefix);
    	}
    	sb.append(text);
    	return sb.toString();
    }
}
