package com.endlessadventure.entity;

import java.util.EnumMap;

public abstract class GameCharacter extends Entity {

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
	
	public enum Training{
		MELEE(Group.COMBAT, Attribute.STR),
		RANGED(Group.COMBAT, Attribute.DEX),
		SPELLCASTING(Group.COMBAT, Attribute.INT),
		
		ATHLETICS(Group.GENERAL, Attribute.STR),
		
		ACROBATICS(Group.GENERAL, Attribute.DEX),
		SLEIGHT_OF_HAND(Group.GENERAL, Attribute.DEX),
		STEALTH(Group.GENERAL, Attribute.DEX),
		
		ARCANA(Group.GENERAL, Attribute.INT),
		CRAFTING(Group.GENERAL, Attribute.INT),
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
		
		private final Group group;
		private final Attribute default_attribute;

		private Training(Group group, Attribute default_attribute) {
			this.group = group;
			this.default_attribute = default_attribute;
		}

		public enum Group { COMBAT, GENERAL }
		
		public Group getGroup() {
			return this.group;
		}
	}
	
	private EnumMap<Attribute, Integer> attributes;
	private EnumMap<Training, Integer> trainings;

	public GameCharacter(String name, int hp, int level) {
		super(name, hp, level);
		// TODO Auto-generated constructor stub
	}
	
	public int getAttribute(Attribute a) {
		return attributes.get(a);
	}
	
	/** @throws IllegalArgumentException */
	public void setAttribute(Attribute a, int v) {
		if(v < 0) {
			throw new IllegalArgumentException("Invalid attribute value: " + a.name() + " : " + v);
		}
	}
	
	public int getTraining(Training t) {
		return trainings.get(t);
	}
	
	@Override
	public int getMaxHp() {
		return super.getMaxHp() + 8 * getAttribute(Attribute.CON);
	}
}
