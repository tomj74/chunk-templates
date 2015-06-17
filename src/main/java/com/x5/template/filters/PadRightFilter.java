package com.x5.template.filters;

import com.x5.template.Chunk;

public class PadRightFilter extends BasicFilter implements ChunkFilter
{
    public String transformText(Chunk chunk, String text, String[] args)
    {
        return text == null ? null : padText(text, cleanArgs(args));
    }
    
    private String[] cleanArgs(String[] args)
    {
    	if (args == null || args.length != 1) return args;
    	
    	String filterCall = args[0];
    	if (filterCall == null || filterCall.equals(getFilterName())) {
    		return null;
    	}
    	for (String alias : getFilterAliases()) {
    		if (filterCall.equals(alias)) return null;
    	}
    	
    	return args;
    }

    public String getFilterName()
    {
        return "rpad";
    }
    
    public String[] getFilterAliases()
    {
        return new String[]{"pad","suffix"};
    }

    protected String padText(String text, String[] args)
    {
    	if (text.length() == 0) return text;
    	
    	if (args == null || args.length == 0 && (args[0].equals("rpad"))) {
    		return text + " ";
    	}
    	
    	String suffix = args[0];
    	int howmany = 1;
    	if (args.length > 1) {
    		suffix = args[1];
    	}
    	if (args.length > 2) {
    		try {
    			howmany = Integer.parseInt(args[2]);
    		} catch (NumberFormatException e) {}
    	}
    	if (howmany == 1) return text + suffix;
    	
    	StringBuilder sb = new StringBuilder(text);
    	for (int i=0; i<howmany; i++) {
    		sb.append(suffix);
    	}
    	return sb.toString();
    }
}
