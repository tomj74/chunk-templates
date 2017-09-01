package com.x5.template;

public class InvalidExpressionException extends Exception
{
    private static final long serialVersionUID = -5546762672751850897L;

    public InvalidExpressionException(String msg)
    {
        super(msg);
    }
}
