package com.x5.template;

import java.io.Writer;

/**
 * SnippetToken is a string of text that was marked with _[...] for running
 * through the translation engine.
 * 
 * @author tmcclure
 *
 */
public class SnippetToken extends SnippetPart
{
    private String[] args;
    
    public SnippetToken(String text)
    {
        super(text);
    }
    
    public void render(Writer out, Chunk context, int depth)
    throws java.io.IOException
    {
        ChunkLocale locale = context.getLocale();

        String translated = null;
        
        if (locale == null) {
            if (args == null) {
                out.append(super.snippetText);
                return;
            } else {
                translated = ChunkLocale.processFormatString(super.snippetText, args, context);
            }
        } else {
            translated = locale.translate(super.snippetText, args, context);
        }
        
        Snippet reprocess = new Snippet(translated);
        reprocess.render(out, context, depth);
    }

    public static SnippetToken parseTokenWithArgs(String wholeTag)
    {
        int bodyA = 3;
        int bodyB = wholeTag.lastIndexOf(LocaleTag.LOCALE_SIMPLE_CLOSE);
        if (bodyB < 0) {
            // no end bracket, no args
            return new SnippetToken(wholeTag.substring(bodyA));
        }
        String body = wholeTag.substring(bodyA,bodyB);
        SnippetToken tokenWithArgs = new SnippetToken(body);
        
        int argsA = bodyB+1;
        int argsB = wholeTag.length();
        if (wholeTag.endsWith(LocaleTag.LOCALE_TAG_CLOSE)) argsB--;
        
        // skip initial comma.
        if (wholeTag.charAt(argsA) == ',') argsA++;
        
        String params = wholeTag.substring(argsA,argsB);
        if (params != null && !params.trim().isEmpty()) {
            String[] args = params.split(" *, *");
            tokenWithArgs.args = args;
        }
        
        return tokenWithArgs;
    }
}
