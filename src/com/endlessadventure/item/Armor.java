package com.endlessadventure.item;

import java.util.List;

import com.endlessadventure.Displayable;

public final class Armor extends Equipment {
	public static final int HEAVY_ARMOR_SPEED_PENALTY = 1;
	
	private final Type type;

	public Armor(String id, String name, String description, Quality quality, Type type) {
		super(id, name, description, quality);
		if(type == null) {
			throw new IllegalArgumentException("Illegal armor: type cannot be null.");
		}
		this.type = type;
	}

	@Override
	public int getArmor() {
		return getQuality().tier * type.base;
	}
	
	@Override
	public boolean fits(Slot slot) {
		return slot == Slot.ARMOR;
	}

	@Override
	public String getCategoryLabel() {
		return type + " Armor";
	}

	@Override
	public int getSpeedPenalty() {
		return type == Type.HEAVY? HEAVY_ARMOR_SPEED_PENALTY : 0;
	}

	@Override
	public boolean allowsDodge() {
		return type == Type.LIGHT;
	}

	@Override
	protected void addStats(List<Detail> details) {
		details.add(Detail.Stat.of("Armor", getArmor()));
		if (getSpeedPenalty() > 0) {
			details.add(Detail.Stat.of("Speed", "-" + getSpeedPenalty(), Tone.BAD));
		}
		details.add(allowsDodge() ? Detail.Line.of("Allows dodging", Tone.GOOD) : Detail.Line.of("Cannot dodge", Tone.BAD));
	}
	
	public enum Type {
		LIGHT(2),
		MEDIUM(4),
		HEAVY(6);

		public final int base;

		Type(int base) {
			this.base = base;
		}

		@Override
		public String toString() {
			return Displayable.titleCase(name());
		}
		
	}
}
