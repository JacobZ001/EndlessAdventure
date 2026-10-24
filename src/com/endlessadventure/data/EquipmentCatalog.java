package com.endlessadventure.data;

import java.io.IOException;

import com.endlessadventure.item.Equipment;
import com.endlessadventure.item.Weapon;
import com.endlessadventure.item.Armor;
import com.endlessadventure.item.Shield;

/** handler for reading the predefined catalog of equipments */
public class EquipmentCatalog extends Catalog<Equipment> {
	private static final String WEAPON_FILE = "weapons.json"; 
	private static final String ARMOR_FILE = "armor.json";
	private static final String SHIELD_FILE = "shields.json";
	
	private record WeaponRecord(String id, String name, String description, Weapon.Type type, Equipment.Quality quality) {}
	private record ArmorRecord(String id, String name, String description, Armor.Type type, Equipment.Quality quality) {}
	private record ShieldRecord(String id, String name, String description, Equipment.Quality quality) {}
		
	public EquipmentCatalog() throws IOException {
		super(Equipment::getId);
		load(WEAPON_FILE, WeaponRecord[].class, r -> new Weapon(r.id(), r.name(), r.description(), r.quality(), r.type()));
		load(ARMOR_FILE, ArmorRecord[].class, r -> new Armor(r.id(), r.name(), r.description(), r.quality(), r.type()));
		load(SHIELD_FILE, ShieldRecord[].class, r -> new Shield(r.id(), r.name(), r.description(), r.quality()));
	}
}
