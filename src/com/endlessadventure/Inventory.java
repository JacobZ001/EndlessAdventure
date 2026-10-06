package com.endlessadventure;

import java.util.ArrayList;
import java.util.List;

public class Inventory {
	private final List<Item> items = new ArrayList<>();
	
	public void addItem(Item item) {
		if(item == null) {
			throw new IllegalArgumentException("Item cannot be null");
		}
		items.add(item);
	}
	
	public Item getItem(int index) {
		return items.get(index);
	}
	
	public Item removeItem(int index) {
		return items.remove(index);
	}
	
	public int getSize() {
		return items.size();
	}
}
