package com.x5.template;

import java.util.Map;

import com.x5.template.filters.ChunkFilter;

public interface ChunkFactory
{
	public Chunk makeChunk();
	public Chunk makeChunk(String templateName);
	public Chunk makeChunk(String templateName, String ext);
	public Map<String,ChunkFilter> getFilters();
}
