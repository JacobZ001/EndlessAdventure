package com.endlessadventure.entity;

public abstract class Combatant extends Entity {
	private double attack;
	private double armor;
	private final int maxEnergy;
	private int energy;

	public Combatant(String name, double MaxHP,
			double attack, double armor, int maxEnergy) {
		super(name, MaxHP);
		this.attack = attack;
		this.armor = armor;
		this.maxEnergy = maxEnergy;
		energy = 0;
	}
}
