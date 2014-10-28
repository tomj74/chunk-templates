package com.x5.util;

import java.util.Map;

public interface TableData
{
    public String[] getColumnLabels();
    public void setColumnLabels(String[] labels);
    public String[] getRow();
    public boolean hasNext();
    public Map<String,Object> nextRecord();
    public void reset();
}
