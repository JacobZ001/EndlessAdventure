package com.endlessadventure;

import java.util.*;

public abstract class Entity {
	private static int nextID = 1;
	private final int ID;
	private final double MaxHP;
	private double HP;
	private boolean isActive = true; //turns inactive when HP reaches 0

	public Entity(double MaxHP) {
		this.ID = nextID++;
		this.MaxHP = MaxHP;
		this.HP = MaxHP;
	}
	
	public int getID() {
		return ID;
	}
	
	//process damage taking and return true if the entity is still alive after taking damage
	public boolean takeDamage(double amount) {
		HP -= amount;
		if(HP <= 0) {
			HP = 0;
			isActive = false;
		}
		return isActive;
	}
	
	public void RestoreHP(double amount) {
		HP += amount;
		if(HP > MaxHP) {
			HP = MaxHP;
		}
	}
	
	public boolean getActive() {
		return isActive;
	}
	
	public abstract String getType();
}
