package com.x5.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// This bare-bones class provides a bare minimum XML parsing functionality
// and is perfect for thin clients that get a lot of very short messages
// formatted in XML.  The XML must be very well-formed, any extraneous spaces
// or angle brackets will cause the parser to choke.  The need to keep
// the client small outweighs the convenience that better exception handling
// would add here.

public class LiteXml
{
    private String xml;
    // can handle this many child nodes in a parse for children
    // before pausing to grow the endpoints array.
    // nested nodes will survive ok, they don't add to the final count.
    private static final int MAX_PARSE = 15;

    public LiteXml(String xmlNode)
    {
        this.xml = xmlNode;
    }

    public String getNodeType()
    {
        if (xml == null) return null;
        int startAt = 0;
        int headerPos = xml.indexOf("?>");
        if (headerPos > -1) {
            startAt = headerPos+2;
        }
        int begPos = xml.indexOf('<',startAt);
        if (begPos < 0) return null;
        // assume space or > follows nodetype (ie no cr/lf)
        // assume no space between < and nodetype
        int spacePos = xml.indexOf(' ',begPos);
        // assume attribute names and values do not contain >
        int endPos = xml.indexOf('>',begPos);
        if (spacePos > -1 && spacePos < endPos) endPos = spacePos;
        if (endPos < begPos+1) return null;
        String nodeType = xml.substring(begPos+1,endPos);
        return nodeType;
    }

    public String getAttribute(String attr)
    {
        // locate attributes
        if (xml == null) return null;
        // assume attribute names and values do not contain >
        int tagEndPos = xml.indexOf('>');
        // malformed? (no >)?
        if (tagEndPos < 0) return null;
        int spacePos = xml.indexOf(' ');
        // no attributes? (no spaces before >)
        if (spacePos < 0 || spacePos > tagEndPos) return null;
        // pull out just the attribute definitions
        String attrs = xml.substring(spacePos+1,tagEndPos);
        // find our attribute
        int attrPos = attrs.indexOf(attr);
        if (attrPos < 0) return null;
        // find the equals sign
        int eqPos = attrs.indexOf('=',attrPos + attr.length());
        // find the opening quote
        int begQuotePos = attrs.indexOf('"',eqPos);
        if (begQuotePos < 0) return null;
        // find the closing quote
        int endQuotePos = begQuotePos+1;
        do {
            endQuotePos = attrs.indexOf('"',endQuotePos);
            if (endQuotePos < 0) return null;
            if (attrs.charAt(endQuotePos-1) == '\\') {
                // escaped quote, doesn't count -- keep seeking
                endQuotePos++;
            }
        } while (endQuotePos < attrs.length() && attrs.charAt(endQuotePos) != '"');
        if (endQuotePos < attrs.length()) {
            return attrs.substring(begQuotePos+1,endQuotePos);
        } else {
            // never found closing quote
            return null;
        }
    }

    private String getRawNodeValue()
    {
        if (xml == null) return null;
        // assume single node
        String nodeType = getNodeType();
        if (nodeType == null) return null;
        int topTagEnd = xml.indexOf(nodeType) + nodeType.length();
        topTagEnd = xml.indexOf('>',topTagEnd);
        int endTagBeg = xml.lastIndexOf('<');
        if (topTagEnd < 0 || endTagBeg < topTagEnd) {
            return null;
        } else {
            if (xml.indexOf(nodeType,endTagBeg) < 0) {
                // begin and end tags are NOT matched.
                // this string is probably orphaned sub-elements
                return xml;
            } else {
                return xml.substring(topTagEnd+1,endTagBeg);
            }
        }
    }
    
    private boolean isCDATA(String x)
    {
        if (x == null) return false;
        String contents = x.trim();
        if (contents.startsWith("<![CDATA[") && contents.endsWith("]]>")) {
            return true;
        } else {
            return false;
        }
    }
    
    public String getNodeValue()
    {
        String contents = getRawNodeValue();
        if (contents == null) return null;
        
        if (isCDATA(contents)) {
            return contents.trim().substring(9,contents.length()-3);
        } else {
            return LiteXml.unescapeXML(contents);
        }
    }

