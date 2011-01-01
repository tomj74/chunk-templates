package com.x5.template;

public interface ChunkFactory
{
	public Chunk makeChunk();
	public Chunk makeChunk(String templateName);
	public Chunk makeChunk(String templateName, String ext);
}
