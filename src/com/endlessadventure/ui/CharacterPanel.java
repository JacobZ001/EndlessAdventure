package com.endlessadventure.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.endlessadventure.entity.Attribute;
import com.endlessadventure.entity.GameCharacter;
import com.endlessadventure.entity.Skill;
import com.endlessadventure.item.Equipment;

/** the character column of the gear screens (starting gear now, the inventory later): name, level and HP,
 *  attributes and combat stats, skills, and the equipment slots, each clickable as a Button carrying its Equipment.Slot */
final class CharacterPanel {
	private CharacterPanel() {}

	/** draws the framed column at x, y; slotColor gives each equipment slot's frame color, so each screen applies its own focus rules */
	static void draw(Canvas canvas, UIHandler ui, int x, int y, int width, int height, GameCharacter c,
			Function<Equipment.Slot, Integer> slotColor) {
		canvas.drawTitledBox(x, y, width, height, Palette.MUTED, "CHARACTER");
		int cx = x + 3, cw = width - 6;
		int tileW = (cw - 4) / 3; //three columns two apart, shared by the stat tiles and the equipment slots
		int[] cols = {cx, cx + tileW + 2, cx + 2 * (tileW + 2)};

		//name and level, then the HP bar
		String level = "Level " + c.getLevel();
		canvas.drawText(cx, y + 2, c.getName(), Palette.KEY);
		canvas.drawText(cx + cw - level.length(), y + 2, level, Palette.TEXT);

		String hp = c.getHp() + " / " + c.getMaxHp();
		int barY = y + 4;
		int barW = cw - 4 - hp.length(); //"HP " before the bar, one space after it
		int filled = barW * c.getHp() / c.getMaxHp();
		int hpColor = Palette.colorOf(Attribute.CON);
		canvas.drawText(cx, barY, "HP", Palette.MUTED);
		canvas.drawText(cx + 3, barY, "█".repeat(filled), hpColor);
		canvas.drawText(cx + 3 + filled, barY, "░".repeat(barW - filled), Palette.LINE);
		canvas.drawText(cx + cw - hp.length(), barY, hp, hpColor);

		//attributes: abbreviation above, value below, spread across the width
		int ay = barY + 2;
		for (int i = 0; i < Attribute.values().length; i++) {
			Attribute a = Attribute.values()[i];
			int ax = cx + i * (cw - 3) / 6; //the last one ends at the right edge
			canvas.drawText(ax, ay, a.name(), Palette.colorOf(a));
			canvas.drawText(ax + 1, ay + 1, String.valueOf(c.getAttribute(a)), Palette.TEXT);
		}

		//derived combat stats as small tiles, three to a row
		List<Map.Entry<String, String>> stats = List.of(
				Map.entry("ATTACK", String.valueOf(c.getAttackPower())),
				Map.entry("OFF-HAND", c.getOffHandAttackPower() > 0 ? String.valueOf(c.getOffHandAttackPower()) : "–"),
				Map.entry("SPEED", String.valueOf(c.getSpeed())),
				Map.entry("ARMOR", String.valueOf(c.getArmor())),
				Map.entry("GUARD", String.valueOf(c.getShieldGuard())),
				Map.entry("DODGE", c.canDodge() ? "Enabled" : "Disabled"));
		for (int i = 0; i < stats.size(); i++) {
			int tx = cols[i % 3],
				ty = ay + 3 + (i / 3) * 3;
			String value = stats.get(i).getValue();
			canvas.drawBox(tx, ty, tileW, 3, Palette.LINE);
			canvas.drawText(tx + 2, ty, " " + stats.get(i).getKey() + " ", Palette.MUTED);
			canvas.drawText(tx + (tileW - value.length()) / 2, ty + 1, value, Palette.TEXT);
		}

		//skills with points in two columns, filled top to bottom: name on the left, value on the right
		canvas.drawText(cx, y + 16, "SKILLS", Palette.TAG);
		List<Skill> trained = Arrays.stream(Skill.values()).filter(s -> c.getSkill(s) > 0).toList();
		int rowsPerCol = (trained.size() + 1) / 2;
		int skillW = Math.min((cw - 2) / 2, 22); //capped so a value stays close to its name in a wide panel
		for (int i = 0; i < trained.size(); i++) {
			Skill s = trained.get(i);
			int sx = cx + (i / rowsPerCol) * (skillW + 2), sy = y + 17 + i % rowsPerCol;
			String value = String.valueOf(c.getSkill(s));
			canvas.drawText(sx, sy, s.displayName, Palette.TEXT);
			canvas.drawText(sx + skillW - value.length(), sy, value, Palette.TEXT);
		}

		//equipment slots on the same three columns as the tiles
		canvas.drawText(cx, y + 26, "EQUIPMENT", Palette.TAG);
		Equipment.Slot[] slots = Equipment.Slot.values();
		for (int i = 0; i < slots.length; i++) {
			int sx = cols[i] + (tileW - ContainerGrid.ICON_W - 2) / 2; //centered under its tile column
			drawSlot(canvas, ui, slots[i], sx, y + 28, c.getEquipped(slots[i]), slotColor);
		}
	}

	/** one equipment slot: its name above, the icon of what it holds in a frame the size of a grid cell */
	private static void drawSlot(Canvas canvas, UIHandler ui, Equipment.Slot slot, int x, int y, Equipment piece,
			Function<Equipment.Slot, Integer> slotColor) {
		int w = ContainerGrid.ICON_W + 2, h = ContainerGrid.ICON_H + 2;
		int color = slotColor.apply(slot);
		canvas.drawText(x, y, slot.label.toUpperCase(), color == Palette.LINE ? Palette.MUTED : color);
		canvas.drawBox(x, y + 1, w, h, color);
		if (piece != null) {
			ContainerGrid.drawIcon(canvas, x + 1, y + 2, piece, 1);
		}
		ui.addButtonBox(x, y, w, h + 1, new Button(slot)); //the name and the frame
	}
}
