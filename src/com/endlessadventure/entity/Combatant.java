package com.endlessadventure.entity;

public abstract class Combatant extends Entity {
	private double attack;
	private double armor;
	private int maxEnergy;
	private int energy;

	/** constructor for fully defined combatant */
	public Combatant(String name, double maxHp, double hp, int level,
			double attack, double armor, int maxEnergy, int energy) {
		super(name, maxHp, hp,level);
		setAttack(attack);
		setArmor(armor);
		setMaxEnergy(maxEnergy);
		setEnergy(energy);
	}
	
	/** constructor for newly spawn combatant */
	public Combatant(String name, double maxHp, int level, double attack, double armor, int maxEnergy) {
		this(name, maxHp, maxHp, level, attack, armor, maxEnergy, 0);
	}

	public double getAttack() {
		return attack;
	}
	
	/** @throws IllegalArgumentException */
	public void setAttack(double attack) {
		if(!Double.isFinite(attack) || attack < 0) {
			throw new IllegalArgumentException("Invalid attack: " + attack);
		}
		this.attack = attack;
	}

	public double getArmor() {
		return armor;
	}

	public void setArmor(double armor) {
		if (!Double.isFinite(armor)) {
			throw new IllegalArgumentException("Invalid armor: " + armor);
		}
		this.armor = armor; //can be set to negative, which instead increases the damage taken
	}

	public int getMaxEnergy() {
		return maxEnergy;
	}
	
	public void setMaxEnergy(int maxEnergy) {
		if (maxEnergy < 0) {
			throw new IllegalArgumentException("Invalid maxEnergy: " + maxEnergy);
		}
		this.maxEnergy = maxEnergy;
		energy = Math.min(energy, maxEnergy);
	}
	
	public int getEnergy() {
		return energy;
	}

	/** @throws IllegalArgumentException */
	public void setEnergy(int energy) {
		if(energy < 0 || energy > maxEnergy) {
			throw new IllegalArgumentException("Invalid energy: " + energy);
		}
		this.energy = energy;
	}
}
