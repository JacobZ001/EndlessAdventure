package com.endlessadventure.item;

import java.util.List;
import java.util.Objects;

import com.endlessadventure.Displayable;

/** base class for all Item types */
public abstract class Item implements Displayable {
	private final String id, name, description;

	protected Item(String id, String name, String description) {
		if (id == null || id.isBlank() || name == null || name.isBlank() || description == null
				|| description.isBlank()) {
			throw new IllegalArgumentException("Illegal item: " + id);
		}
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public abstract String getCategoryLabel();

	/** returns full details on stats */
	@Override
	public abstract List<Detail> getDetails();

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Item other = (Item) obj;
		return Objects.equals(id, other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return name;
	}
}
