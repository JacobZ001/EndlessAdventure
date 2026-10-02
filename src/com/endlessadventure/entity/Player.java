package com.endlessadventure.entity;

public class Player extends Combatant {
	public static final int MAX_LEVEL = 20;
	private static final int[] XP_TO_NEXT = { //index = current level; value = amount of EXP required for leveling up
			0, 							//unused level 0
			48, 64, 80, 96,				//1-4
			196, 224, 252, 280, 308,	//5-9
			480, 520, 560, 600, 640,	//10-14
			816, 864, 912, 960, 1008	//15-19
	};
	private static final double DEFAULT_MAX_HP = 40.0;
	private static final double DEFAULT_ATTACK = 6.0;
	private static final double DEFAULT_ARMOR = 1.0;
	private static final int DEFAULT_MAX_ENERGY = 5;
	private int level;
	private int EXP;

	public Player(String name, double MaxHP, double attack,
			double armor, int maxEnergy, int level, int EXP) {
		super(name, MaxHP, attack, armor, maxEnergy);
		this.level = level;
		this.EXP = EXP;
	}
	public Player(String name, double MaxHP, double attack,
			double armor, int maxEnergy) {
		this(name, MaxHP, attack, armor, maxEnergy, 1, 0);
	}
	
	public Player(String name) {
		this(name,DEFAULT_MAX_HP,DEFAULT_ATTACK,DEFAULT_ARMOR,DEFAULT_MAX_ENERGY);
	}
	
	public static int xpToNext(int level) {
		if (level < 1 || level >= 20) return 0;
		return XP_TO_NEXT[level];
	}

	public int getLevel() {
		return level;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}

	public double getEXP() {
		return EXP;
	}

	public void setEXP(int EXP) {
		this.EXP = EXP;
	}
	
	
}