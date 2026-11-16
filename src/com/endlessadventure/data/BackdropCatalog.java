package com.endlessadventure.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.endlessadventure.WorldMap.LocationKind;

public class BackdropCatalog extends Catalog<BackdropCatalog.Backdrop> {
	private static final String BACKDROP_FILE = "backdrops.json";
	private static final String BACKDROP_PATH = "assets/backdrops/%s.png";
	
	public record Backdrop(String id, LocationKind kind) {}

	public BackdropCatalog() throws IOException {
		super(Backdrop::id);
		load(BACKDROP_FILE, Backdrop[].class, BackdropCatalog::check);
	}
	
	private static Backdrop check(Backdrop b){
		String file = BACKDROP_PATH.formatted(b.id());
		if(!Files.exists(Path.of(file))){
			throw new IllegalArgumentException("backdrop image not found: " + file);
		}
		return b;
	}

	public List<Backdrop> getAllOfKind(LocationKind kind) {
		return getAll().stream().filter(b -> b.kind() == kind).toList();
	}
}
