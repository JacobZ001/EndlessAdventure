package com.endlessadventure.entity;

import java.util.EnumMap;

import com.endlessadventure.inventory.Inventory;

public class Player extends Combatant {	
	private static final int[] XP_TO_NEXT = { //index = current level; value = amount of exp required for leveling up
			48, 64, 80, 96,				//1-4
			196, 224, 252, 280, 308,	//5-9
			480, 520, 560, 600, 640,	//10-14
			816, 864, 912, 960, 1008	//15-19
	};
	private int exp;
	
	private final Inventory inventory = new Inventory();
	
	/** constructor for fully defined player */
	public Player(String name, int hp, int level, int exp) {
		super(name, hp, level);
		setExp(exp);
	}
	
	/** constructor for new default player */
	//TODO implement new player creation
	public Player(String name, CharacterBuild build) {
		this(name, 40, 1, 0);
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
