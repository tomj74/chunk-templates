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
    protected String token;
    private String[] args;

    public SnippetToken(String text, String token)
    {
        super(text);
        this.token = token;
    }

    public void render(Writer out, Chunk context, int depth)
    throws java.io.IOException
    {
        ChunkLocale locale = context.getLocale();

        String translated = null;

        if (locale == null) {
            if (args == null) {
                out.append(token);
                return;
            } else {
                translated = ChunkLocale.processFormatString(token, args, context);
            }
        } else {
            translated = locale.translate(token, args, context);
        }

        Snippet reprocess = Snippet.getSnippet(translated);
        reprocess.render(out, context, depth);
    }

    public static SnippetToken parseTokenWithArgs(String wholeTag)
    {
        int bodyA = 3;
        int bodyB = wholeTag.lastIndexOf(LocaleTag.LOCALE_SIMPLE_CLOSE);
        if (bodyB < 0) {
            // no end bracket, no args
            return new SnippetToken(wholeTag,wholeTag.substring(bodyA));
        }

        int argsA = bodyB+1;
        int argsB = wholeTag.length();
        if (wholeTag.endsWith(LocaleTag.LOCALE_TAG_CLOSE)) argsB--;

        // remove trailing whitespace if this is a {% whitespace-friendly %} tag
        if (wholeTag.startsWith("{%")) {
            bodyA++;
            while (Character.isWhitespace(wholeTag.charAt(bodyA-2))) bodyA++;
            if (wholeTag.charAt(argsB-1) == '%') argsB--;
            while (Character.isWhitespace(wholeTag.charAt(argsB-1))) argsB--;
        }

        // skip initial comma.
        if (wholeTag.charAt(argsA) == ',') argsA++;
        String params = wholeTag.substring(argsA,argsB);

        String body = wholeTag.substring(bodyA,bodyB);
        SnippetToken tokenWithArgs = new SnippetToken(wholeTag,body);

        if (params != null && params.trim().length() > 0) {
            String[] args = params.split(" *, *");
            tokenWithArgs.args = args;
        }

        return tokenWithArgs;
    }

}
