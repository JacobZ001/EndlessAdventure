package com.endlessadventure.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.endlessadventure.StatRules;
import com.endlessadventure.item.Equipment;

public final class Enemy extends GameCharacter {

	public enum Behavior { PRESSING }
	
	private final Role role;
	private final Behavior behavior;

	public Enemy(String name, int level, Map<Attribute, Integer> attributes, Map<Skill, Integer> skills, Role role, Behavior behavior, List<Equipment> gear) {
		super(name, level, attributes, skills);
		if(role == null) {
			throw new IllegalArgumentException("Invalid enemy constructing: role cannot be null.");
		}
		if(behavior == null) {
			throw new IllegalArgumentException("Invalid enemy constructing: behavior cannot be null.");
		}
		this.role = role;
		this.behavior = behavior;
		for(Equipment piece : gear) {
			if(!equip(piece).isEmpty()) {
				throw new IllegalArgumentException("Conflicting equipment when creating enemy: " + piece.getId());
			}
		}
		setHp(getMaxHp());
	}

	@Override
	public String getDescription() {
		return getName() + "  Lv." + getLevel();
	}

	@Override
	public int getMaxHp() {
		return StatRules.scaleToLevel(role.hpPerTier,getLevel());
	}
	
	public Behavior getBehavior( ) {return behavior;}
	
	public enum Role {
		MOB(21),
		NORMAL(42);
		
		public final int hpPerTier;

		Role(int hpPerTier) {
			this.hpPerTier = hpPerTier;
		}
	}
}
