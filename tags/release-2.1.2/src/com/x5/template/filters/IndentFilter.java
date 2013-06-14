package com.x5.template.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.Chunk;

public class IndentFilter extends BasicFilter implements ChunkFilter
{

    @Override
    public String transformText(Chunk chunk, String text, String[] args)
    {
        return text == null ? null : applyIndent(text, args);
    }

    public String getFilterName()
    {
        return "indent";
    }

    private static final Pattern EOL = Pattern.compile("(\\r\\n|\\r\\r|\\n)");

    public static String applyIndent(String text, String[] args)
    {
        if (args == null) return text;
        
        String indent = args.length > 1 ? args[1] : args[0];
        String padChip = " ";
        
        if (args.length > 2) {
            padChip = args[2];
        }
        
        try {
            int pad = Integer.parseInt(indent);
            int textLen = text.length();
            
            String linePrefix = padChip;
            for (int i=1; i<pad; i++) linePrefix += padChip;
            
            StringBuilder indented = new StringBuilder();
            indented.append(linePrefix);

            Matcher m = EOL.matcher(text);

            int marker = 0;
            while (m.find()) {
                String line = text.substring(marker,m.end());
                indented.append(line);
                marker = m.end();
                if (marker < textLen) indented.append(linePrefix);
            }
            
            if (marker < textLen) {
                indented.append(text.substring(marker));
            }
            
            return indented.toString();
        } catch (NumberFormatException e) {
            return text;
        }
    }
    
}
