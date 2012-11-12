package com.x5.template;

import java.io.IOException;
import java.io.Writer;

public class SnippetTag extends SnippetPart
{
    protected String tag;
    
    public SnippetTag(String text, String tag)
    {
        super(text);
        this.tag = tag;
    }
    
    public boolean isTag()
    {
        return true;
    }
    
    public String getTag()
    {
        return this.tag;
    }

    public void render(Writer out, Chunk rules, int depth)
    throws IOException
    {
        if (depthCheckFails(depth,out)) return;

        Object tagValue = null;
        
        tagValue = rules.resolveTagValue(tag, depth);
        
        if (tagValue == null) {
            // preserve tag in final output (can be used as template)
            out.append(super.snippetText);
        } else if (tagValue instanceof Snippet) {
            // needs additional processing
            ((Snippet)tagValue).render(out, rules, depth);
        } else if (tagValue instanceof String) {
            Snippet compiled = Snippet.getSnippet((String)tagValue);
            compiled.render(out,rules,depth+1);
        } else {
            rules.explodeToPrinter(out, tagValue, depth+1);
        }
    }
    
    private static final BlockTag[] BLOCK_TAGS
        = new BlockTag[]{new LoopTag(),
                         new IfTag(),
                         new LocaleTag(),
                         new MacroTag()
                        };

    private static final String[] BLOCK_TAG_TOKENS = extractTagTokens(BLOCK_TAGS);
    
    private static String[] extractTagTokens(BlockTag[] blockTags)
    {
        String[] tokens = new String[blockTags.length];
        for (int i=0; i<blockTags.length; i++) {
            // This will miss old-style (deprecated?) {^loop(...)} invocation (do we care?)
            tokens[i] = "."+blockTags[i].getBlockStartMarker();
        }
        return tokens;
    }
    
    public BlockTag getBlockTagType()
    {
        for (int i=0; i<BLOCK_TAG_TOKENS.length; i++) {
            if (tag.startsWith(BLOCK_TAG_TOKENS[i])) {
                if (BLOCK_TAGS[i].hasBody(tag)) {
                    return BLOCK_TAGS[i];
                } else {
                    return null;
                }
            }
        }
        return null;
    }
    
}
