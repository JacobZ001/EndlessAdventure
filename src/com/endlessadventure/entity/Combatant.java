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

	public double getAttack() {
		return attack;
	}

	public void setAttack(double attack) {
		this.attack = attack;
	}

	public double getArmor() {
		return armor;
	}

	public void setArmor(double armor) {
		this.armor = armor;
	}

	public int getEnergy() {
		return energy;
	}

	public void setEnergy(int energy) {
		this.energy = energy;
	}

	public int getMaxEnergy() {
		return maxEnergy;
	}
	
}
