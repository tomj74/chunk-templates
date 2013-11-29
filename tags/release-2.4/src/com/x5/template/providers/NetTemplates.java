package com.x5.template.providers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class NetTemplates extends TemplateProvider
{
    private String baseURL = null;

    public NetTemplates(String baseURL)
    {
        this.baseURL = baseURL;
    }
    
    public String loadContainerDoc(String docName)
    throws IOException
    {
        return getUrlContents(baseURL + docName);
    }

    public String getProtocol()
    {
        return "net";
    }

    private static String getUrlContents(String theUrl)
    throws IOException
    {
        URL url = new URL(theUrl);
        URLConnection urlConnection = url.openConnection();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

        StringBuilder content = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            content.append(line + "\n");
        }
        bufferedReader.close();
        return content.toString();
    }
}
