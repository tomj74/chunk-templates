package com.x5.template.filters;

import java.util.HashMap;
import java.util.Map;

import com.x5.template.Chunk;
import com.x5.template.Snippet;
import com.x5.util.ObjectDataMap;

public abstract class BasicFilter implements ChunkFilter
{
    public Object applyFilter(Chunk chunk, String text, FilterArgs args)
    {
        return transformText(chunk, text, args);
    }

    public abstract String transformText(Chunk chunk, String text, FilterArgs args);
    public abstract String getFilterName();
    public String[] getFilterAliases()
    {
        return null;
    }

    public Object applyFilter(Chunk chunk, Object object, FilterArgs args)
    {
        String stringifiedObject = object == null ? null : BasicFilter.stringify(object);
        return transformText(chunk, stringifiedObject, args);
    }

    public static String stringify(Snippet snippet)
    {
        // lose leading comment with origin
        return snippet.toSimpleString();
    }

    public static String stringify(Object object)
    {
        if (object instanceof Snippet) return stringify((Snippet)object);
        return ObjectDataMap.getAsString(object);
    }

    public BasicFilter() {}

    public static ChunkFilter[] stockFilters = new ChunkFilter[]{
        // String in, String out
        new AlternateFilter(),
        new Base64DecodeFilter(),
        new Base64EncodeFilter(),
        new BooleanFilter(),
        new CalcFilter(),
        new CheckedFilter(),
        new DefangFilter(),
        new DefaultFilter(),
        new EscapeQuotesFilter(),
        new EscapeXMLFilter(),
        new UnescapeXMLFilter(),
        new ExecFilter(),
        new FormatFilter(),
        new HexFilter(),
        new HexUpperFilter(),
        new IndentFilter(),
        new LetterCaseFilter(),
        new MD5HexFilter(),
        new MD5Base64Filter(),
        new OnEmptyFilter(),
        new OnDefinedFilter(),
        new OnMatchFilter(),
        new OrdinalSuffixFilter(),
        new PadLeftFilter(),
        new PadRightFilter(),
        new PageFilter(),
        new QuickCalcFilter(),
        new RegexFilter(),
        new SelectedFilter(),
        new SHA1HexFilter(),
        new SHA1Base64Filter(),
        new StringFilter(),
        new TranslateFilter(),
        new URLDecodeFilter(),
        new URLEncodeFilter(),
        // List in, List out
        new SliceFilter(),
        new SortFilter(),
        new ReverseFilter(),
        // List in, String out
        new JoinFilter(),
        new ListIndexFilter(),
        // String in, List out
        new SplitFilter(),
        // List/String in, String out
        new LengthFilter(),
    };

    public static Map<String,ChunkFilter> getStockFilters()
    {
        Map<String,ChunkFilter> filters = new HashMap<String,ChunkFilter>();

        for (ChunkFilter filter : stockFilters) {
            filters.put(filter.getFilterName(), filter);
            String[] aliases = filter.getFilterAliases();
            if (aliases != null) {
                for (String alias : aliases) {
                    filters.put(alias, filter);
                }
            }
        }

        return filters;
    }
}
