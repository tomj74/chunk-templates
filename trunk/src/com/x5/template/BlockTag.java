package com.x5.template;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public abstract class BlockTag
{
    public abstract void renderBlock(Writer out, Chunk context, int depth)
        throws IOException;
    public abstract String getBlockStartMarker();
    public abstract String getBlockEndMarker();
    
    private static int locateTag(List<SnippetPart> parts, String tagToMatch, int startAt)
    {
        for (int i=startAt; i<parts.size(); i++) {
            SnippetPart part = parts.get(i);
            if (part.isTag()) {
                SnippetTag tag = (SnippetTag)part;
                String tagText = tag.getTag();
                if (tagText != null && tagText.startsWith(tagToMatch)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int findMatchingBlockEnd(BlockTag helper, List<SnippetPart> parts, int startAt)
    {
        String endBlock = helper.getBlockEndMarker();
        String scanFor = "." + endBlock;

        String beginBlock = helper.getBlockStartMarker();
        String nestedScanFor = "." + beginBlock;
        
        int nestDepth = 0;
        int nestedBlockPos = locateTag(parts,nestedScanFor,startAt);
        int endMarkerPos = locateTag(parts,scanFor,startAt);
        
        if (nestedBlockPos > -1 && nestedBlockPos < endMarkerPos) {
            nestDepth++;
        }
        
        while (nestDepth > 0 && endMarkerPos > 0) {
            while (nestedBlockPos > -1 && nestedBlockPos < endMarkerPos) {
                // check for another nested block starting before this end-candidate
                nestedBlockPos = locateTag(parts,nestedScanFor,nestedBlockPos+1);
                if (nestedBlockPos > -1 && nestedBlockPos < endMarkerPos) {
                    nestDepth++;
                }
            }
            // got past end of nested block.
            nestDepth--;
            // locate new candidate for block end.
            endMarkerPos = locateTag(parts, scanFor, endMarkerPos+1);
            // check for problems with candidate
            if (nestedBlockPos > -1 && nestedBlockPos < endMarkerPos) {
                nestDepth++;
            }
        }
        
        return endMarkerPos;
    }
    
    public boolean hasBody(String openingTag)
    {
        return true;
    }

}