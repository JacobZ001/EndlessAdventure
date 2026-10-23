package com.endlessadventure.data;

import java.util.List;
import java.util.Map;

import com.endlessadventure.entity.Attribute;
import com.endlessadventure.entity.Enemy;
import com.endlessadventure.entity.Skill;

public class EnemyCatalog extends Catalog<EnemyCatalog.EnemyTemplate> {
	private static final String ENEMY_FILE = "enemies.json"; 
	
	public record EnemyTemplate(String id, String name, int level, Enemy.Role role, Enemy.Stance stance, Map<Attribute,Integer> attributes,
			Map<Skill,Integer> skills, List<String> equipment) {}
	
	private EquipmentCatalog equipCat;
	
	public EnemyCatalog(EquipmentCatalog equipCat) {
		super(Enemy::getId);
		this.equipCat = equipCat;
		load(ENEMY_FILE, EnemyTemplate[].class, t -> {create(t); return t;});
	}

	Enemy create(String id) {
		if(id == null) {throw new IllegalArgumentException("Invalid ID when loading enemy catalog: " + id);}
		return new Enemy(get(id));
	}
}
