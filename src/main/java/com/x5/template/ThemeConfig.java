package com.x5.template;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;

import com.x5.template.filters.ChunkFilter;

public class ThemeConfig
{
    public static final String THEME_PATH = "theme_path";
    public static final String LAYER_NAMES = "layers";
    public static final String DEFAULT_EXT = "default_extension";
    public static final String CACHE_MINUTES = "cache_minutes";
    public static final String HIDE_ERRORS = "hide_errors";
    public static final String ERROR_LOG = "error_log";
    public static final String FILTERS = "filters";
    public static final String LOCALE = "locale";
    public static final String ENCODING = "encoding";

    public static final String STD_ERR = "stderr";
    public static final String STANDARD_DEFAULT_EXT = "chtml";

    private String themePath = null;
    private String layerNames = null;
    private String defaultExtension = STANDARD_DEFAULT_EXT;
    private int cacheMinutes = 0;
    private String locale = null;
    private String encoding = null;
    private boolean hideErrors = false;
    private PrintStream errorLog = null;
    private ChunkFilter[] filters = null;

    public ThemeConfig() {}

    public ThemeConfig(Map<String,String> params)
    {
        for (String key : params.keySet()) {
            set(key, params.get(key));
        }
    }

    public void set(String configKey, String value)
    {
        if (configKey == null) return;
        value = sanitize(value);

        if (configKey.equals(THEME_PATH)) {
            this.themePath = value;
        } else if (configKey.equals(LAYER_NAMES)) {
            this.layerNames = value;
        } else if (configKey.equals(DEFAULT_EXT)) {
            this.defaultExtension = value;
        } else if (configKey.equals(LOCALE)) {
            this.locale = value;
        } else if (configKey.equals(ENCODING)) {
            this.encoding = value;
        } else if (configKey.equals(CACHE_MINUTES)) {
            if (value == null) return;
            try {
                this.cacheMinutes = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Chunk Theme config error: cache_minutes must be a number.");
            }
        } else if (configKey.equals(HIDE_ERRORS)) {
            if (value != null && !value.equalsIgnoreCase("FALSE")) {
                this.hideErrors = true;
            }
        } else if (configKey.equals(ERROR_LOG)) {
            if (value != null) {
                if (!value.equalsIgnoreCase(STD_ERR)) {
                    this.errorLog = openForAppend(value);
                }
            }
        } else if (configKey.equals(FILTERS)) {
            if (value != null) {
                // allow filters to be added by class name (look up, instantiate via reflection)
                this.filters = parseFilters(value);
            }
        }
    }

    private String sanitize(String value)
    {
        if (value == null) return value;
        value = value.trim();
        if (value.length() == 0) return null;
        return value;
    }

    private PrintStream openForAppend(String logPath)
    {
        File file = new File(logPath);
        try {
            FileOutputStream out = new FileOutputStream(file, true);
            return new PrintStream(out);
        } catch (FileNotFoundException e) {
            System.err.println("Can not open error log file '" + logPath + "' for append.");
            return null;
        }
    }

    private ChunkFilter[] parseFilters(String filterList)
    {
        ArrayList<ChunkFilter> filters = new ArrayList<ChunkFilter>();

        String[] filterClassNames = filterList.split("[\\s,]+");
        for (String filterClassName : filterClassNames) {
            ChunkFilter filter = createFilterFromClassName(filterClassName);
            if (filter != null) filters.add(filter);
        }
        if (filters.size() == 0) return null;

        ChunkFilter[] filterArray = new ChunkFilter[filters.size()];
        return filters.toArray(filterArray);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ChunkFilter createFilterFromClassName(String filterClassName)
    {
        Object filter = null;
        Class filterClass = null;
        try {
            filterClass = Class.forName(filterClassName);
            filter = filterClass.newInstance();
        } catch (InstantiationException e) {
            if (filterClassName.contains("$")) {
                // support instantiating a nested class
                try {
                    Class outerClass = Class.forName(filterClassName.substring(0, filterClassName.indexOf('$')));
                    Object outerInstance = outerClass.newInstance();
                    filter = filterClass.getDeclaredConstructor(outerClass).newInstance(outerInstance);
                } catch (ClassNotFoundException e2) {
                } catch (InstantiationException e2) {
                } catch (IllegalAccessException e2) {
                } catch (NoSuchMethodException e2) {
                } catch (InvocationTargetException e2) {
                }
            }
            if (filter == null) {
                System.err.println("Could not call constructor for filter: " + filterClassName);
                e.printStackTrace(System.err);
            }
        } catch (IllegalAccessException e) {
            System.err.println("Permission denied adding user-contributed filter: " + filterClassName);
            e.printStackTrace(System.err);
        } catch (ClassNotFoundException e) {
            System.err.println("Filter class not found: " + filterClassName);
            e.printStackTrace(System.err);
        }

        ChunkFilter chunkFilter = null;
        try {
            chunkFilter = (ChunkFilter)filter;
        } catch (ClassCastException e) {
            System.err.println("User-contributed filter rejected; must implement ChunkFilter: " + filterClassName);
            e.printStackTrace(System.err);
        }

        return chunkFilter;
    }

    public String getThemeFolder()
    {
        return this.themePath;
    }

    public String getLayerNames()
    {
        return this.layerNames;
    }

    public String getDefaultExtension()
    {
        return this.defaultExtension;
    }

    public int getCacheMinutes()
    {
        return this.cacheMinutes;
    }

    public String getLocaleCode()
    {
        return this.locale;
    }

    public String getEncoding()
    {
        return this.encoding;
    }

    public boolean hideErrors()
    {
        return this.hideErrors;
    }

    public PrintStream getErrorLog()
    {
        return this.errorLog == null ? System.err : this.errorLog;
    }

    public ChunkFilter[] getFilters()
    {
        return this.filters;
    }
}
