package com.endlessadventure.entity;

import java.util.List;

import com.endlessadventure.Displayable;
import com.endlessadventure.StatRules;

public abstract class Entity implements Displayable {
	private static final int MAX_NAME_LENGTH = 40;
		
	private String name;
	private int hp;
	private int level;
	
	/** hp starts at 0; subclasses set it once everything getMaxHp reads is initialized */
	public Entity(String name, int level) {
		setName(name);
		setLevel(level);
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public abstract String getDescription();

	@Override
	public List<Detail> getDetails() {
		return List.of(
				Detail.Line.of(getName(), Tone.KEYWORD),
				Detail.Stat.of("Level", level),
				Detail.Stat.of("HP", hp + " / " + getMaxHp(), Attribute.CON),
				Detail.Description.of(this));
	}

	/** @throws IllegalArgumentException */
	public void setName(String name) {
		this.name = checkName(name);
	}
	
	/** returns the stripped name
	 *  @throws IllegalArgumentException */
	public static String checkName(String name) {
		if (name == null || name.isBlank() || name.strip().length() > MAX_NAME_LENGTH
				|| name.chars().anyMatch(Character::isISOControl)) {
			throw new IllegalArgumentException("Name must contain 1 to " + MAX_NAME_LENGTH + " characters without control characters");
		}
		return name.strip();
	}
	
	/** derived from level and attributes, never saved */
	public abstract int getMaxHp();
	
	public int getHp() {
		return hp;
	}
	
	/** @throws IllegalArgumentException */
	public void setHp(int hp) {
		if(hp < 0 || hp > getMaxHp()) {
			throw new IllegalArgumentException("Invalid hp: " + hp);
		}
		this.hp = hp;
	}
	
	public int getLevel() {
		return level;
	}
	
	/** @throws IllegalArgumentException */
	public void setLevel(int level) {
		if(level < 1 || level > StatRules.MAX_LEVEL) {
			throw new IllegalArgumentException("Invalid level: " + level);
		};
		this.level = level;
	}

	/** take the amount as damage and subtract from hp; return true if the entity is still alive afterwards
	 *  @throws IllegalArgumenException */
	public boolean takeDamage(int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("Invalid damage: " + amount);
		}
		hp = Math.max(0, hp - amount);
		return isAlive();
	}

	/** take the amount as restoration and add it to hp
	 *  @throws IllegalArgumenException */
	public void restoreHP(int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("Invalid healing: " + amount);
		}
		hp = Math.min(getMaxHp(), hp + amount);
	}
	
	public boolean isAlive() {
		return hp > 0;
	}
}
