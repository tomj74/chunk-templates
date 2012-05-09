package com.x5.template.filters;

import java.util.HashMap;
import java.util.Map;

import com.x5.template.Chunk;

public abstract class BasicFilter implements ChunkFilter
{
    public abstract String transformText(Chunk chunk, String text, String[] args);
    public abstract String getFilterName();
    public String[] getFilterAliases()
    {
        return null;
    }
    
    public BasicFilter() {}

    public static ChunkFilter[] stockFilters = new ChunkFilter[]{
        new Base64DecodeFilter(),
        new Base64EncodeFilter(),
        new CalcFilter(),
        new CheckedFilter(),
        new DefangFilter(),
        new EscapeXMLFilter(),
        new UnescapeXMLFilter(),
        new FormatFilter(),
        new HexFilter(),
        new HexUpperFilter(),
        new IndentFilter(),
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
        new AlternateFilter(),
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
