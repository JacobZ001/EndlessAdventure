package com.endlessadventure.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.endlessadventure.Displayable.Detail;
import com.endlessadventure.Displayable.Tone;
import com.endlessadventure.entity.GameCharacter;
import com.endlessadventure.item.Equipment;
import com.endlessadventure.item.Item;

/** the item details column of the gear screens (starting gear now, the inventory later): the item's large icon and details,
 *  and at the bottom what equipping it would change */
final class ItemDetails {
	private ItemDetails() {}

	/** draws the framed column; item may be null for an empty column and comparison null for none.
	 *  the comparison sits at the bottom so it stays in place while the focus moves between items */
	static void draw(Canvas canvas, int x, int y, int width, int height, Item item, List<Detail> comparison) {
		canvas.drawTitledBox(x, y, width, height, Palette.MUTED, "DETAILS");
		if (item == null) {
			return;
		}
		int dx = x + 3,
			dw = width - 6;

		//the icon at twice the cell size, framed like a cell
		int iw = ContainerGrid.ICON_W * 2, ix = x + (width - iw) / 2;
		canvas.drawBox(ix - 1, y + 1, iw + 2, ContainerGrid.ICON_H * 2 + 2, Palette.LINE);
		ContainerGrid.drawIcon(canvas, ix, y + 2, item, 2);

		//draw details under the icon
		DetailView.draw(canvas, dx, y + 16, dw, item.getDetails());

		//draw the comparison: its last row leaves one empty row above the bottom border, with a divider an empty row above it
		if (comparison != null) {
			int h = DetailView.height(dw, comparison);
			int cy = y + height - 2 - h;
			canvas.drawText(dx, cy - 2, "─".repeat(dw), Palette.LINE);
			DetailView.draw(canvas, dx, cy, dw, comparison);
		}
	}

	/** what changes from before to after a piece goes on: the derived stats that differ, colored better or worse,
	 *  and what had to come off (the list equip returned) */
	static List<Detail> compare(GameCharacter before, GameCharacter after, List<Equipment> removed) {
		List<Detail> lines = new ArrayList<>();
		lines.add(Detail.Line.of("IF EQUIPPED", Tone.KEYWORD));
		addChange(lines, "Attack", before.getMainAttackPower(), after.getMainAttackPower());
		addChange(lines, "Off-hand Attack", before.getOffHandAttackPower(), after.getOffHandAttackPower());
		addChange(lines, "Armor", before.getArmor(), after.getArmor());
		addChange(lines, "Guard", before.getShieldGuard(), after.getShieldGuard());
		addChange(lines, "Speed", before.getSpeed(), after.getSpeed());
		if (before.canDodge() != after.canDodge()) {
			lines.add(Detail.Stat.of("Dodge", after.canDodge() ? "Enabled" : "Disabled", after.canDodge() ? Tone.GOOD : Tone.BAD));
		}
		if (lines.size() == 1) {
			lines.add(Detail.Line.of("No change", Tone.NOTE));
		}
		if (!removed.isEmpty()) {
			lines.add(Detail.Line.of("Replaces " + removed
					.stream()
					.map(Equipment::getName)
					.collect(Collectors.joining(" and ")), Tone.NOTE));
		}
		return lines;
	}

	/** adds e.g. "Attack 14 (+3)" in green, or in red when it drops; nothing when it stays the same */
	private static void addChange(List<Detail> lines, String label, int before, int after) {
		if (before != after) {
			int diff = after - before;
			lines.add(Detail.Stat.of(label, after + " (" + (diff > 0 ? "+" : "") + diff + ")", diff > 0 ? Tone.GOOD : Tone.BAD));
		}
	}
}
