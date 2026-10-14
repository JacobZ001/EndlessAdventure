package com.endlessadventure.entity;

public enum Skill{
	MELEE(Group.COMBAT, Attribute.STR),
	RANGED(Group.COMBAT, Attribute.DEX),
	SPELLCASTING(Group.COMBAT, Attribute.INT),
	
	ATHLETICS(Group.GENERAL, Attribute.STR),
	
	ACROBATICS(Group.GENERAL, Attribute.DEX),
	SLEIGHT_OF_HAND(Group.GENERAL, Attribute.DEX),
	STEALTH(Group.GENERAL, Attribute.DEX),
	
	ARCANA(Group.GENERAL, Attribute.INT),
	//CRAFTING(Group.GENERAL, Attribute.INT), reserved for when crafting/forging/enchantment system is implemented
	HISTORY(Group.GENERAL, Attribute.INT),
	INVESTIGATION(Group.GENERAL, Attribute.INT),
	NATURE(Group.GENERAL, Attribute.INT),
	RELIGION(Group.GENERAL, Attribute.INT),

	ANIMAL_HANDLING(Group.GENERAL, Attribute.PER),
	INSIGHT(Group.GENERAL, Attribute.PER),
	MEDICINE(Group.GENERAL, Attribute.PER),
	SCOUTING(Group.GENERAL, Attribute.PER),
	SURVIVAL(Group.GENERAL, Attribute.PER),
	
	INTIMIDATION(Group.GENERAL, Attribute.WIL),
	
	PERSUASION(Group.GENERAL, Attribute.CHA),
	DECEPTION(Group.GENERAL, Attribute.CHA),
	PERFORMANCE(Group.GENERAL, Attribute.CHA);
	
	public enum Group {
		COMBAT,
		GENERAL;
	}
	
	private final Group group;
	private final Attribute defaultAttribute;

	private Skill(Group group, Attribute defaultAttribute) {
		this.group = group;
		this.defaultAttribute = defaultAttribute;
	}
	
	public Group getGroup() {
		return this.group;
	}

	public Attribute getDefaultAttribute() {
		return defaultAttribute;
	}
}
