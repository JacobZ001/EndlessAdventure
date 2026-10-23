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

public class Catalog<T> {
	
	protected final Map<String, T> catalog = new LinkedHashMap<>();
	protected final Function<T, String> idOf;
	
	public Catalog(Function<T, String> idOf){
		this.idOf = idOf;
	}

	/** reads one file's rows and builds each piece; any bad row becomes an IOException naming the file */
	protected final <R> void load(String file, Class<R[]> type, Function<R, T> build) throws IOException {
		R[] rows;
		try (Reader reader = Files.newBufferedReader(Path.of("data", file))) {
			rows = new Gson().fromJson(reader, type);
			if (rows == null) {
				throw new IOException(file + ": empty file");
			}
		} catch (NoSuchFileException e1) {
			throw new IOException("File not found: " + e1.getFile(), e1);
		} catch (JsonParseException e2) {
			throw new IOException(file + ": " + e2.getMessage(), e2);
		}

		for (int i = 0; i < rows.length; i++) {
			try {
				R row = DataValidation.require(rows[i], "entry." + i);
				T entry = build.apply(row);
				add(entry, file);
			} catch (IllegalArgumentException | MissingFieldException e) {
				throw new IOException(file + ": load error: " + e.getMessage(), e);
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
	
	private void add(T e, String file) throws IOException {
		if(catalog.containsKey(idOf.apply(e))) {
			throw new IOException(file + ": duplicate id: " + idOf.apply(e));
		}
		catalog.put(idOf.apply(e), e);
	}
}
