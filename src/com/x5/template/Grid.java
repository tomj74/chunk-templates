package com.x5.template;

import java.util.Map;
import com.x5.util.TableData;

public class Grid
{
    private TableData data;
    private Chunk chunk;
    private int across;
    private String cellTemplate;
    private String rowTemplate;
    private String emptyTemplate;
    private Map<String,String> options;

    public static String expandGrid(String params, Chunk ch)
    {
        Grid grid = new Grid(params, ch);
        return grid._cookGrid();
    }

    public Grid(String params, Chunk ch)
    {
        this.chunk = ch;
        parseParams(params);
    }

    private void parseParams(String params)
    {
        if (params == null) return;

        if (params.startsWith(".grid(")) {
            parseFnParams(params);
        } else {
            parseAttributes(params);
        }
    }

    // ^grid(4x,~data[...range...],#cellTemplate,#rowTemplate,#emptyTemplate)
    private void parseFnParams(String params)
    {
        int endOfParams = params.length();
        if (params.endsWith(")")) endOfParams--;
        params = params.substring(".loop(".length(),endOfParams);
        String[] args = params.split(",");
        if (args != null && args.length >= 5) {
            String acrossX = args[0];
            if (acrossX.endsWith("x")) acrossX = acrossX.substring(0,acrossX.length()-1);
            this.across = Integer.parseInt(acrossX);

            String dataVar = args[1];
            fetchData(dataVar);

            this.cellTemplate = args[2];
            this.rowTemplate = args[3];
            this.emptyTemplate = args[4];
        }
    }

    // ^grid data="~data" template="#..." no_data="#..." range="..." rows_per_page="x" page="x"
    private void parseAttributes(String params)
    {
        String acrossX = getAttribute("per_row", params);
        if (acrossX.endsWith("x")) acrossX = acrossX.substring(0,acrossX.length()-1);
        this.across = Integer.parseInt(acrossX);

        String dataVar = getAttribute("data", params);
        fetchData(dataVar);
        this.cellTemplate = getAttribute("cell_template", params);
        this.rowTemplate = getAttribute("row_template", params);
        this.emptyTemplate = getAttribute("no_data", params);

        // okay, this is heinously inefficient, scanning the whole thing every time for each param
        // esp. optional params which probably won't even be there
        String[] optional = new String[]{"range","filler","cells_marker"}; //... what else?
        for (int i=0; i<optional.length; i++) {
            String param = optional[i];
            String val = getAttribute(param, params);
            if (val != null) registerOption(param, val);

            // really?
            if (param.equals("range") && val == null) {
                // alternate range specification via optional params page and per_page
                String perPage = getAttribute("rows_per_page", params);
                String page = getAttribute("page", params);
                if (perPage != null && page != null) {
                    registerOption("range", page + "*" + perPage);
                }
            }
        }
    }

    private void fetchData(String dataVar)
    {
        if (dataVar != null) {
            int rangeMarker = dataVar.indexOf("[");
            if (rangeMarker > 0) {
                int rangeMarker2 = dataVar.indexOf("]",rangeMarker);
                if (rangeMarker2 < 0) rangeMarker2 = dataVar.length();
                String range = dataVar.substring(rangeMarker+1,rangeMarker2);
                dataVar = dataVar.substring(0,rangeMarker);
                registerOption("range",range);
            }
            if (dataVar.startsWith("~")) dataVar = dataVar.substring(1);

            if (chunk != null) {
                Object dataStore = chunk.get(dataVar);
                if (dataStore instanceof TableData) {
                    this.data = (TableData)dataStore;
                } else if (dataStore instanceof String) {
                    this.data = InlineTable.parseTable((String)dataStore);
                }
            }
        }
    }

    private void registerOption(String param, String value)
    {
        if (options == null) options = new java.util.HashMap<String,String>();
        options.put(param,value);
    }

    private String _cookGrid()
    {
        return Grid.cookGrid(data, across, chunk,
                             cellTemplate, rowTemplate, emptyTemplate, options);
    }

    public static String cookGrid(TableData data, int perRow, Chunk context,
                                  String cellTemplate, String rowTemplate, String emptyTemplate,
                                  Map<String,String> opt)
    {
        ContentSource tpl = context.getTemplateSet();

        if (!data.hasNext()) {
            return tpl.fetch(emptyTemplate);
        }

        // TODO honor options like filler template and cells marker

        String[] columnLabels = data.getColumnLabels();

        String marker = (opt == null ? null : (String)opt.get("cells_marker"));
        if (marker != null && marker.startsWith("~")) marker = marker.substring(1);
        if (marker == null) marker = "GRID_ROW";

        ChunkFactory factory = context.getChunkFactory();
        
        StringBuilder cells = new StringBuilder();
        StringBuilder rows = new StringBuilder();
        Chunk rowX = factory.makeChunk(rowTemplate);
        Chunk cellX = factory.makeChunk(cellTemplate);
        // these orphaned chunks need to have access to their ancestry
        int counter = 0;
        int row = -1;
        while (data.hasNext()) {
            if (counter % perRow == 0) {
                if (counter > 0) {
                    rowX.set("R0",row);
                    rowX.set("R1",row+1);
                    rowX.set(marker, cells.toString());
                    rows.append( rowX.toString(context) );
                    cells.setLength(0);
                }
                row++;
            }
            cellX.set("G0",counter);
            cellX.set("G1",counter+1);

            Map<String,String> record = data.nextRecord();

            for (int i=columnLabels.length-1; i>-1; i--) {
                String field = columnLabels[i];
                String value = record.get(field);
                cellX.set(field, value);
                cellX.set("DATA["+i+"]",value);
            }

            cells.append( cellX.toString(context) );

            counter++;
        }

        if (counter > 0) {
            // if final row is not exactly x-across
            int fillCount = perRow - (counter % perRow);
            if (fillCount < perRow) {
                String filler = (opt == null ? null : (String)opt.get("filler"));
                // if filler template was provided
                // then use filler template for each empty cell in the grid
                if (filler != null) {
                    Chunk fillX = factory.makeChunk(filler);
                    for (int i=0; i<fillCount; i++) {
                        fillX.set("G0",counter);
                        fillX.set("G1",counter+1);
                        cells.append( fillX.toString(context) );
                        counter++;
                    }
                }
            }
            rowX.set("R0",row);
            rowX.set("R1",row+1);
            rowX.set(marker, cells.toString());
            rows.append( rowX.toString(context) );
        }

        return rows.toString();
    }

    private String getAttribute(String attr, String toScan)
    {
        return Loop.getAttribute(attr,toScan);
    }
}
