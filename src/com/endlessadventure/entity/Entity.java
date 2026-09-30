package com.endlessadventure.entity;

public abstract class Entity {
	private static int nextID = 1;
	private final int ID;
	private String name;
	private final double MaxHP;
	private double HP;

	public Entity(String name, double MaxHP) {
		this.ID = nextID++;
		this.name = name;
		this.MaxHP = MaxHP;
		this.HP = MaxHP;
	}
	
	public int getID() {
		return ID;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void takeDamage(double amount) {
		HP -= amount;
		if(HP <= 0) {
			HP = 0;
		}
	}
	
	public void RestoreHP(double amount) {
		HP += amount;
		if(HP > MaxHP) {
			HP = MaxHP;
		}
	}
	
	public boolean isAlive() {
		return HP > 0;
	}
}