    /**
     * getChildNodes(nodeType) shares the limitations of the method below.
     *
     * only returns nodes which match specified nodetype.
     *
     * @param nodeType
     * @return
     */
    public LiteXml[] getChildNodes(String nodeType)
    {
        if (nodeType == null) return null;

        LiteXml[] children = getChildNodes();
        if (children == null) return null;

        // count matching nodes
        boolean[] isMatch = new boolean[children.length];
        int matches = 0;
        for (int i=0; i<children.length; i++) {
            LiteXml child = children[i];
            if (child.getNodeType().equals(nodeType)) {
                matches++;
                isMatch[i] = true;
            }
        }

        if (matches == 0) return null;
        if (matches == children.length) return children;

        // go back through and build smaller array of matching-only
        LiteXml[] matchingNodes = new LiteXml[matches];
        matches = 0;
        for (int i=0; i<isMatch.length; i++) {
            if (isMatch[i]) {
                matchingNodes[matches] = children[i];
                matches++;
            }
        }

        return matchingNodes;
    }

    /**
     * There are two things that make this class lightweight.  One is extremely
     * limited support for xpath and zero support for namespaces.  The other is
     * the fact that instead of building a vector or arraylist to handle a
     * potentially large number of child nodes, we build an array of child node
     * positions that doubles in size as capacity needs increase.
     *
     * @return
     */
    public LiteXml[] getChildNodes()
    {
        if (xml == null) return null;
        String insides = getRawNodeValue();
        if (insides == null || isCDATA(insides)) return null;
        // assume all angle brackets are xml boundaries
        // ie, assume no angle brackets in data
        // parse...
        // most docs will have less than MAX_PARSE children
        // (2003aug26 no longer a limit on children -tjm)
        int[] endpoints = new int[MAX_PARSE*2];
        int marker = 0;
        int len = insides.length();
        int count = 0;
        while (marker < len) {
            if (count*2 >= endpoints.length) {
                // hacked to enable unlimited children -tjm
                endpoints = extendArray(endpoints);
            }
            // locate beginning of child
            int opening = insides.indexOf('<',marker);
            if (opening < 0) {
                break;
                //marker = opening + 1;
                //continue;
            }
            // verify that this is not a closing tag eg </TAG>
            if (insides.charAt(opening+1) == '/') return null;
            int closing = insides.indexOf('>',opening+1);
            if (closing < 0) return null;
            // check for self-closing tag eg </TAG attr="data" />
            if (insides.charAt(closing-1) == '/') {
                endpoints[count*2] = opening;
                endpoints[count*2+1] = closing+1;
                count++;
                marker = closing+1;
                continue;
            }
            // scan ahead for end tag, then verify that it matches our tag and
            // not some nested tag of the same nodetype.
            int spacePos = insides.indexOf(' ',opening+1);
            int bracketPos = insides.indexOf('>',opening+1);
            if (spacePos < 0 && bracketPos < 0) return null;
            int typeEnd = spacePos;
            if (typeEnd < 0 || typeEnd > bracketPos) typeEnd = bracketPos;
            String type = insides.substring(opening+1,typeEnd);
            String childEnd = "</" + type;
            int childEndPos = insides.indexOf(childEnd,closing+1);
            String nestedSOB = "<" + type;
            int nestedPos = insides.indexOf(nestedSOB,closing+1);
            // handle nesting if discovered
            while (nestedPos > -1 && nestedPos < childEndPos) {
                // this first time here we matched the most nested endtag...
                // 1:A
                //  2:B
                //   3:C
                //   4:/C
                //   5:D
                //   6:/D
                //  7:/B
                // 8:/A
                // ie we found two and four so keep searching for
                // pairs (3/6,5/7) until you get to an unmatched one (8).
                // It doesn't matter that the pairs aren't correctly matched,
                // just that they are found in pairs.
                childEndPos = insides.indexOf(childEnd,childEndPos + 3);
                if (childEndPos < 0) return null;
                nestedPos = insides.indexOf(nestedSOB,nestedPos + 3);
            }
            int finalBoundary = insides.indexOf('>',childEndPos+2);
            if (finalBoundary < 0) return null; // fatal
            endpoints[count*2] = opening;
            endpoints[count*2+1] = finalBoundary+1;
            count++;
            marker = finalBoundary+1;
        }
        if (count < 1) return null;
        LiteXml[] children = new LiteXml[count];
        for (int i=0; i<count; i++) {
            int beg = endpoints[i*2];
            int end = endpoints[i*2+1];
            LiteXml child = new LiteXml(insides.substring(beg,end));
            children[i] = child;
        }
        return children;
    }

