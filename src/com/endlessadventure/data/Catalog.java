package com.endlessadventure.data;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public abstract class Catalog<T> {
	
	private final Map<String, T> catalog = new LinkedHashMap<>();
	private final Function<T, String> idAccessor;
	
	public Catalog(Function<T, String> idAccessor){
		this.idAccessor = idAccessor;
	}

	/** reads one file's rows and builds each piece; any bad row becomes an IOException naming the file */
	protected final <R> void load(String file, Class<R[]> type, Function<R, T> build) throws IOException {
		R[] templates;
		try (Reader reader = Files.newBufferedReader(Path.of("data", file))) {
			templates = new Gson().fromJson(reader, type);
			if (templates == null) {
				throw new IOException(file + ": empty file");
			}
		} catch (NoSuchFileException e1) {
			throw new IOException("File not found: " + e1.getFile(), e1);
		} catch (JsonParseException e2) {
			throw new IOException(file + ": " + e2.getMessage(), e2);
		}
		
		for (int i = 0; i < templates.length; i++) {
			R template = templates[i];
			try {
				DataValidation.checkNotNull(template, "");
				T entry = build.apply(template);
				String id = idAccessor.apply(entry);
				if(catalog.containsKey(id)) {
					throw new IOException(file + ": duplicate id: " + id);
				}
				catalog.put(id, entry);
			} catch (IllegalArgumentException | MissingFieldException e) {
				throw new IOException(file + " load error: entry." + (i + 1) + ": " + e.getMessage(), e);
			}
		}
	}
	
	/** get the entry with this ID, or null if the catalog has none */
    public T get(String id) {
    	return catalog.get(id);
    }
    
    /** get every entry, in catalog file order */
    public List<T> getAll() {
        return List.copyOf(catalog.values());
    }
}
