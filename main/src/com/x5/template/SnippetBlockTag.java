package com.x5.template;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class SnippetBlockTag extends SnippetTag
{
    private SnippetTag tagOpen;
    private Snippet body;
    private SnippetTag tagClose;

    private BlockTag renderer;

    public SnippetBlockTag(SnippetTag tagOpen, List<SnippetPart> bodyParts, SnippetTag tagClose)
    {
        super(tagOpen.snippetText, tagOpen.tag);
        this.tagOpen = tagOpen;
        this.tagClose = tagClose;
        this.body = new Snippet(bodyParts);

        initBlockTag();
    }

    private void initBlockTag()
    {
        String tagName = tagOpen.tag;
        if (tagName.startsWith(".loop")) {
            renderer = new LoopTag(tagName,body);
        } else if (tagName.startsWith(".if")) {
            renderer = new IfTag(tagName,body);
        } else if (tagName.startsWith(".loc")) {
            renderer = new LocaleTag(tagName,body);
        } else if (tagName.startsWith("."+MacroTag.MACRO_MARKER)) {
            renderer = new MacroTag(tagName,body);
        }
    }

    public void render(Writer out, Chunk context, int depth)
    throws IOException
    {
        if (depthCheckFails(depth,out)) return;

        if (renderer == null) return;
        renderer.renderBlock(out,context,depth);
    }

    public String toString()
    {
        return snippetText + body.toString() + tagClose.toString();
    }

    public SnippetTag getOpenTag()
    {
        return tagOpen;
    }

    public Snippet getBody()
    {
        return body;
    }

    public SnippetTag getCloseTag()
    {
        return tagClose;
    }

    public boolean doSmartTrimAroundBlock()
    {
        if (renderer != null && renderer.doSmartTrimAroundBlock()) {
            return true;
        } else {
            return false;
        }
    }
}
