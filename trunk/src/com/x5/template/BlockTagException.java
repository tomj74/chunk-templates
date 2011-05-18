package com.x5.template;

@SuppressWarnings("serial")
public class BlockTagException extends Exception
{
    private String tagFunction;
    private BlockTagHelper helper;
    
    public BlockTagException(String tagFunction, BlockTagHelper helper)
    {
        this.tagFunction = tagFunction;
        this.helper = helper;
    }
    
    public String getTagFunction()
    {
        return tagFunction;
    }
    
    public BlockTagHelper getHelper()
    {
        return helper;
    }
}