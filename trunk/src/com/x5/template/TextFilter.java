package com.x5.template;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;

/* TextFilter provides a library of text filtering functions
   to support in-template presentation transformations. */

public class TextFilter
{
    public static String FILTER_FIRST = "FILTER_FIRST";
    public static String FILTER_LAST  = "FILTER_LAST";

    public static String applyTextFilter(Chunk context, String filter, String text)
    {
        if (filter == null) return text;

        // filters might be daisy-chained
        int pipePos = findNextFilter(filter);
        if (pipePos >= 0) {
            String firstFilter = filter.substring(0,pipePos);
            String nextFilters = filter.substring(pipePos+1);
            text = applyTextFilter(context, firstFilter, text);
            return applyTextFilter(context, nextFilters, text);
        }

        if (text == null) {
            if (filter.startsWith("onmatch")) {
                // onmatch is special wrt null text
                // onmatch can transform null into an empty string
                // or even a nomatch string
            } else {
                return null;
            }
        }

        if (filter.equals("trim")) {
            // trim leading and trailing whitespace
            return text.trim(); //text.replaceAll("^\\s+","").replaceAll("\\s+$","");
        } else if (filter.equals("qs") || filter.equals("quoted") || filter.equals("quotedstring")) {
            // qs is a quoted string - escape " and ' with backslashes
            text = Chunk.findAndReplace(text,"\"","\\\"");
            text = Chunk.findAndReplace(text,"'","\\'");
            return text;
        } else if (filter.equals("uc")) {
            // uppercase
            return text.toUpperCase();
        } else if (filter.equals("lc")) {
            // lowercase
            return text.toLowerCase();
        } else if (filter.equals("html") || filter.equals("htmlescape") || filter.equals("htmlesc")
        		|| filter.equals("xml") || filter.equals("xmlescape") || filter.equals("xmlesc")) {
            // html-escape
            text = Chunk.findAndReplace(text,"&","&amp;");
            text = Chunk.findAndReplace(text,"<","&lt;");
            text = Chunk.findAndReplace(text,">","&gt;");
            text = Chunk.findAndReplace(text,"\"","&quot;");
            text = Chunk.findAndReplace(text,"'","&apos;");
            return text;
        } else if (filter.equals("url") || filter.equals("urlencode")) {
            // url-encode
            try {
                return java.net.URLEncoder.encode(text,"UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                return text;
            }
        } else if (filter.equals("urldecode")) {
            // url-decode
            try {
                return java.net.URLDecoder.decode(text, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                return text;
            }
        } else if (filter.equals("translate") || filter.equals("xlate") || filter.equals("__") || filter.equals("_")) {
            // surround with translate tags, escape brackets
            return markForTranslation(text);
        } else if (filter.startsWith("sprintf")) {
            // apply sprintf formatting, target string should be a number
            // (if not, filter will be skipped)
            return applyFormatString(text, filter);
        } else if (filter.startsWith("ondefined")) {
            // direct transform if text is defined
            return (text.trim().equals("")) ? "" : applyDirectTransform(context,filter);
        } else if (filter.startsWith("onmatch")) {
            // case-style transform with optional nomatch postfix
            return applyMatchTransform(context, text,filter);
        } else if (filter.startsWith("s/")) {
            // regular expression (regex)
            return applyRegex(text,filter);
        } else if (filter.equals("base64") || filter.equals("base64encode")) {
            // base64-encode
            return base64(text);
        } else if (filter.equals("base64decode")) {
            // base64-decode
            return base64Decode(text);
        } else if (filter.equals("md5") || filter.equals("md5hex")) {
            // md5 hash (hex)
            return md5Hex(text);
        } else if (filter.equals("md5base64") || filter.equals("md5b64")) {
            // md5 hash (base64)
            return md5Base64(text);
        } else if (filter.equals("sha1") || filter.equals("sha1hex")) {
            // md5 hash (hex)
            return sha1Hex(text);
        } else if (filter.equals("sha1base64") || filter.equals("sha1b64")) {
            // md5 hash (base64)
            return sha1Base64(text);
        } else if (filter.equalsIgnoreCase("hex")) {
            String hex = null;
            try {
                hex = new java.math.BigInteger(1,text.getBytes("UTF-8")).toString(16);
            } catch (java.io.UnsupportedEncodingException e) {
                hex = new java.math.BigInteger(1,text.getBytes()).toString(16);
            }
            if (hex == null) return text;
            return (filter.equals("HEX")) ? hex.toUpperCase() : hex;
        } else if (filter.equals("ordsuffix") || filter.equals("th")) {
            // 1 -> 1st, 2 -> 2nd, 3 -> 3rd etc.
            return ordinalSuffix(text);
        } else if (filter.equals("defang") || filter.equals("neuter") || filter.equals("noxss")) {
            return defang(text);
        } else if (filter.startsWith("sel")) {
            // selected(value) is convenience syntax for
            // onmatch(/^value$/, selected="selected" )
            // supports comparing two tags eg {~tagA|select(~tagB)}
            return selected(context, text, filter);
        } else if (filter.startsWith("check")) {
            // checked(value) is convenience syntax for
            // onmatch(/^value$/, checked="checked" )
            // supports comparing two tags eg {~tagA|check(~tagB)}
            return checked(context, text, filter);
        } else if (filter.startsWith("qcalc")) {
            // simple x+y addition, subtraction, etc.
            return applyQuickCalc(text, filter);
        } else if (filter.startsWith("calc")) {
            // exec simple eval
            return easyCalc(text,filter);
        } else if (filter.startsWith("indent")) {
            // indent
            return applyIndent(text,filter);
        } else {
            // ?? unknown filter, do nothing
            return text;
        }
    }
    
    private static String markForTranslation(String text)
    {
        if (text == null) return null;
        text = Chunk.findAndReplace(text,"[","\\[");
        text = Chunk.findAndReplace(text,"]","\\]");
        return LocaleTag.LOCALE_SIMPLE_OPEN + text + LocaleTag.LOCALE_SIMPLE_CLOSE;
    }
    
    private static String defang(String text)
    {
        // keep only a very restrictive set of harmless
        // characters, eg when quoting back user input
        // in a server-generated page, to prevent xss
        // injection attacks.
        return applyRegex(text, "s/[^A-Za-z0-9@\\!\\?\\*\\#\\$\\(\\)\\+\\=\\:\\;\\,\\~\\/\\._-]//g");
    }
    
    private static final String SELECTED_TOKEN = " selected=\"selected\" ";
    private static final String CHECKED_TOKEN = " checked=\"checked\" ";
    
    private static String selected(Chunk context, String text, String filter)
    {
        return selected(context, text, filter, SELECTED_TOKEN);
    }
    
    private static String checked(Chunk context, String text, String filter)
    {
        return selected(context, text, filter, CHECKED_TOKEN);
    }
    
    private static String selected(Chunk context, String text, String filter, String token)
    {
        if (text == null) return "";

        String[] args = parseArgs(filter);
        // no arg!!  so, just return token if text is non-null
        if (args == null) return token;
        
        String testValue = args[0];
        int delimPos = testValue.indexOf(",");
        if (delimPos > -1) {
            token = testValue.substring(delimPos+1);
            testValue = testValue.substring(0,delimPos);
        }
        
        if (testValue.charAt(0) == '~') {
            // this is a sneaky way of allowing {~xyz|sel(~tag)}
            // -- flip it into an onmatch, and let it get re-eval'ed:
            //
            // {~tag|onmatch(/^[text]$/,SELECTED_TOKEN)}
            //
            // I think this means that this would have to be the
            // final filter in the chain, but I can't imagine
            // wanting to filter the output token.
            //
            // The more crazy crap like this that I do, the more
            // I think the Chunk tag table needs to just get passed
            // into applyTextFilter -- but, alas, with recursion
            // resolution, this is not so simple.
            //
            String xlation = testValue + "|onmatch(/^"
                + escapeRegex(text) + "$/," + token + ")";
            return magicBraces(context, xlation);
        }
        
        // simple case, compare to static text string
        if (text.equals(testValue)) {
            return token;
        } else {
            return "";
        }
    }
    
    private static String[] parseArgs(String filter)
    {
        int quote1 = filter.indexOf("\"");
        int quote2;
        boolean isQuoted = true;
        if (quote1 < 0) {
            quote1 = filter.indexOf("(");
            quote2 = filter.lastIndexOf(")");
            if (quote2 < 0) return null;
                
            isQuoted = false;
        } else {
            quote2 = filter.indexOf("\"",quote1+1);
            if (quote2 < 0) quote2 = filter.length();
        }
        
        String arg0 = filter.substring(quote1+1,quote2);

        String arg1 = null;
        if (isQuoted) {
            int quote3 = filter.indexOf("\"",quote2+1);
            if (quote3 > 0) {
                int quote4 = filter.indexOf("\"",quote3+1);
                if (quote4 > 0) {
                    arg1 = filter.substring(quote3+1,quote4);
                }
            }
        }
        
        if (arg1 == null) {
            return new String[]{arg0};
        } else {
            return new String[]{arg0,arg1};
        }
    }

    private static String easyCalc(String text, String filter)
    {
        String[] args = parseArgs(filter);
        String expr = args[0];

        // optional -- format string; only possible when args are quoted
        String fmt = null;
        if (args.length > 1) {
            fmt = args[1];
        }

        if (expr.indexOf("x") < 0) expr = "x"+expr;
        expr = expr.replace("\\$","");
        try {
            return Calc.evalExpression(expr,fmt,new String[]{"x"},new String[]{text});
        } catch (NumberFormatException e) {
            // not a number?  no-op
            return text;
        } catch (NoClassDefFoundError e) {
        	return "[ERROR: jeplite jar missing from classpath! calc filter requires jeplite library]";
        }
    }

    private static String ordinalSuffix(String num)
    {
        if (num == null) return null;
        int x = Integer.parseInt(num);
        int mod100 = x % 100;
        int mod10 = x % 10;
        if (mod100 - mod10 == 10) {
            return num + "th";
        } else {
            switch (mod10) {
              case 1: return num + "st";
              case 2: return num + "nd";
              case 3: return num + "rd";
              default: return num + "th";
            }
        }
    }

    private static String applyDirectTransform(Chunk context, String formatString)
    {
        int parenPos = formatString.indexOf("(");
        int finalParen = formatString.lastIndexOf(")");

        String output;
        if (parenPos > 0) {
            if (finalParen == parenPos + 1) return "";
            if (finalParen > 0) {
                output = formatString.substring(parenPos+1,finalParen);
            } else {
                output = formatString.substring(parenPos+1);
            }
            // add braces if necessary
            return magicBraces(context, output);
        } else {
            return "";
        }
    }

    private static String magicBraces(Chunk context, String output)
    {
        char firstChar = output.charAt(0);
        if (firstChar == '~' || firstChar == '+') {
        	if (context == null || context.isConforming()) {
        		return "{"+output+"}";
        	} else {
        		String tagOpen = context.makeTag("XXX");
        		tagOpen = TextFilter.applyRegex(tagOpen, "s/XXX.*//");
        		
        		String tag = context.makeTag(output);
        		
        		tag = Chunk.findAndReplace(tag, tagOpen+'~', tagOpen);
        		tag = Chunk.findAndReplace(tag, tagOpen+'+', TemplateSet.INCLUDE_SHORTHAND);
        		
        		return tag;
        	}
        } else if (firstChar == '^') {
        	if (context == null) {
        		// internally, {^xyz} is just an alias for {~.xyz}
        		return TemplateSet.DEFAULT_TAG_START+'.'+output.substring(1)+TemplateSet.DEFAULT_TAG_END;
        	} else {
        		return context.makeTag('.'+output.substring(1));
        	}
        } else {
            return output;
        }
    }

    private static String applyMatchTransform(Chunk context, String text, String formatString)
    {
        // scan for next regex, check for match, kick out on first match
        int cursor = 8;
        while (text != null && cursor < formatString.length() && formatString.charAt(cursor) != ')') {
            if (formatString.charAt(cursor) == ',') cursor++;
            if (formatString.charAt(cursor) == 'm') cursor++;
            if (formatString.charAt(cursor) == '/') cursor++;
            int regexEnd = nextRegexDelim(formatString,cursor);
            if (regexEnd < 0) return text; // fatal, unmatched regex boundary

            String pattern = formatString.substring(cursor,regexEnd);

            // check for modifiers between regex end and comma
            int commaPos = formatString.indexOf(",",regexEnd+1);
            if (commaPos < 0) return text; // fatal, missing argument delimiter

            boolean ignoreCase = false;
            boolean multiLine = false;
            boolean dotAll = false;

            for (int i=commaPos-1; i>regexEnd; i--) {
                char option = formatString.charAt(i);
                if (option == 'i') ignoreCase = true;
                if (option == 'm') multiLine = true;
                if (option == 's') dotAll = true; // dot matches newlines too
            }

            if (multiLine) pattern = "(?m)" + pattern;
            if (ignoreCase) pattern = "(?i)" + pattern;
            if (dotAll) pattern = "(?s)" + pattern;

            // scan for a comma not preceded by a backslash
            int nextMatchPos = nextArgDelim(formatString,commaPos+1);
            if (nextMatchPos > 0) {
                cursor = nextMatchPos;
            } else {
                // scan for close-paren
                int closeParen = nextUnescapedDelim(")",formatString,commaPos+1);
                if (closeParen > 0) {
                    cursor = closeParen;
                } else {
                    cursor = formatString.length();
                }
            }

            if (matches(text,pattern)) {
                if (cursor == commaPos + 1) return "";
                String output = formatString.substring(commaPos+1,cursor);
                return magicBraces(context,output);
            }
        }

        // reached here?  no match
        int elseClause = formatString.lastIndexOf("nomatch(");
        if (elseClause > 0) {
            String output = formatString.substring(elseClause + "nomatch(".length());
            if (output.endsWith(")")) output = output.substring(0,output.length()-1);
            if (output.length() == 0) return output;
            return magicBraces(context,output);
        } else {
            // standard behavior without a nomatch clause is blank output
            return "";
        }
    }

    private static String applyFormatString(String text, String formatString)
    {
        // strip calling wrapper ie "sprintf(%.03f)" -> "%.03f"
        if (formatString.startsWith("sprintf(")) {
            formatString = formatString.substring(8);
            if (formatString.endsWith(")")) {
                formatString = formatString.substring(0,formatString.length()-1);
            }
        }
        // strip quotes if arg is quoted
        char first = formatString.charAt(0);
        char last = formatString.charAt(formatString.length()-1);
        if (first == last && (first == '\'' || first == '"')) {
            formatString = formatString.substring(1,formatString.length()-1);
        }

        return formatNumberFromString(formatString, text);
    }

    public static String formatNumberFromString(String formatString, String value)
    {
        char expecting = formatString.charAt(formatString.length()-1);
        try {
            if ("sS".indexOf(expecting) > -1) {
                return String.format(formatString, value);
            } else if ("eEfgGaA".indexOf(expecting) > -1) {
                float f = Float.valueOf(value);
                return String.format(formatString, f);
            } else if ("doxX".indexOf(expecting) > -1) {
                if (value.trim().startsWith("#")) {
                    long l = Long.parseLong(value.trim().substring(1),16);
                    return String.format(formatString, l);
                } else if (value.trim().startsWith("0X") || value.trim().startsWith("0x")) {
                    long l = Long.parseLong(value.trim().substring(2),16);
                    return String.format(formatString, l);
                } else {
                    float f = Float.valueOf(value);
                    return String.format(formatString, (long)f);
                }
            } else if ("cC".indexOf(expecting) > -1) {
                if (value.trim().startsWith("0X") || value.trim().startsWith("0x")) {
                    int i = Integer.parseInt(value.trim().substring(2),16);
                    return String.format(formatString, (char)i);
                } else {
                    float f = Float.valueOf(value);
                    return String.format(formatString, (char)f);
                }
            } else {
                return "[Unknown format "+expecting+": \""+formatString+"\","+value+"]";
            }
        } catch (NumberFormatException e) {
            return value;
        } catch (java.util.IllegalFormatException e) {
            return "["+e.getClass().getName()+": "+e.getMessage()+" \""+formatString+"\","+value+"]";
        }
    }

    private static String applyQuickCalc(String text, String calc)
    {
        if (text == null) return null;
        if (calc == null) return text;

        // strip calling wrapper ie "calc(-30)" -> "-30"
        if (calc.startsWith("qcalc(")) {
            calc = calc.substring(6);
            if (calc.endsWith(")")) {
                calc = calc.substring(0,calc.length()-1);
            }
        }

        try {
            if (text.indexOf(".") > 0 || calc.indexOf(".") > 0) {
                double x = Double.parseDouble(text);
                char op = calc.charAt(0);
                double y = Double.parseDouble(calc.substring(1));

                //System.err.println("float-op: "+op+" args: "+x+","+y);

                double z = x;
                if (op == '-') z = x - y;
                if (op == '+') z = x + y;
                if (op == '*') z = x * y;
                if (op == '/') z = x / y;
                if (op == '%') z = x % y;
                return Double.toString(z);

            } else {
                long x = Long.parseLong(text);
                char op = calc.charAt(0);
                long y = Long.parseLong(calc.substring(1));

                //System.err.println("int-op: "+op+" args: "+x+","+y);

                long z = x;
                if (op == '-') z = x - y;
                if (op == '+') z = x + y;
                if (op == '*') z = x * y;
                if (op == '/') z = x / y;
                if (op == '%') z = x % y;
                if (op == '^') z = Math.round( Math.pow(x,y) );
                return Long.toString(z);
            }
        } catch (NumberFormatException e) {
            return text;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static String base64Decode(String text)
    {
        byte[] decoded = null;
        // try base 64 using two potentially available 3rd party classes
        try {
            // 1. would this really compile if sun.misc.BASE64Encoder weren't on the classpath?
            // 2. why is BASE in all caps?  is it an acronym?
            sun.misc.BASE64Decoder decoder =
                (sun.misc.BASE64Decoder) Class.forName("sun.misc.BASE64Decoder").newInstance();
            decoded = decoder.decodeBuffer(text);
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (java.io.IOException e) {
        }

        if (decoded == null) {
            // hmm, that didn't work.  maybe com.x5.util.Base64 is available?
            byte[] textBytes;
            try {
                textBytes = text.getBytes("UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                textBytes = text.getBytes();
            }
            try {
                Class b64 = Class.forName("com.x5.util.Base64");
                Class[] paramTypes = new Class[] { byte[].class, Integer.TYPE, Integer.TYPE };
                java.lang.reflect.Method decode = b64.getMethod("decode", paramTypes);
                decoded = (byte[]) decode.invoke(null, new Object[]{ textBytes, new Integer(0), new Integer(textBytes.length) });
            } catch (ClassNotFoundException e2) {
            } catch (NoSuchMethodException e2) {
            } catch (IllegalAccessException e2) {
            } catch (java.lang.reflect.InvocationTargetException e2) {
            }
        }

        if (decoded == null) {
            // on failure -- return original bytes
            return text;
        } else {
            // convert decoded bytes to string
            try {
                return new String(decoded,"UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                return new String(decoded);
            }
        }
    }

    public static String base64(String text)
    {
        try {
            byte[] textBytes = text.getBytes("UTF-8");
            return base64(textBytes);
        } catch (java.io.UnsupportedEncodingException e) {
            return base64(text.getBytes());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static String base64(byte[] bytes)
    {
        // try base 64 using two potentially available 3rd party classes
        try {
            // 1. would this really compile if sun.misc.BASE64Encoder weren't on the classpath?
            // 2. why is BASE in all caps?  is it an acronym?
            sun.misc.BASE64Encoder encoder =
                (sun.misc.BASE64Encoder) Class.forName("sun.misc.BASE64Encoder").newInstance();
            return encoder.encode(bytes);
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        // hmm, that didn't work.  maybe com.x5.util.Base64 is available?
        try {
            Class b64 = Class.forName("com.x5.util.Base64");
            Class[] paramTypes = new Class[] { byte[].class };
            java.lang.reflect.Method encode = b64.getMethod("encodeBytes", paramTypes);
            String b64text = (String) encode.invoke(null, new Object[]{ bytes });
            return b64text;
        } catch (ClassNotFoundException e2) {
        } catch (NoSuchMethodException e2) {
        } catch (IllegalAccessException e2) {
        } catch (java.lang.reflect.InvocationTargetException e2) {
        }

        // on failure -- return original bytes
        try {
            return new String(bytes,"UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(bytes);
        }
    }

    public static String hashCrypt(String alg, String text, boolean base64)
    {
        // make byte array out of text
        byte[] textBytes;
        try {
            textBytes = text.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            textBytes = text.getBytes();
        }

        // attempt hashing algorithm
        try {
            java.security.MessageDigest hasher = java.security.MessageDigest.getInstance(alg);
            hasher.update(textBytes,0,textBytes.length);
            if (base64) {
                // return as base64-encoded string
                return base64(hasher.digest());
            } else {
                // return as lowercase hex string
                return new java.math.BigInteger(1,hasher.digest()).toString(16);
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            return text;
        }
    }

    public static String md5Hex(String text)
    {
        return md5(text, false);
    }

    public static String md5Base64(String text)
    {
        return md5(text, true);
    }

    public static String md5(String text, boolean base64)
    {
        return hashCrypt("MD5",text,base64);
    }

    public static String sha1Hex(String text)
    {
        return sha1(text, false);
    }

    public static String sha1Base64(String text)
    {
        return sha1(text, true);
    }

    public static String sha1(String text, boolean base64)
    {
        return hashCrypt("SHA-1",text,base64);
    }

    public static int nextArgDelim(String arglist, int searchFrom)
    {
        return nextUnescapedDelim(",",arglist,searchFrom);
    }

    public static int nextRegexDelim(String regex, int searchFrom)
    {
        return nextUnescapedDelim("/",regex,searchFrom);
    }

    public static int nextUnescapedDelim(String delim, String regex, int searchFrom)
    {
        int delimPos = regex.indexOf(delim, searchFrom);

        boolean isProvenDelimeter = false;
        while (!isProvenDelimeter) {
            // count number of backslashes that precede this forward slash
            int bsCount = 0;
            while (delimPos-(1+bsCount) >= searchFrom && regex.charAt(delimPos - (1+bsCount)) == '\\') {
                bsCount++;
            }
            // if odd number of backslashes precede this delimiter char, it's escaped
            // if even number precede, it's not escaped, it's the true delimiter
            // (because it's preceded by either no backslash or an escaped backslash)
            if (bsCount % 2 == 0) {
                isProvenDelimeter = true;
            } else {
                // keep looking for real delimiter
                delimPos = regex.indexOf(delim, delimPos+1);
                // if the regex is not legal (missing delimiters??), bail out
                if (delimPos < 0) return -1;
            }
        }
        return delimPos;
    }

    public static String applyRegex(String text, String regex)
    {
        // parse perl-style regex a la s/find/replace/gmi
        int patternStart = 2;
        int patternEnd = nextRegexDelim(regex, patternStart);

        // if the regex is not legal (missing delimiters), bail out
        if (patternEnd < 0) return text;

        int replaceEnd = nextRegexDelim(regex, patternEnd+1);
        if (replaceEnd < 0) return text;

        boolean greedy = false;
        boolean ignoreCase = false;
        boolean multiLine = false;
        boolean dotAll = false;

        for (int i=regex.length()-1; i>replaceEnd; i--) {
            char option = regex.charAt(i);
            if (option == 'g') greedy = true;
            if (option == 'i') ignoreCase = true;
            if (option == 'm') multiLine = true;
            if (option == 's') dotAll = true; // dot matches newlines too
        }

        String pattern = regex.substring(patternStart,patternEnd);
        String replaceWith = regex.substring(patternEnd+1,replaceEnd);
        replaceWith = parseRegexEscapes(replaceWith);
        // re-escape escaped backslashes, ie \ -> \\
        replaceWith = Chunk.findAndReplace(replaceWith,"\\","\\\\");

        if (multiLine) pattern = "(?m)" + pattern;
        if (ignoreCase) pattern = "(?i)" + pattern;
        if (dotAll) pattern = "(?s)" + pattern;

        boolean caseConversions = false;
        if (replaceWith.matches(".*\\\\[UL][\\$\\\\]\\d.*")) {
        	// this monkey business marks up case-conversion blocks
        	// since java's regex engine doesn't support perl-style
        	// case-conversion.  but we do :)
        	caseConversions = true;
            replaceWith = replaceWith.replaceAll("\\\\([UL])[\\$\\\\](\\d)", "!$1@\\$$2@$1!");
        }
        
        try {
        	String result = null;
        	
            if (greedy) {
                result = text.replaceAll(pattern,replaceWith);
            } else {
                result = text.replaceFirst(pattern,replaceWith);
            }
            
            if (caseConversions) {
            	return applyCaseConversions(result);
            } else {
            	return result;
            }
        } catch (IndexOutOfBoundsException e) {
            return text + "[REGEX "+regex+" Error: "+e.getMessage()+"]";
        }
    }
    
    private static String applyCaseConversions(String result)
    {
    	StringBuilder x = new StringBuilder();
    	
    	Matcher m = Pattern.compile("!U@(.*?)@U!").matcher(result);
    	int last = 0;
        while (m.find()) {
            x.append(result.substring(last, m.start()));
            x.append(m.group(1).toUpperCase());
            last = m.end();
        }
        if (last > 0) {
        	x.append(result.substring(last));
            result = x.toString();
            x = new StringBuilder();
            last = 0;
        }
        
    	m = Pattern.compile("!L@(.*?)@L!").matcher(result);
        while (m.find()) {
            x.append(result.substring(last, m.start()));
            x.append(m.group(1).toLowerCase());
            last = m.end();
        }
        if (last > 0) {
        	x.append(result.substring(last));
        	return x.toString();
        } else {
        	return result;
        }
    }

    private static String parseRegexEscapes(String str)
    {
        if (str == null) return str;

        char[] strArr = str.toCharArray();
        boolean escape = false;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < strArr.length; ++i) {
            if (escape) {
                if (strArr[i] == 'b') {
                    buf.append('\b');
                } else if (strArr[i] == 't') {
                    buf.append('\t');
                } else if (strArr[i] == 'n') {
                    buf.append('\n');
                } else if (strArr[i] == 'r') {
                    buf.append('\r');
                } else if (strArr[i] == 'f') {
                    buf.append('\f');
                } else if (strArr[i] == 'U') {
                	buf.append("\\U");
                } else if (strArr[i] == 'L') {
                	buf.append("\\L");
                } else if (strArr[i] == 'u') {
                    // Unicode escape
                    int utf = Integer.parseInt(str.substring(i + 1, i + 5), 16);
                    buf.append((char)utf);
                    i += 4;
                } else if (Character.isDigit(strArr[i])) {
                    // Octal escape
                    int j = 0;
                    for (j = 1; (j < 2) && (i + j < strArr.length); ++j) {
                        if (!Character.isDigit(strArr[i+j]))
                            break;
                    }
                    int octal = Integer.parseInt(str.substring(i, i + j), 8);
                    buf.append((char)octal);
                    i += j-1;
                } else {
                    buf.append(strArr[i]);
                }
                escape = false;
            } else if (strArr[i] == '\\') {
                escape = true;
            } else {
                buf.append(strArr[i]);
            }
        }
        return buf.toString();
    }

    public static String[] splitFilters(String filter)
    {
        int startOfNext = findNextFilter(filter);
        if (startOfNext < 0) {
            return new String[]{filter};
        }

        java.util.ArrayList<String> filterList = new java.util.ArrayList<String>();
        while (startOfNext >= 0) {
            filterList.add(filter.substring(0,startOfNext));
            filter = filter.substring(startOfNext+1);
            startOfNext = findNextFilter(filter);
        }
        filterList.add(filter);
        String[] filters = new String[filterList.size()];
        return filterList.toArray(filters);
    }

    private static int findNextFilter(String filter)
    {
        int pipePos = filter.indexOf('|');

        if (pipePos >= 0 && filter.startsWith("s/")) {
            // tricky case -- skip pipes that appear inside regular expressions
            int regexEnd = nextRegexDelim(filter,2);
            if (regexEnd < 0) return pipePos;

            // ok, we have reached the middle delimeter, now find the the closer
            regexEnd = nextRegexDelim(filter,regexEnd+1);
            if (regexEnd < 0) return pipePos;

            if (regexEnd < pipePos) {
                return pipePos;
            } else {
                return filter.indexOf("|",regexEnd+1);
            }
        } else if (pipePos >= 0 && filter.startsWith("onmatch")) {
            // also tricky, find regexes/args and skip pipes inside them
            // there might be several regex matches in one onmatch
            boolean skippedArgs = false;
            int cursor = 8;
            while (!skippedArgs) {
                int slashPos = filter.indexOf("/",cursor);
                if (slashPos < 0) break;
                slashPos = nextRegexDelim(filter,slashPos+1);
                if (slashPos < 0) break;
                int commaPos = nextUnescapedDelim(",",filter,slashPos+1);
                if (commaPos < 0) break;
                int moreArgs = nextUnescapedDelim(",",filter,commaPos+1);
                if (moreArgs < 0) {
                    int closeParen = nextUnescapedDelim(")",filter,commaPos+1);
                    if (closeParen < 0) break;
                    // else
                    if (filter.length() > closeParen+8 && filter.substring(closeParen+1,closeParen+8).equals("nomatch")) {
                        cursor = closeParen+1;
                        skippedArgs = true;
                        // drop out and continue on to exclude nomatch(...) args
                    } else {
                        // this is the end of the onmatch(/regex/,output,/regex/,output)
                        // there is no onmatch(...)nomatch(...) suffix
                        pipePos = filter.indexOf("|",closeParen+1);
                        return pipePos;
                    }
                } else {
                    cursor = moreArgs+1;
                }
            }
            // reached here? find end of nomatch(...) clause
            int openParen = filter.indexOf("(",cursor);
            if (openParen > 0) {
                int closeParen = nextUnescapedDelim(")",filter,openParen+1);
                if (closeParen > 0) {
                    pipePos = filter.indexOf("|",closeParen+1);
                    return pipePos;
                }
            }
            // reached here? something unexpected happened
            pipePos = filter.indexOf("|",cursor);
            return pipePos;
        } else {
            return pipePos;
        }
    }

    public static boolean matches(String text, Pattern pattern)
    {
        // lamest syntax ever...
        Matcher m = pattern.matcher(text);
        return m.find();
    }

    public static boolean matches(String text, String pattern)
    {
        // lamest syntax ever...
        Matcher m = Pattern.compile(pattern).matcher(text);
        return m.find();
    }

    // this is really just /includeIf([!~].*).[^)]*$/
    // with some groupers to parse out the variable pieces
    private static final Pattern parsePattern =
        Pattern.compile("includeIf\\(([\\!\\~])(.*)\\)\\.([^\\)]*)$");
    // this is really just /include.([!~].*)[^)]*$/
    // with some groupers to parse out the variable pieces
    private static final Pattern parsePatternAlt =
        Pattern.compile("include\\.\\(([\\!\\~])(.*)\\)([^\\)]*)$");

    public static String translateIncludeIf(String tag, String open, String close, Map<String,Object> tagTable)
    {
        // {~.includeIf(~asdf).tpl_name}
        // is equiv to {~asdf|ondefined(+tpl_name):}
        // ...and...
        // {~.includeIf(~asdf =~ /xyz/).tpl_name}
        // is equiv to {~asdf|onmatch(/xyz/,+tpl_name)}
        // ...and...
        // {~.includeIf(~asdf == xyz).tpl_name}
        // is equiv to {~asdf|onmatch(/^xyz$/,+tpl_name)}
        // ...and...
        // {~.includeIf(~asdf == ~xyz).tpl_name}
        // is equiv to {~xyz|onmatch(/^__value of ~asdf__$/,+tpl_name)}
        // ...so...
        // just translate and return

        Matcher parseMatcher = parsePattern.matcher(tag);

        if (!parseMatcher.find()) {
            // all is not lost, just yet --
            // will also accept include.(~xyz)asdf since this is how +(~xyz)asdf expands
            parseMatcher = parsePatternAlt.matcher(tag);
            if (!parseMatcher.find()) {
                // ok, now all is lost...
                return "[includeIf bad syntax: "+tag+"]";
            }
        }
        // group zero is the primary toplevel match.
        // paren'd matches start at 1 but only if you first call it with zero.
        parseMatcher.group(0);
        String negater = parseMatcher.group(1);
        String test = parseMatcher.group(2);
        String includeTemplate = parseMatcher.group(3);
        includeTemplate = includeTemplate.replaceAll("[\\|:].*$", "");
        
        if (test.indexOf('=') < 0 && test.indexOf("!~") < 0) {
            // simplest case: no comparison, just a non-null test
            if (negater.charAt(0) == '~') {
                return open + test + "|ondefined(+" + includeTemplate + "):" + close;
            } else {
                return open + test + "|ondefined():+" + includeTemplate + close;
            }
        }

        // now handle straight equality/inequality
        // (~asdf == ~xyz) and (~asdf != ~xyz)
        boolean isNeg = false;
        if (test.indexOf("==") > 0 || (isNeg = test.indexOf("!=") > 0)) {
            String[] parts = test.split("!=|==");
            if (parts.length == 2) {
                String tagA = parts[0].trim();
                String tagB = parts[1].trim();
                String xlation;
                if (tagB.charAt(0) == '~') {
                    // equality (or inequality) of two variables (tags)
                    // resolve right-hand tag reference to a value
                    String tagValue = (String)tagTable.get(tagA);
                    if (tagValue == null) tagValue = "";
                    if (isNeg) {
                        xlation = open + tagB.substring(1)
                            + "|onmatch(/^" + escapeRegex(tagValue) + "$/,)nomatch(+"
                            + includeTemplate + ")" + close;
                    } else {
                        xlation = open + tagB.substring(1)
                            + "|onmatch(/^" + escapeRegex(tagValue) + "$/,+"
                            + includeTemplate + ")nomatch()" + close;
                    }
                } else {
                    // equality (or inequality) of one variable (tag)
                    // compared to a constant string
                    String match = tagB;
                    // allow tagB to be quoted?  if so, strip quotes here
                    if (tagB.charAt(0) == '"' && tagB.charAt(match.length()-1) == '"') {
                    	match = tagB.substring(1, tagB.length()-1);
                    }
                    if (isNeg) {
                        // include the template if the value does not match
                        xlation = open + tagA + "|onmatch(/^" + escapeRegex(match) + "$/,)nomatch(+"
                            + includeTemplate + ")" + close;
                    } else {
                        // include the template if the value matches
                        xlation = open + tagA + "|onmatch(/^" + escapeRegex(match) + "$/,+"
                            + includeTemplate + ")nomatch()" + close;
                    }
                }
                return xlation;
            } else {
                return "[includeIf bad syntax: "+tag+"]";
            }
        }

        // handle pattern match
        String[] parts = test.split("=~");
        boolean neg = false;
        if (parts.length != 2) {
            parts = test.split("!~");
            neg = true;
            if (parts.length != 2) {
                return "[includeIf bad syntax: "+tag+"]";
            }
        }
        String var = parts[0].trim();
        String match = parts[1].trim();
        String xlation;
        if (neg) {
            xlation = open + var + "|onmatch(" + match + ",)nomatch(+"
                + includeTemplate + ")" + close;
        } else {
            xlation = open + var + "|onmatch(" + match + ",+"
                + includeTemplate + ")nomatch()" + close;
        }
        //        System.err.println(xlation);
        return xlation;
    }

    public static String escapeRegex(String x)
    {
        if (matches(x,"^[-A-Za-z0-9_ <>\"']*$")) return x;
        // nothing should leave this sub with its special regex meaning preserved
        StringBuilder noSpecials = new StringBuilder();
        for (int i=0; i<x.length(); i++) {
            char c = x.charAt(i);
            if ((c == ' ') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                           || (c >= '0' && c <= '9')) {
                // do not escape A-Z a-z 0-9, spaces
                noSpecials.append(c);
            } else {
                noSpecials.append("\\");
                noSpecials.append(c);
            }
        }
        return noSpecials.toString();
    }

    public static int grokFinalFilterPipe(String wholeTag, int startHere)
    {
        int cursor = startHere;
        String filter = wholeTag.substring(cursor+1);
        int startOfNext = findNextFilter(filter);
        while (startOfNext >= 0) {
            cursor++;
            cursor+= startOfNext;
            startOfNext = findNextFilter(filter.substring(startOfNext+1));
        }
        return cursor;
    }

    public static int grokValidColonScanPoint(String wholeTag, int startHere)
    {
        // presumably we are starting at the final filter.
        // so, we need to ignore colons that appear within function args
        // eg:
        // s/...:.../...:.../
        // sprintf(...:...)
        // ondefined(...:...)
        // onmatch(/:/,:,/:/,:)
        // onmatch(/:/,:,/:/,:)nomatch(:)
        // onmatch(/(asdf|as:df)/,:)
        if (wholeTag.charAt(startHere) == 's' && wholeTag.charAt(startHere+1) == '/') {
            int regexMid = nextRegexDelim(wholeTag,startHere+2);
            int regexEnd = nextRegexDelim(wholeTag,regexMid+1);
            return regexEnd+1;
        }
        // this assumes no whitespace in the filters
        if (wholeTag.length() > startHere+7 && wholeTag.substring(startHere,startHere+7).equals("onmatch")) {
            // tricky, you have to traverse each regex and consider the contents as blind spots (anything goes)
            boolean skippedArgs = false;
            startHere += 8;
            while (!skippedArgs) {
                int slashPos = wholeTag.indexOf("/",startHere);
                if (slashPos < 0) break;
                slashPos = nextRegexDelim(wholeTag,slashPos+1);
                if (slashPos < 0) break;
                int commaPos = nextUnescapedDelim(",",wholeTag,slashPos+1);
                if (commaPos < 0) break;
                int moreArgs = nextUnescapedDelim(",",wholeTag,commaPos+1);
                if (moreArgs < 0) {
                    int closeParen = nextUnescapedDelim(")",wholeTag,commaPos+1);
                    if (closeParen < 0) break;
                    // else
                    if (wholeTag.length() > closeParen+8 && wholeTag.substring(closeParen+1,closeParen+8).equals("nomatch")) {
                        startHere = closeParen+1;
                        skippedArgs = true;
                        // drop out and continue on to exclude nomatch(...) args
                    } else {
                        // this is the end of the onmatch(/regex/,output,/regex/,output)
                        // there is no onmatch(...)nomatch(...) suffix
                        return closeParen+1;
                    }
                } else {
                    startHere = moreArgs+1;
                }
            }
        }

        // got here?  just one set of parens left to skip, maybe less!

        int openParen = wholeTag.indexOf("(",startHere);
        if (openParen < 0) return startHere;

        int closeParen = nextUnescapedDelim(")",wholeTag,openParen+1);
        if (closeParen < 0) return startHere;

        // if it has args and it's not an onmatch, then this close-paren is the end of the last filter
        return closeParen+1;
    }
    
    public static String joinStringArray(String[] array, String joinFilter)
    {
    	if (array == null) return "";
    	if (array.length == 1) return array[0];
    	
    	// the only arg is the divider
    	String[] args = parseArgs(joinFilter);
    	String divider = args[0];
    	
    	StringBuilder x = new StringBuilder();
    	for (int i=0; i<array.length; i++) {
    		if (i>0 && divider != null) x.append(divider);
    		if (array[i] != null) x.append(array[i]);
    	}
    	return x.toString();
    }
    
    public static String applyIndent(String text, String filter)
    {
        String[] args = parseArgs(filter);
        if (args == null) return text;
        
        String indent = args[0];

        String padChip = " ";
        if (args.length > 1) {
            padChip = args[1];
        } else if (indent.indexOf(",") > 0) {
            String[] args2 = indent.split(",");
            indent = args2[0];
            padChip = args2[1];
        }
        
        try {
            int pad = Integer.parseInt(indent);
            int textLen = text.length();
            
            String linePrefix = padChip;
            for (int i=1; i<pad; i++) linePrefix += padChip;
            
            StringBuilder indented = new StringBuilder();
            indented.append(linePrefix);

            Pattern eol = Pattern.compile("(\\r\\n|\\r\\r|\\n)");
            Matcher m = eol.matcher(text);

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
