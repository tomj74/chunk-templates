package com.x5.template.filters;

import java.util.HashMap;
import java.util.Map;

import com.x5.template.Chunk;

public abstract class BasicFilter implements ChunkFilter
{
    public Object applyFilter(Chunk chunk, String text, String[] args)
    {
        return transformText(chunk, text, args);
    }

    public abstract String transformText(Chunk chunk, String text, String[] args);
    public abstract String getFilterName();
    public String[] getFilterAliases()
    {
        return null;
    }

    public Object applyFilter(Chunk chunk, Object object, String[] args)
    {
        String stringifiedObject = (object == null) ? null : object.toString();
        return transformText(chunk, stringifiedObject, args);
    }

    public BasicFilter() {}

    public static ChunkFilter[] stockFilters = new ChunkFilter[]{
        // String in, String out
        new AlternateFilter(),
        new Base64DecodeFilter(),
        new Base64EncodeFilter(),
        new CalcFilter(),
        new CheckedFilter(),
        new DefangFilter(),
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
        new QuickCalcFilter(),
        new RegexFilter(),
        new SelectedFilter(),
        new SHA1HexFilter(),
        new SHA1Base64Filter(),
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
