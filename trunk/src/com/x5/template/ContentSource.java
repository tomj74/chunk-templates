package com.x5.template;

public interface ContentSource
{
    public String fetch(String itemName);
    public boolean provides(String itemName);
    public String getProtocol();
}
