package com.endlessadventure.entity;

public class Player extends Combatant {
	private int level;
	private int experience;
	private String background;

	public Player(String name, double MaxHP, double attack,
			double armor, int maxEnergy, String background) {
		super(name, MaxHP, attack, armor, maxEnergy);
	}
}
