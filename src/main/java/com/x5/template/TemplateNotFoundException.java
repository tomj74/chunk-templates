package com.x5.template;

public class TemplateNotFoundException extends RuntimeException
{
    private String msg;
    private Throwable cause;

    public TemplateNotFoundException(String msg)
    {
        this.msg = msg;
    }

    public TemplateNotFoundException(String msg, Throwable cause)
    {
        this.msg = msg;
        this.cause = cause;
    }

    public String getMessage()
    {
        return msg;
    }

    public Throwable getRootCause()
    {
        return cause;
    }
}
