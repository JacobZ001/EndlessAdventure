package com.endlessadventure.entity;

import java.util.EnumMap;

public abstract class GameCharacter extends Entity {
		
	private EnumMap<Attribute, Integer> attributes;
	private EnumMap<Training, Integer> trainings;

	public GameCharacter(String name, int level, int hp) {
		super(name, level, hp);
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
