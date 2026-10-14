package com.endlessadventure.entity;

import java.util.EnumMap;

public abstract class GameCharacter extends Entity {

	private final EnumMap<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
	private final EnumMap<Skill, Integer> skills = new EnumMap<>(Skill.class);

	public GameCharacter(String name, int level) {
		super(name, level);
	}

	public int getAttribute(Attribute a) {
		return attributes.getOrDefault(a, 0);
	}

	/** upper limits depend on the kind of character and are checked by the subclass
	 *  @throws IllegalArgumentException */
	protected void setAttribute(Attribute a, int v) {
		if(v < 0) {
			throw new IllegalArgumentException("Invalid attribute value: " + a.name() + " : " + v);
		}
		attributes.put(a, v);
	}

	public int getSkill(Skill s) {
		return skills.getOrDefault(s, 0);
	}

	/** upper limits depend on the kind of character and are checked by the subclass
	 *  @throws IllegalArgumentException */
	protected void setSkill(Skill s, int v) {
		if(v < 0) {
			throw new IllegalArgumentException("Invalid skill value: " + s.name() + " : " + v);
		}
		skills.put(s, v);
	}
}
