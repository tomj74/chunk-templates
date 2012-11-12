package com.x5.template;

import java.io.IOException;
import java.io.Writer;

public class FilteredPrinter extends Writer
{
    private Writer finalOutput;
    private Chunk context;
    private String filter;
    
    private StringBuilder copout = new StringBuilder();
    
    public FilteredPrinter(Writer out, Chunk context, String filter)
    {
        super(out);
        this.finalOutput = out;
        this.context = context;
        this.filter = filter;
    }
    
    public void write(char[] cbuf, int off, int len)
    {
        copout.append(cbuf, off, len);
    }
    
    public void flush()
    throws IOException
    {
        // until TextFilter is refactored to handle streams...
        String output = TextFilter.applyTextFilter(context, filter, copout.toString());
        copout.setLength(0);
        // process one more time (!) since filtered output might contain new tags
        if (output != null) {
            context.explodeToPrinter(finalOutput, output, 1);
        }
        finalOutput.flush();
    }

    @Override
    public void close() throws IOException
    {
        // TODO Auto-generated method stub
        
    }
}
