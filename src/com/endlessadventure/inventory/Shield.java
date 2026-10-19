package com.endlessadventure.inventory;

import java.util.List;

public final class Shield extends Equipment {
	private static final int SHIELD_GUARD_PER_TIER = 2;

	public Shield(String id, String name, String description, Quality quality) {
		super(id, name, description, quality);
	}
	
	@Override
	public int getGuard() {
		return getQuality().getTier() * SHIELD_GUARD_PER_TIER;
	}

	@Override
	public boolean fits(Slot slot) {
		return slot == Slot.OFF_HAND;
	}

	@Override
	public String getCategoryLabel() {
		return "Shield";
	}

	@Override
	protected void addStats(List<Detail> details) {
		details.add(Detail.Stat.of("Guard", "+" + getGuard()));
	}
}
