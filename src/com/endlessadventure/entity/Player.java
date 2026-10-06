package com.endlessadventure.entity;

import com.endlessadventure.inventory.Inventory;

public class Player extends Combatant {
	private static final int[] XP_TO_NEXT = { //index = current level; value = amount of exp required for leveling up
			48, 64, 80, 96,				//1-4
			196, 224, 252, 280, 308,	//5-9
			480, 520, 560, 600, 640,	//10-14
			816, 864, 912, 960, 1008	//15-19
	};
	private static final double DEFAULT_MAX_HP = 40.0;
	private static final double DEFAULT_ATTACK = 6.0;
	private static final double DEFAULT_ARMOR = 1.0;
	private static final int DEFAULT_MAX_ENERGY = 5;
	private int exp;
	
	private final Inventory inventory = new Inventory();
	
	/** constructor for fully defined player */
	public Player(String name, double maxHp, double hp, int level, double attack, double armor, int maxEnergy, int energy, int exp) {
		super(name, maxHp, hp, level, attack, armor, maxEnergy, energy);
		setExp(exp);
	}
	
	/** constructor for new default player */
	public Player(String name) {
		this(name, DEFAULT_MAX_HP, DEFAULT_MAX_HP, 1, DEFAULT_ATTACK, DEFAULT_ARMOR, DEFAULT_MAX_ENERGY, 0, 0);
	}
	
	/** returns the amount of exp required for the selected level to level-up */
	public static int xpToNext(int level) {
		if (level < 1 || level >= MAX_LEVEL) return Integer.MAX_VALUE;
		return XP_TO_NEXT[level-1];
	}

	public int getExp() {
		return exp;
	}

	/** Set valid within-level progress, for example when loading a save. */
	public void setExp(int exp) {
		if(exp < 0 || (getLevel() == MAX_LEVEL && exp != 0) || (getLevel() < MAX_LEVEL && exp >= xpToNext(getLevel()))) {
			throw new IllegalArgumentException("Invalid exp for level: Lv " + getLevel() + ": " + exp);
		}
		this.exp = exp;
	}

	/** direct level change, does not leave incompatible EXP behind. */
	@Override
	public void setLevel(int level) {
		if ((level == MAX_LEVEL && exp != 0) || (level < MAX_LEVEL && exp >= xpToNext(level))) {
			throw new IllegalArgumentException("Current EXP is invalid for level: " + level);
		}
		super.setLevel(level);
	}
	
	public Inventory getInventory() {
		return inventory;
	}
}
