package com.x5.template;

import java.io.IOException;
import java.io.Writer;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.x5.template.filters.FilterArgs;
import com.x5.template.filters.RegexFilter;

public class LocaleTag extends BlockTag
{
    private Chunk context;
    private String[] args;
    private String body = null;

    public static String LOCALE_TAG_OPEN = "{_[";
    public static String LOCALE_TAG_CLOSE = "}";
    public static String LOCALE_SIMPLE_OPEN = "_[";
    public static String LOCALE_SIMPLE_CLOSE = "]";

    public LocaleTag(String params, Chunk context)
    {
        this.context = context;
        parseParams(params);
    }

    public LocaleTag()
    {
    }

    public LocaleTag(String params, Snippet body)
    {
        this.body = body.toString();
    }

    private void parseParams(String params)
    {
        if (params == null) return;
        int spacePos = params.indexOf(" ");
        if (spacePos < 0) return;

        String cleanParams = params.substring(spacePos+1).trim();
        if (cleanParams.startsWith(",")) {
            cleanParams = cleanParams.substring(1).trim();
        }
        if (cleanParams.length() == 0) return;

        // look for comma delimeters not preceded by a backslash
        this.args = cleanParams.split(" *(?<!\\\\), *");
    }

    private String _translate()
    {
        ChunkLocale locale = context.getLocale();
        if (locale == null) {
            return ChunkLocale.processFormatString(body,args,context);
        } else {
            return locale.translate(body,args,context);
        }
    }

    @Override
    public String getBlockStartMarker()
    {
        return "loc";
    }

    @Override
    public String getBlockEndMarker()
    {
        return "/loc";
    }

    private static String convertToChunkTag(String ezSyntax, Chunk ctx)
    {
        if (ezSyntax.startsWith(LOCALE_SIMPLE_OPEN)) {
            // simple case, no args
            int bodyA = 2;
            int bodyB = ezSyntax.length();
            if (ezSyntax.endsWith(LOCALE_SIMPLE_CLOSE)) bodyB--;
            String body = ezSyntax.substring(bodyA,bodyB);

            String blockStart = ctx.makeTag(".loc");
            String blockEnd = ctx.makeTag("./loc");

            return blockStart + body + blockEnd;
        }

        if (ezSyntax.startsWith(LOCALE_TAG_OPEN)) {
            int bodyA = 3;
            int bodyB = ezSyntax.lastIndexOf(LOCALE_SIMPLE_CLOSE);
            if (bodyB < 0) {
                return convertToChunkTag(ezSyntax.substring(1),ctx);
            }
            String body = ezSyntax.substring(bodyA,bodyB);

            int argsA = bodyB+1;
            int argsB = ezSyntax.length();
            if (ezSyntax.endsWith(LOCALE_TAG_CLOSE)) argsB--;

            String params = ezSyntax.substring(argsA,argsB);

            String blockStart = ctx.makeTag(".loc " + params);
            String blockEnd = ctx.makeTag("./loc");

            return blockStart + body + blockEnd;
        }

        // not properly formatted, pass through.
        return ezSyntax;
    }

    public static String expandLocaleTags(String template, Chunk ctx)
    {
        int[] markers = scanForMarkers(template);
        if (markers == null) return template;

        StringBuilder buf = new StringBuilder();
        int cursor = 0;
        for (int i=0; i<markers.length; i+=3) {
            int a = markers[i];
            int b = markers[i+1];
            int c = markers[i+2];
            buf.append(template.substring(cursor,a));
            String localeTag = template.substring(a,b);
            buf.append(convertToChunkTag(localeTag,ctx));
            cursor = c;
        }
        if (cursor < template.length()) {
            buf.append(template.substring(cursor));
        }
        return buf.toString();
    }

    private static final Pattern OPEN_TAG_PATTERN
        = Pattern.compile(RegexFilter.escapeRegex(LOCALE_TAG_OPEN) + "|" + RegexFilter.escapeRegex(LOCALE_SIMPLE_OPEN));

    private static int[] scanForMarkers(String template)
    {
        if (template.indexOf(LOCALE_SIMPLE_OPEN) < 0) {
            return null;
        }

        boolean isSimple = true;

        String markers = "";
        int len = template.length();

        Matcher m = OPEN_TAG_PATTERN.matcher(template);

        int tagPos = m.find() ? m.start() : -1;

        while (tagPos > -1) {
            // is simple _[...] or not simple {_[...%s...],~x}
            String whatMatched = m.group();
            isSimple = whatMatched.equals(LOCALE_SIMPLE_OPEN) ? true : false;

            int tagEndInside = nextUnescapedDelim(isSimple,template,tagPos);
            int tagEndOutside = tagEndInside + (isSimple ? LOCALE_SIMPLE_CLOSE.length() : LOCALE_TAG_CLOSE.length());
            markers += tagPos + "," + tagEndInside + "," + tagEndOutside + ",";

            if (tagEndOutside >= len) break;

            tagPos = m.find(tagEndOutside) ? m.start() : -1;
        }

        return makeIntArray(markers);
    }

    private static int nextUnescapedDelim(boolean isSimple, String template, int tagPos)
    {
        if (isSimple) {
            // scan for matching non-escaped ]
            return FilterArgs.nextUnescapedDelim(LOCALE_SIMPLE_CLOSE, template, tagPos + LOCALE_SIMPLE_OPEN.length());
        } else {
            // scan for matching non-escaped }
            return FilterArgs.nextUnescapedDelim(LOCALE_TAG_CLOSE, template, tagPos + LOCALE_TAG_OPEN.length());
        }
    }

    private static int[] makeIntArray(String markersStr)
    {
        StringTokenizer tokens = new StringTokenizer(markersStr,",");
        int[] markers = new int[tokens.countTokens()];
        for (int i=0; i<markers.length; i++) {
            markers[i] = Integer.parseInt(tokens.nextToken());
        }
        return markers;
    }

    @Override
    public void renderBlock(Writer out, Chunk context, String origin, int depth)
        throws IOException
    {
        if (body == null) return;

        this.context = context;

        String translated = _translate();
        out.append( translated );
    }
}