    // extendArray grows the endpoints array as necessary to allow for docs
    // which may have unlimited children elements.  Didn't want to
    // use a vector here since this code is used in applet-land which
    // might execute in a JVM without java.util.Vector.
    private int[] extendArray(int[] endpoints)
    {
        int[] largerArray = new int[endpoints.length + MAX_PARSE*2];
        System.arraycopy(endpoints,0,largerArray,0,endpoints.length);
        return largerArray;
    }

    public LiteXml getFirstChild()
    {
        LiteXml[] children = this.getChildNodes();
        if (children == null) return null;
        return children[0];
    }

    public String getPathValue(String xpathLite)
    {
        LiteXml x = findNode(xpathLite);
        if (x == null) return null;
        return x.getNodeValue();
    }

    public String getNodeValue(String branchPath)
    {
        if (branchPath == null) return null;

        String actualPath = normalizeBranchPath(branchPath);
        return getPathValue(actualPath);
    }

    /**
     * normalizeBranchPath converts a relative child path to an absolute path
     * by prepending a "*" node.
     */
    private String normalizeBranchPath(String branchPath)
    {
        if (branchPath == null) return null;

        if (branchPath.startsWith("/")) {
            return "*" + branchPath;
        } else {
            return "*/" + branchPath;
        }
    }

    public LiteXml findChildNode(String branchPath)
    {
        return findNode( normalizeBranchPath(branchPath) );
    }

    public LiteXml findNode(String xpathLite)
    {
        if (xpathLite == null) return null;
        if (xpathLite.charAt(0) == '/') {
            if (xpathLite.charAt(1) == '/') {
                // leading double slash // not supported
                return null;
            }
            xpathLite = xpathLite.substring(1);
        }
        java.util.StringTokenizer splitter = new java.util.StringTokenizer(xpathLite, "/");
        int depth = splitter.countTokens();

        String[] nodeNames = new String[depth];
        for (int i=0; i<depth; i++) {
            nodeNames[i] = splitter.nextToken();
        }

        return findNode(nodeNames);
    }

    public LiteXml findNode(String[] xpathLite)
    {
        return findNodeX(xpathLite, 0);
    }

    private static boolean isMatch(String nodeName, String pattern)
    {
        if (nodeName == null || pattern == null) return false;
        if (nodeName.equals(pattern)) return true;
        if (pattern.equals("*")) return true;
        return false;
    }

    private LiteXml findNodeX(String[] xpathLite, int x)
    {
        if ( isMatch(this.getNodeType(), xpathLite[x]) ) {
            LiteXml[] childNodes = this.getChildNodes();
            if (childNodes == null) return null;
            for (int i=0; i<childNodes.length; i++) {
                LiteXml child = childNodes[i];
                String nodeType = child.getNodeType();
                if ( isMatch(nodeType, xpathLite[x+1]) ) {
                    if (xpathLite.length == x+2) return child;
                    LiteXml node = child.findNodeX(xpathLite, x+1);
                    if (node != null) return node;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private static final Pattern XML_ENTITY_REGEX = Pattern.compile( "&(#?)([^;]+);" );
    private static final Map<String,String> STD_ENTITIES = getStandardEntities();
    
    public static String unescapeXML( final String xml )
    {
        //Unfortunately, Matcher requires a StringBuffer instead of a StringBuilder
        StringBuffer unescapedOutput = new StringBuffer( xml.length() );

        Matcher m = XML_ENTITY_REGEX.matcher( xml );
        String entity;
        String hashmark;
        String ent;
        int code;
        while ( m.find() ) {
            ent = m.group(2);
            hashmark = m.group(1);
            if ( (hashmark != null) && (hashmark.length() > 0) ) {
                if ( ent.substring(0,1).toLowerCase().equals("x") ) {
                    code = Integer.parseInt( ent.substring(1), 16 );
                } else {
                    code = Integer.parseInt( ent );
                }
                entity = Character.toString( (char) code );
            } else {
                entity = STD_ENTITIES.get( ent );
                if ( entity == null ) {
                    //not a known entity - ignore it
                    entity = "&" + ent + ';';
                }
            }
            m.appendReplacement( unescapedOutput, entity );
        }
        m.appendTail( unescapedOutput );

        return unescapedOutput.toString();
    }

    private static Map<String,String> getStandardEntities()
    {
        Map<String,String> entities = new HashMap<String,String>(10);
        entities.put( "lt", "<" );
        entities.put( "gt", ">" );
        entities.put( "amp", "&" );
        entities.put( "apos", "'" );
        entities.put( "quot", "\"" );
        return entities;
    }
   
    public String toString()
    {
        return xml;
    }
}