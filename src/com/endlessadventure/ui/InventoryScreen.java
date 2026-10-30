package com.endlessadventure.ui;

import java.util.List;
import java.util.Map;

import com.endlessadventure.Displayable.Detail;
import com.endlessadventure.entity.Player;
import com.endlessadventure.item.Equipment;
import com.endlessadventure.item.Item;

public final class InventoryScreen extends Screen {

	private static final ContainerGrid GRID = new ContainerGrid(4, 4);

	//same frame as character creation: three columns, the details column leaves room for the CLOSE button under it
	private static final int X = 3, Y = 4, H = 44, DETAIL_H = 40;
	private static final int CHAR_W = CharacterPanel.WIDTH, GEAR_W = 77, SPACING = 2;
	
	private static final int GEAR_X = X + CHAR_W + SPACING,
							 DETAIL_X = GEAR_X + GEAR_W + SPACING,
						 	 DETAIL_W = Canvas.WIDTH - 3 - DETAIL_X; //54

	private final Player player;
	private Area area = Area.BACKPACK;
	private int gridFocus = -1; //focused box in the backpack, -1 for none
	private Equipment.Slot slotFocus = Equipment.Slot.MAIN_HAND;
	
	/** the backpack grid and the worn slots are two areas; TAB switches between them, the mouse moves to whichever it points at */
	private enum Area { BACKPACK, WORN }
	public enum Action { CLOSE, CHARACTER }

	public InventoryScreen(UIHandler ui, Player player) {
		super(ui);
		this.player = player;
	}
	
	/** shows the inventory until the player closes it; equipping and unequipping change the player at once */
	public Action run() {
		while (true) {
			drawFrame();
			UIHandler.Input in = ui.readInput();
			List<Item> items = player.getInventory();
			switch (in.inputKey()) {
				case ESC -> {return Action.CLOSE;}
				case TAB, BACK_TAB -> {
					area = area == Area.BACKPACK ? Area.WORN : Area.BACKPACK;
					if (area == Area.BACKPACK && gridFocus < 0 && !items.isEmpty()) {
						gridFocus = 0;
					}
				}
				case UP, DOWN, LEFT, RIGHT -> {
					if (area == Area.BACKPACK) {
						gridFocus = GRID.moveFocus(in.inputKey(), gridFocus, items.size());
					}
					else { //the slots are one row: left and right only, stopping at the ends
						Equipment.Slot[] slots = Equipment.Slot.values();
						int i = slotFocus.ordinal() + (in.inputKey() == UIHandler.InputKey.LEFT ? -1
								: in.inputKey() == UIHandler.InputKey.RIGHT ? 1 : 0);
						slotFocus = slots[Math.clamp(i, 0, slots.length - 1)];
					}
				}
				case HOVER -> { //pointing at nothing keeps the focus, so the details stay up
					if (in.button() instanceof Button b && b.action() instanceof Equipment.Slot s) {
						area = Area.WORN;
						slotFocus = s;
					}
					else if (in.button() instanceof Button b && b.action() == null) {
						area = Area.BACKPACK;
						gridFocus = b.index();
					}
				}
				case CHAR -> {
					switch(Character.toLowerCase(in.ch())) {
						case 'c' -> {return Action.CHARACTER;}
						case 'i' -> {return Action.CLOSE;}
						case 'e' -> performItemOperation(items);
					}
				}
				case CLICK -> {
					if (in.button() instanceof Button b && b.action() instanceof Action a) { //CLOSE or the CHARACTER tab
						return a;
					}
					if (in.button() != null) { //a box or a slot: the hover already moved the focus there
						performItemOperation(items);
					}
				}
				default -> {}
			}
		}
	}
	
	/** equips the focused backpack item, or puts the focused worn piece back in the backpack */
	private void performItemOperation(List<Item> items) {
		if (area == Area.WORN) {
			if (!player.unequipToInventory(slotFocus)) {
				ui.showWarning("Nothing worn there.");
			}
			return;
		}
		if (gridFocus < 0 || gridFocus >= items.size()) {
			return;
		}
		if (items.get(gridFocus) instanceof Equipment piece) {
			player.equipFromInventory(piece);
			gridFocus = Math.min(gridFocus, player.getInventory().size() - 1); //the backpack may have shrunk
		}
		else {
			ui.showWarning("This item cannot be equipped.");
		}
	}
	
	/** a stand-in with the same stats and gear, so a piece can be tried on without touching the real player */
	private Player copyWithGear() {
		Player p = new Player(player.getName(), player.getLevel(), player.getHp(), player.getExp(),
				player.getAttributes(), player.getSkills());
		player.getEquippedAll().forEach((slot, piece) -> p.equip(piece, slot));
		return p;
	}
	
	private void drawFrame() {
		beginFrame();
		drawTabs(new String[] {"CHARACTER", "INVENTORY"}, new Enum<?>[] {Action.CHARACTER, null}, 1);
		List<Item> items = player.getInventory();

		CharacterPanel.draw(canvas, ui, X, Y, CHAR_W, H, player, 
				s -> area == Area.WORN && s == slotFocus ? Palette.KEY : Palette.LINE);

		//draw the backpack: count, the grid, a note when empty
		canvas.drawTitledBox(GEAR_X, Y, GEAR_W, H, Palette.MUTED, "BACKPACK");
		canvas.drawText(GEAR_X + 3, Y + 2, items.size() + (items.size() == 1 ? " item" : " items"), Palette.MUTED);
		GRID.draw(canvas, ui, GEAR_X + (GEAR_W - GRID.width()) / 2, Y + 4, items,
				area == Area.BACKPACK ? gridFocus : -1, i -> false);
		canvas.drawText(GEAR_X + 3, Y + H - 2, items.isEmpty() ? "Your backpack is empty."
				: "Click to equip; click a worn piece to unequip.", Palette.MUTED);

		//draw details of the focused item; a backpack piece also shows what equipping it would change
		Item shown = area == Area.WORN ? player.getEquipped(slotFocus)
				: gridFocus >= 0 && gridFocus < items.size() ? items.get(gridFocus) : null;
		List<Detail> comparison = null;
		if (area == Area.BACKPACK && shown instanceof Equipment piece) {
			Player after = copyWithGear();
			comparison = ItemDetails.compare(copyWithGear(), after, after.equip(piece));
		}
		ItemDetails.draw(canvas, DETAIL_X, Y, DETAIL_W, DETAIL_H, shown, comparison);
		ui.drawButton(DETAIL_X + DETAIL_W - Canvas.BUTTON_WIDTH, Y + DETAIL_H + 1, "CLOSE", true, new Button(Action.CLOSE));

		endFrame(List.of(
				Map.entry("↑↓←→", "Move"),
				Map.entry("TAB / SHIFT TAB", "Backpack / Worn"),
				Map.entry("E", "Equip / Unequip"),
				Map.entry("C", "Character Details"),
				Map.entry("ESC", "Close")));
	}
}
