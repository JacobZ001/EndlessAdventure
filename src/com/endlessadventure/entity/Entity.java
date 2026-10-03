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
	
	public void setName(String name) {
		this.name = name;
	}
	
	public double getMaxHp() {
		return maxHp;
	}

	/** @throws IllegalArgumentException */
	public void setMaxHp(double maxHp) {
		if(maxHp < 0) {
			throw new IllegalArgumentException("Invalid maxHp: " + maxHp);
		}
		this.maxHp = maxHp;
	}
	
	public double getHp() {
		return hp;
	}
	
	/** @throws IllegalArgumentException */
	public void setHp(double hp) {
		if(hp < 0 ||  hp > maxHp) {
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

	/** take the amount as damage and subtract from hp; return true if the entity is still alive afterwards */
	public boolean takeDamage(double amount) {
		hp -= amount;
		if(hp <= 0) {
			hp = 0;
		}
		return isAlive();
	}

	public void RestoreHP(double amount) {
		hp += amount;
		if(hp > maxHp) {
			hp = maxHp;
		}
	}
	
	public boolean isAlive() {
		return hp > 0;
	}
}
