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

import com.endlessadventure.item.Equipment;
import com.endlessadventure.item.Weapon;
import com.endlessadventure.item.Armor;
import com.endlessadventure.item.Shield;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/** handler for reading the predefined catalog of equipments */
public final class EquipmentCatalog {
	private static final String WEAPON_FILE = "weapons.json"; 
	private static final String ARMOR_FILE = "armor.json";
	private static final String SHIELD_FILE = "shields.json";
	
	private record WeaponRecord(String id, String name, String description, Weapon.Type type, Equipment.Quality quality) {}
	private record ArmorRecord(String id, String name, String description, Armor.Type type, Equipment.Quality quality) {}
	private record ShieldRecord(String id, String name, String description, Equipment.Quality quality) {}
	
	private final Map<String, Equipment> catalog = new LinkedHashMap<>();
	
	public EquipmentCatalog() throws IOException {
		load(WEAPON_FILE, WeaponRecord[].class, r -> new Weapon(r.id(), r.name(), r.description(), r.quality(), r.type()));
		load(ARMOR_FILE, ArmorRecord[].class, r -> new Armor(r.id(), r.name(), r.description(), r.quality(), r.type()));
		load(SHIELD_FILE, ShieldRecord[].class, r -> new Shield(r.id(), r.name(), r.description(), r.quality()));
	}

	/** reads one file's rows and builds each piece; any bad row becomes an IOException naming the file */
	private <R> void load(String file, Class<R[]> type, Function<R, Equipment> build) throws IOException {
		R[] rows;
		try (Reader reader = Files.newBufferedReader(Path.of("data", file))) {
			rows = new Gson().fromJson(reader, type);
			if (rows == null) {
				throw new IOException(file + ": empty file");
			}
		} catch(NoSuchFileException e1) {
			throw new IOException("File not found: " + e1.getFile(), e1);
		}
		catch (JsonParseException e2) {
			throw new IOException(file + ": " + e2.getMessage(), e2);
		}

		for (int i = 0; i < rows.length; i++) {
			if (rows[i] == null) {
				throw new IOException(file + ": empty entry at position " + (i + 1));
			}
			Equipment piece;
			try {
				piece = build.apply(rows[i]); //the game constructors validate the fields
			} catch (IllegalArgumentException e) {
				throw new IOException(file + ": entry " + (i + 1) + ": " + e.getMessage(), e);
			}
			add(piece, file);
		}
	}
	
	/** the piece with this ID, or null if the catalog has none */
    public Equipment get(String id) {
    	return catalog.get(id);
    }
    
    /** every piece, in catalog file order */
    public List<Equipment> getAll() {
        return List.copyOf(catalog.values());
    }
	
	private void add(Equipment e, String file) throws IOException {
		if(catalog.containsKey(e.getId())) {
			throw new IOException(file + ": duplicate id: " + e.getId());
		}
		catalog.put(e.getId(), e);
	}
}
