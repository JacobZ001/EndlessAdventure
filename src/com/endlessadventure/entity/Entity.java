package com.endlessadventure.entity;

public abstract class Entity {
	public static final int MAX_LEVEL = 20;
	private String name;
	private double maxHp;
	private double hp;
	private int level;
	
	/** constructor for fully defined entity */
	public Entity(String name, double maxHp, double hp, int level) {
		setName(name);
		setMaxHp(maxHp);
		setHp(hp);
		setLevel(level);
	}
	
	/** constructor for newly spawn entity */
	public Entity(String name, double maxHp, int level) {
		this(name, maxHp, maxHp, level);
	}
	
	public String getName() {
		return name;
	}
	
	/** @throws IllegalArgumentException */
	public void setName(String name) {
		if (name == null || name.isBlank() || name.strip().length() > 40
				|| name.chars().anyMatch(Character::isISOControl)) {
			throw new IllegalArgumentException("Name must contain 1 to 40 characters without control characters");
		}
		this.name = name.strip();
	}
	
	public double getMaxHp() {
		return maxHp;
	}

	/* @throws IllegalArgumentException */
	public void setMaxHp(double maxHp) {
		if(!Double.isFinite(maxHp) || maxHp <= 0) {
			throw new IllegalArgumentException("Invalid maxHp: " + maxHp);
		}
		this.maxHp = maxHp;
		hp = Math.min(hp, maxHp);
	}
	
	public double getHp() {
		return hp;
	}
	
	/** @throws IllegalArgumentException */
	public void setHp(double hp) {
		if(!Double.isFinite(hp) || hp < 0 || hp > maxHp) {
			throw new IllegalArgumentException("Invalid hp: " + hp);
		}
		this.hp = hp;
	}
	
	public int getLevel() {
		return level;
	}
	
	/** @throws IllegalArgumentException */
	public void setLevel(int level) {
		if(level < 1 || level > MAX_LEVEL) {
			throw new IllegalArgumentException("Invalid level: " + level);
		};
		this.level = level;
	}

	/** take the amount as damage and subtract from hp; return true if the entity is still alive afterwards
	 *  @throws IllegalArgumenException */
	public boolean takeDamage(double amount) {
		if (!Double.isFinite(amount) || amount < 0) {
			throw new IllegalArgumentException("Invalid damage: " + amount);
		}
		hp = Math.max(0, hp - amount);
		return isAlive();
	}

	/** take the amount as restoration and add it to hp
	 *  @throws IllegalArgumenException */
	public void RestoreHP(double amount) {
		if (!Double.isFinite(amount) || amount < 0) {
			throw new IllegalArgumentException("Invalid healing: " + amount);
		}
		hp = Math.min(maxHp, hp + amount);
	}
	
	public boolean isAlive() {
		return hp > 0;
	}
}
