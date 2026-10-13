package com.endlessadventure.entity;

public enum Attribute{ 
	STR("Strength"), 
	DEX("Dexterity"), 
	CON("Constitution"), 
	INT("Intelligence"), 
	PER("Perception"), 
	WIL("Willpower"), 
	CHA("Charisma");
	
	private final String displayName;

	private Attribute(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
