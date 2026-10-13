package com.endlessadventure.entity;

import java.util.EnumMap;

public abstract class GameCharacter extends Entity {

	private final EnumMap<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
	private final EnumMap<Training, Integer> trainings = new EnumMap<>(Training.class);

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

	public int getTraining(Training t) {
		return trainings.getOrDefault(t, 0);
	}

	/** upper limits depend on the kind of character and are checked by the subclass
	 *  @throws IllegalArgumentException */
	protected void setTraining(Training t, int v) {
		if(v < 0) {
			throw new IllegalArgumentException("Invalid training value: " + t.name() + " : " + v);
		}
		trainings.put(t, v);
	}
}
