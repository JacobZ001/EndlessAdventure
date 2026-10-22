package com.endlessadventure.ui;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.endlessadventure.Displayable.Detail;
import com.endlessadventure.data.EquipmentCatalog;
import com.endlessadventure.entity.Player;
import com.endlessadventure.item.Equipment;

/** the starting-gear step of character creation: the character column, a grid of the common gear with one tab per slot,
 *  and the item details with a comparison. the creation screen keeps the step bar, the action bar and ENTER / ESC */
final class StartingGearPanel {
	private static final ContainerGrid GRID = new ContainerGrid(5, 4);
	private static final int CHAR_W = 52, GEAR_W = 93, SPACING = 2; //the details column takes the rest

	private final UIHandler ui;
	private final Canvas canvas;
	private final Supplier<Player> base; //a fresh player from the spent points, without gear
	private final Map<Equipment.Slot, List<Equipment>> choices = new EnumMap<>(Equipment.Slot.class);
	private final Map<Equipment.Slot, Equipment> picked = new EnumMap<>(Equipment.Slot.class);

	private Equipment.Slot focusedTab = Equipment.Slot.MAIN_HAND;
	private Equipment.Slot hoveredTab = null;
	private int gearFocus = -1; //focused cell in the grid, -1 for none

	StartingGearPanel(UIHandler ui, EquipmentCatalog catalog, Supplier<Player> base) {
		this.ui = ui;
		this.canvas = ui.getCanvas();
		this.base = base;

		//for each slot, the common catalog items that fit it, in catalog order
		for (Equipment.Slot slot : Equipment.Slot.values()) {
			choices.put(slot, catalog.getAll().stream()
					.filter(e -> e.getQuality() == Equipment.Quality.COMMON && e.fits(slot))
					.toList());
		}
	}

	/** a weapon and armor are on */
	boolean isComplete() {
		return picked.containsKey(Equipment.Slot.MAIN_HAND) && picked.containsKey(Equipment.Slot.ARMOR);
	}

	/** the player from the spent points with the picked gear on */
	Player buildPlayer() {
		Player p = base.get();
		picked.forEach((s, e) -> p.equip(e, s));
		return p;
	}

	/** the step's own input: moving in the grid, hovering, switching tabs, equipping;
	 *  returns false for the inputs the creation screen handles (ENTER, ESC, BACK, NEXT) */
	boolean handleInput(UIHandler.Input in) {
		switch (in.inputKey()) {
			case UP, DOWN, LEFT, RIGHT -> gearFocus = GRID.moveFocus(in.inputKey(), gearFocus, choices.get(focusedTab).size());
			case HOVER -> {
				gearFocus = in.button() instanceof Button b && b.action() == null ? b.index() : -1;
				hoveredTab = in.button() instanceof Button b && b.action() instanceof Equipment.Slot s ? s : null;
			}
			case TAB, BACK_TAB -> {
				Equipment.Slot[] slots = Equipment.Slot.values();
				int delta = in.inputKey() == UIHandler.InputKey.TAB ? 1 : -1;
				focusedTab = slots[Math.floorMod(focusedTab.ordinal() + delta, slots.length)];
				gearFocus = -1;
			}
			case CHAR -> {
				if (Character.toLowerCase(in.ch()) != 'e') {
					return false;
				}
				toggleGear(gearFocus);
			}
			case CLICK -> {
				if (in.button() instanceof Button b && b.action() == null) {
					toggleGear(b.index()); //a cell
				}
				else if (in.button() instanceof Button b && b.action() instanceof Equipment.Slot s) {
					focusedTab = s; //a tab or a slot
					gearFocus = -1;
				}
				else {
					return false;
				}
			}
			default -> {
				return false;
			}
		}
		return true;
	}

	/** wears the item at index of the open tab in that tab's slot, or takes it off when it is already there */
	private void toggleGear(int index) {
		List<Equipment> items = choices.get(focusedTab);
		if (index < 0 || index >= items.size()) {
			return;
		}
		Equipment piece = items.get(index);
		if (piece.equals(picked.get(focusedTab))) {
			picked.remove(focusedTab);
		}
		else {
			pick(piece, focusedTab);
		}
	}

	/** wears the piece on a preview player and keeps what that player ends up wearing,
	 *  so picking goes through the real equip rules and picked only holds legal combinations */
	private void pick(Equipment piece, Equipment.Slot slot) {
		Player preview = buildPlayer();
		preview.equip(piece, slot);
		picked.clear();
		picked.putAll(preview.getEquippedAll());
	}

	/** draws the three columns in the area; the details column is detailHeight tall, leaving room for the action bar */
	void draw(int x, int y, int width, int height, int detailHeight) {
		int gearX = x + CHAR_W + SPACING;
		int detailX = gearX + GEAR_W + SPACING;

		CharacterPanel.draw(canvas, ui, x, y, CHAR_W, height, buildPlayer(), 
				s -> s == focusedTab? Palette.TAG : s == hoveredTab ? Palette.TEXT : Palette.LINE);

		//draw the gear column: tabs, the grid, the instruction
		canvas.drawTitledBox(gearX, y, GEAR_W, height, Palette.MUTED, "STARTING GEAR");
		int tx = gearX + 3;
		for (Equipment.Slot s : Equipment.Slot.values()) {
			String label = s.label.toUpperCase() + " " + choices.get(s).size();
			canvas.drawText(tx, y + 2, label, s == focusedTab ? Palette.KEY : (s == hoveredTab ? Palette.TEXT : Palette.MUTED));
			if (s == focusedTab) {
				canvas.drawText(tx, y + 3, "━".repeat(label.length()), Palette.KEY);
			}
			else if (s == hoveredTab) {
				canvas.drawText(tx, y + 3, "━".repeat(label.length()), Palette.TEXT);
			}
			ui.addButtonBox(tx, y + 2, label.length(), 1, new Button(s));
			tx += label.length() + 6;
		}
		List<Equipment> items = choices.get(focusedTab);
		GRID.draw(canvas, ui, gearX + (GEAR_W - GRID.width()) / 2, y + 5, items, gearFocus, e -> e.equals(picked.get(focusedTab)));
		String instruction = items.size() + " items. Click an item to equip it. You need a weapon and armor.";
		canvas.drawText(gearX + 3, y + height - 2, instruction, Palette.MUTED);

		//draw details of the focused piece, or of the one worn in this tab's slot; compare unless it is already worn there
		Equipment shown = gearFocus >= 0 && gearFocus < items.size() ? items.get(gearFocus) : picked.get(focusedTab);
		List<Detail> comparison = null;
		if (shown != null && !shown.equals(picked.get(focusedTab))) {
			Player before = buildPlayer();
			Player after = buildPlayer();
			comparison = ItemDetails.compare(before, after, after.equip(shown, focusedTab));
		}
		ItemDetails.draw(canvas, detailX, y, x + width - detailX, detailHeight, shown, comparison);
	}
}
