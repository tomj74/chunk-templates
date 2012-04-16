package com.x5.template;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public abstract class BlockTag
{
    public abstract String cookBlock(String blockBody);
    public abstract void renderBlock(Writer out, Chunk context, int depth)
        throws IOException;
    public abstract String getBlockStartMarker();
    public abstract String getBlockEndMarker();
    
    public static int[] findMatchingBlockEnd(Chunk context, String template, int blockStartPos, BlockTag helper)
    {
        String endBlock = helper.getBlockEndMarker();
        String scanFor = context.tagStart + "." + endBlock + context.tagEnd;

        String beginBlock = helper.getBlockStartMarker();
        String nestedScanFor = context.tagStart + "." + beginBlock;
        
        int nestDepth = 0;
        int nestedBlockPos = template.indexOf(nestedScanFor, blockStartPos);
        int endMarkerPos = template.indexOf(scanFor, blockStartPos);
        
        if (nestedBlockPos > -1 && nestedBlockPos < endMarkerPos) {
            nestDepth++;
        }
        
        while (nestDepth > 0 && endMarkerPos > 0) {
            while (nestedBlockPos > -1 && nestedBlockPos < endMarkerPos) {
                // check for another nested block starting before this end-candidate
                nestedBlockPos = template.indexOf(nestedScanFor, nestedBlockPos + nestedScanFor.length());
                if (nestedBlockPos > -1 && nestedBlockPos < endMarkerPos) {
                    nestDepth++;
                }
            }
            // got past end of nested block.
            nestDepth--;
            // locate new candidate for block end.
            endMarkerPos = template.indexOf(scanFor, endMarkerPos+scanFor.length());
            // check for problems with candidate
            if (nestedBlockPos > -1 && nestedBlockPos < endMarkerPos) {
                nestDepth++;
            }
        }
        
        if (endMarkerPos > 0) {
            // keep eating whitespace, up to and including the next linefeed
            // but barf it all back up if non-whitespace is found before next LF
            int blockAndLF = endMarkerPos+scanFor.length();
            for (int i=blockAndLF; i<template.length(); i++) {
                char c = template.charAt(i);
                if (c == ' ' || c == '\t') {
                    // keep eating
                    blockAndLF = i+1;
                } else if (c == '\n') {
                    // done eating (unix)
                    blockAndLF = i+1;
                    break;
                } else if (c == '\r') {
                    // done eating (Win/Mac)
                    if (i+1 < template.length()) {
                        blockAndLF = i+2;
                    } else {
                        blockAndLF = i+1;
                    }
                    break;
                } else {
                    // content on same line as block end!
                    // reverse course!
                    blockAndLF = endMarkerPos+scanFor.length();
                    break;
                }
            }
            return new int[]{endMarkerPos,blockAndLF};
        } else {
            return null;
        }
    }
    
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
        
        /* not sure how to handle this yet, but probably not here anymore
        if (endMarkerPos > 0) {
            // keep eating whitespace, up to and including the next linefeed
            // but barf it all back up if non-whitespace is found before next LF
            int blockAndLF = endMarkerPos+scanFor.length();
            for (int i=blockAndLF; i<template.length(); i++) {
                char c = template.charAt(i);
                if (c == ' ' || c == '\t') {
                    // keep eating
                    blockAndLF = i+1;
                } else if (c == '\n') {
                    // done eating (unix)
                    blockAndLF = i+1;
                    break;
                } else if (c == '\r') {
                    // done eating (Win/Mac)
                    if (i+1 < template.length()) {
                        blockAndLF = i+2;
                    } else {
                        blockAndLF = i+1;
                    }
                    break;
                } else {
                    // content on same line as block end!
                    // reverse course!
                    blockAndLF = endMarkerPos+scanFor.length();
                    break;
                }
            }
            return new int[]{endMarkerPos,blockAndLF};
        } else {
            return null;
        }*/
    }
    
    public boolean hasBody(String openingTag)
    {
        return true;
    }

}