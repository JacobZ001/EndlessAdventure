package com.endlessadventure.entity;

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

	/** set the exp to value, return true if the level-up requirement is met */
	public void setExp(int exp) {
		if(exp < 0 || (getLevel() == MAX_LEVEL && exp != 0) || (getLevel() < MAX_LEVEL && exp >= xpToNext(getLevel()))) {
			throw new IllegalArgumentException("Invalid exp for level: Lv " + getLevel() + ": " + exp);
		}
		this.exp = exp;
	}
	
	public void levelUp(int levels) {
		setExp(xpToNext(getLevel()));
	}
}