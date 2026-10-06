package com.endlessadventure;

public abstract class Item {
	private static int nextId = 1;
	private final int id;
	private final String name;
	private final String description;

	public Item(String name, String description) {
		id = nextId++;
		this.name = name;
		this.description = description;
	}
	
	public Item(String name) {
		this(name, "");
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
	
	@Override
	public abstract String toString();

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);	
}
