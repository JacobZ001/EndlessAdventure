package com.endlessadventure.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.endlessadventure.entity.Attribute;
import com.endlessadventure.entity.Enemy;
import com.endlessadventure.entity.Skill;
import com.endlessadventure.item.Equipment;

public class EnemyCatalog extends Catalog<EnemyCatalog.EnemyTemplate> {
	private static final String ENEMY_FILE = "enemies.json"; 
	
	public record EnemyTemplate(String id, String name, Integer level, Enemy.Role role, Enemy.Behavior behavior, Map<Attribute,Integer> attributes,
			Map<Skill,Integer> skills, List<String> equipment) {}
	
	private final EquipmentCatalog equipCat;
	
	public EnemyCatalog(EquipmentCatalog equipCat) throws IOException {
		super(EnemyTemplate::id);
		this.equipCat = equipCat;
		load(ENEMY_FILE, EnemyTemplate[].class, t -> {
			build(t); //validate enemy template by building an enemy instance and go through construction validation
			return t;
		});
	}
	
	private Enemy build(EnemyTemplate t) {
		List<Equipment> gear = new ArrayList<>();
		for(String id : t.equipment()) {
			if(equipCat.get(id) instanceof Equipment piece) {
				gear.add(piece);
			}
			else {
				throw new IllegalArgumentException("Invalid equipment id: id not found in catalog: " + id);
			}
		}
		return new Enemy(t.id(), t.name(), t.level(), t.attributes(), t.skills(), t.role(), t.behavior(), gear);
	}

	public Enemy createFromId(String id){
		EnemyTemplate t = get(id);
		if(t == null) {
			throw new IllegalArgumentException("Unknown id when creating enemy: " + id);
		}
		return build(t);
	}
}
