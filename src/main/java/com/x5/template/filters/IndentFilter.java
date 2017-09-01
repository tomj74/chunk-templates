package com.x5.template.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.Chunk;

public class IndentFilter extends BasicFilter implements ChunkFilter
{

    @Override
    public String transformText(Chunk chunk, String text, FilterArgs args)
    {
        return text == null ? null : applyIndent(text, args, chunk);
    }

    public String getFilterName()
    {
        return "indent";
    }

    private static final Pattern EOL = Pattern.compile("(\\r\\n|\\r\\r|\\n)");

    public static String applyIndent(String text, FilterArgs arg, Chunk chunk)
    {
        String[] args = arg.getFilterArgs(chunk);
        if (args == null) return text;

        String indent = args[0];
        String padChip = " ";

        if (args.length > 1) {
            padChip = args[1];
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
