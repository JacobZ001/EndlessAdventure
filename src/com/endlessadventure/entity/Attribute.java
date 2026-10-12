package com.endlessadventure.entity;

public enum Attribute{ 
	STR("Strength"), 
	DEX("Dexterity"), 
	CON("Constitution"), 
	INT("Intelligence"), 
	PER("Perception"), 
	WIL("Willpower"), 
	CHA("Charisma");
	
	private String name;

	private Attribute(String name) {
		this.name = name;
	}
}
