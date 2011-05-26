package com.x5.template;

@SuppressWarnings("serial")
public class BlockTagException extends Exception
{
    private BlockTag helper;
    
    public BlockTagException(BlockTag helper)
    {
        this.helper = helper;
    }
    
    public BlockTag getHelper()
    {
        return helper;
    }
}