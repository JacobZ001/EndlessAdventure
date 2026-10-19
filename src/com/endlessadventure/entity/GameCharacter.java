package com.endlessadventure.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.endlessadventure.inventory.Item;

public abstract class GameCharacter extends Entity {

	private final EnumMap<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
	private final EnumMap<Skill, Integer> skills = new EnumMap<>(Skill.class);
	private final List<Item> inventory = new ArrayList<>();

	public GameCharacter(String name, int level) {
		super(name, level);
	}

	public int getAttribute(Attribute a) {
		return attributes.getOrDefault(a, 0);
	}

	/** upper limits depend on the kind of character and are checked by the subclass
	 *  @throws IllegalArgumentException */
	protected void setAttribute(Attribute a, int v) {
		if(v < 0) {
			throw new IllegalArgumentException("Invalid attribute value: " + a.name() + " : " + v);
		}
		attributes.put(a, v);
	}

	public int getSkill(Skill s) {
		return skills.getOrDefault(s, 0);
	}

	/** upper limits depend on the kind of character and are checked by the subclass
	 *  @throws IllegalArgumentException */
	protected void setSkill(Skill s, int v) {
		if(v < 0) {
			throw new IllegalArgumentException("Invalid skill value: " + s.name() + " : " + v);
		}
		skills.put(s, v);
	}
	
	/** returns a read-only list of items */
	public List<Item> getInventory(){
		return List.copyOf(inventory);
	}
	
	public void addItem(Item item) {
		if(item == null) {
			throw new IllegalArgumentException("Invalid item: item cannot be null.");
		}
		inventory.add(item);
	}
	
	public boolean removeItem(Item item) {
		return inventory.remove(item);
	}
}
