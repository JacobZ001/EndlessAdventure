package com.endlessadventure.entity;

import java.util.EnumMap;

public class CharacterBuild{
	private static final int ATTRIBUTE_BUDGET = 8;
	private static final int ATTRIBUTE_START_CAP = 4;
	private static final int COMBAT_BUDGET = 8;
	private static final int GENERAL_BUDGET = 12;
	private static final int TRAINING_START_CAP = 8;
	
	private EnumMap<Attribute, Integer> attributes;
	private EnumMap<Training, Integer> trainings;
	private int attPoints = ATTRIBUTE_BUDGET;
	private int comPoints = COMBAT_BUDGET;
	private int genPoints = GENERAL_BUDGET;
	
	public void addAttribute(Attribute a) {
		if(attPoints < 1) {
			throw new IllegalArgumentException("Insufficient Attribute points");
		}
		int current = attributes.get(a);
		if(current <= ATTRIBUTE_START_CAP) {
			attributes.put(a, current + 1);
		}
	}
	
	public void addTraining(Training t) {
		int current = trainings.get(t);
		if(current <= TRAINING_START_CAP) {
			trainings.put(t, current + 1);
		}
	}
	
	public void removeAttribute(Attribute a) {
		
	}
	
	public int getAttPoints() {
		return attPoints;
	}

	public void setAttPoints(int attPoints) {
		this.attPoints = attPoints;
	}

	public int getComPoints() {
		return comPoints;
	}

	public void setComPoints(int comPoints) {
		this.comPoints = comPoints;
	}

	public int getGenPoints() {
		return genPoints;
	}

	public void setGenPoints(int genPoints) {
		this.genPoints = genPoints;
	}

	public boolean isComplete() {
		return attPoints == 0 && comPoints == 0 && genPoints == 0;
	}
}
