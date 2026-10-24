package com.endlessadventure.ui;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import com.endlessadventure.StatRules;
import com.endlessadventure.entity.Attribute;
import com.endlessadventure.entity.Player;
import com.endlessadventure.entity.Skill;
import com.endlessadventure.item.Equipment;
import com.endlessadventure.item.Shield;
import com.endlessadventure.item.Weapon;

/** the character column shared by the gear screens (starting gear, inventory) and the character screen: name, level and HP,
 *  attributes and combat skills, then a paper doll: the portrait between the equipment slots, each slot clickable as a Button
 *  carrying its Equipment.Slot and showing on its bottom border what it adds in combat. CharacterDetails shows the rest */
final class CharacterPanel {
	static final int WIDTH = 68; //two slot columns with the portrait between them

	//where each slot sits around the portrait: {0 left / 1 right column, row 0-2}; the free places are kept for later slots
	private static final Map<Equipment.Slot, int[]> PLACES = Map.of(
			Equipment.Slot.MAIN_HAND, new int[] {0, 0},
			Equipment.Slot.ARMOR, new int[] {0, 1},
			Equipment.Slot.OFF_HAND, new int[] {1, 0});
	private static final int DOLL_Y = 13, SLOT_STEP = ContainerGrid.BOX_H + 2; //first slot row below the top; one slot to the next

	private CharacterPanel() {}

	/** draws the framed column at x, y; slotColor gives each equipment slot's frame color, so each screen applies its own focus rules */
	static void draw(Canvas canvas, UIHandler ui, int x, int y, int width, int height, Player p,
			Function<Equipment.Slot, Integer> slotColor) {
		canvas.drawTitledBox(x, y, width, height, Palette.MUTED, "CHARACTER");
		int cx = x + 3, cw = width - 6;
		int colW = (cw - 4) / 3; //three columns two apart
		int[] cols = {cx, cx + colW + 2, cx + 2 * (colW + 2)};

		//name, level, exp, then the HP bar
		String lvlLabel = "Level " + p.getLevel();
		String expLabel = p.getLevel() == StatRules.MAX_LEVEL? "(Max)" : "(%d / %d)".formatted(p.getExp(), StatRules.getXpToNext(p.getLevel()));
		int lvl_x = cx + cw - lvlLabel.length() - expLabel.length() - 1;
		int exp_x = cx + cw - expLabel.length();
		canvas.drawText(cx, y + 2, p.getName(), Palette.KEY);
		canvas.drawText(lvl_x, y + 2, lvlLabel, Palette.TEXT);
		canvas.drawText(exp_x, y + 2, expLabel, Palette.MUTED);

		String hp = p.getHp() + " / " + p.getMaxHp();
		int barY = y + 4;
		int barW = cw - 4 - hp.length(); //"HP " before the bar, one space after it
		int filled = barW * p.getHp() / p.getMaxHp();
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
			canvas.drawText(ax + 1, ay + 1, String.valueOf(p.getAttribute(a)), Palette.TEXT);
		}

		//combat skills as tiles, one per column: the name centered on the top border, the value centered inside, like the slots;
		//general skills are in the details
		Skill[] combat = Arrays.stream(Skill.values()).filter(s -> s.group == Skill.Group.COMBAT).toArray(Skill[]::new);
		for (int i = 0; i < combat.length; i++) {
			String name = " " + combat[i].displayName.toUpperCase() + " ", value = String.valueOf(p.getSkill(combat[i]));
			canvas.drawBox(cols[i], ay + 3, colW, 3, Palette.LINE);
			canvas.drawText(cols[i] + (colW - name.length()) / 2, ay + 3, name, Palette.TAG);
			canvas.drawText(cols[i] + (colW - value.length()) / 2, ay + 4, value, Palette.TEXT);
		}

		//the paper doll: the portrait standing between the two slot columns, its feet on the panel's last row
		int leftX = x + 3, rightX = x + width - 3 - ContainerGrid.BOX_W;
		int gapX = leftX + ContainerGrid.BOX_W, gapW = rightX - gapX;
		PixelImage.portrait("player").ifPresent(art -> //TODO the portrait id will come from the player
				canvas.drawImage(gapX + (gapW - art.getWidth()) / 2, (y + height - 2) * 2 + 2 - art.getHeight(), art));

		//a two-handed weapon fills the off hand too, so its slot shows the weapon
		Equipment mainHand = p.getEquipped(Equipment.Slot.MAIN_HAND);
		boolean twoHanded = mainHand instanceof Weapon w && w.getType().grip == Weapon.Grip.TWO_HANDED;
		for (Equipment.Slot slot : Equipment.Slot.values()) {
			int[] place = PLACES.get(slot);
			Equipment piece = slot == Equipment.Slot.OFF_HAND && twoHanded ? mainHand : p.getEquipped(slot);
			String stat = switch (slot) {
				case MAIN_HAND -> "ATK " + p.getMainAttackPower();
				case ARMOR -> "ARMOR " + p.getArmor();
				case OFF_HAND -> twoHanded ? "TWO-HANDED"
						: piece instanceof Shield ? "GUARD " + p.getShieldGuard()
						: piece != null ? "ATK " + p.getOffHandAttackPower() : "";
			};
			drawSlot(canvas, ui, slot, place[0] == 0 ? leftX : rightX, y + DOLL_Y + place[1] * SLOT_STEP, piece, stat,
					slotColor.apply(slot));
		}
	}

	/** one slot, a frame the size of a grid box: its name centered on the top border, the icon of what it holds,
	 *  and what it adds in combat centered on the bottom border */
	private static void drawSlot(Canvas canvas, UIHandler ui, Equipment.Slot slot, int x, int y, Equipment piece, String stat,
			int color) {
		int w = ContainerGrid.BOX_W, h = ContainerGrid.BOX_H;
		canvas.drawBox(x, y, w, h, color);
		String name = " " + slot.label.toUpperCase() + " ";
		canvas.drawText(x + (w - name.length()) / 2, y, name, color == Palette.LINE ? Palette.MUTED : color);
		if (piece != null) {
			ContainerGrid.drawIcon(canvas, x + 1, y + 1, piece, 1);
		}
		if (!stat.isEmpty()) {
			String text = " " + stat + " ";
			canvas.drawText(x + (w - text.length()) / 2, y + h - 1, text, stat.equals("TWO-HANDED") ? Palette.MUTED : Palette.TEXT);
		}
		ui.addButtonBox(x, y, w, h, new Button(slot));
	}
}
