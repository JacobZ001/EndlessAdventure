package com.endlessadventure.ui;

import java.util.List;
import java.util.Map;

import com.endlessadventure.save.SaveManager;
import com.endlessadventure.save.SaveManager.SlotOverview;
import com.endlessadventure.save.SaveManager.SlotStatus;

public final class SaveScreen extends Screen {
	private static final int START_Y = 6,
							 CARD_W = 96, CARD_H = 7, CARD_STEP = CARD_H + 2, CARD_P = 4,
							 CARD_X = (Canvas.WIDTH - CARD_W) / 2,
							 CARD_Y = START_Y + 3,
							 
							 BUTTON_SPACING = 2,
							 BUTTON_Y = CARD_Y + SaveManager.SLOT_COUNT * CARD_STEP + 2,
							 BUTTON_X = CARD_X + CARD_W - 3 * Canvas.BUTTON_WIDTH - 2 * BUTTON_SPACING;
	
	
	public enum Mode { LOAD, SAVE }
	public enum Action {CONFIRM, DELETE, BACK }
	public record Choice(Action action, int slot) {}

	private final Mode mode;
	private final SlotOverview[] slots;
	private int selected;
	
	public SaveScreen(UIHandler ui, Mode mode, SaveManager.SlotOverview[] slots, int selectedSlot) {
		super(ui, mode.toString() + " GAME");
		this.mode = mode;
		this.slots = slots;
		selected = selectedSlot - 1; //1-indexing to 0-indexing
	}

	public Choice run() {
		while(true) {
			drawFrame();
			UIHandler.Input in = ui.readInput();
			Action action = null;
			switch(in.inputKey()) {
				case UP -> selected = Math.floorMod(selected - 1, slots.length);
				case DOWN -> selected = Math.floorMod(selected + 1, slots.length);
				case ENTER -> action = Action.CONFIRM;
				case ESC -> action = Action.BACK;
				case DEL -> action = Action.DELETE;
				case CHAR -> {
					int digit = in.digit();
					if (digit >= 0 && digit < slots.length) {
						selected = digit;
					}
				}
				case CLICK -> {
					UIHandler.Button b = in.button();
					if (b != null){
						if(b.index() >= SaveManager.SLOT_COUNT) {
							action = Action.values()[b.index() - SaveManager.SLOT_COUNT];
						}
						else {
							selected = b.index();
						}
					}
				}
			default -> {}
			}
			if(action != null) {
				Choice choice = act(action);
				if (choice != null) {
					return choice;
				}
			}
		}
	}
	
	private Choice act(Action a) {
		SlotOverview slot = slots[selected];
		return switch(a) {
			case CONFIRM -> {
				if(canConfirm()) {
					boolean confirmed = true;
					if(mode == Mode.SAVE && slot.status() != SlotStatus.EMPTY) {
						confirmed = ConfirmDialog.show(ui,
								"OVERWRITE SAVE",
								describe("Overwrite", slot),
								"This cannot be undone.",
								"OVERWRITE");
					}
					yield confirmed? new Choice(Action.CONFIRM, slot.slot()) : null;
				}
				else {
					String warning = (slots[selected].status() == SlotStatus.EMPTY)? "This slot is empty" : "This save cannot be read";
					ui.showWarning(warning);
					yield null;
				}
			}
			case DELETE -> {
				if(canDelete()) {
					yield ConfirmDialog.show(ui, 
							"DELETE SAVE",
							describe("Delete", slot),
							"This cannot be undone.",
							"DELETE")? new Choice(Action.DELETE, slot.slot()) : null;
				}
				else {
					ui.showWarning("This slot is already empty");
					yield null;
				}
			}
			case BACK -> new Choice(a, slot.slot());
		};
	}
	
	private void drawFrame() {
		beginFrame();
		
		int y = START_Y;
		canvas.drawTextCentered(y, "Choose a slot to " + mode.toString().toLowerCase(), Palette.MUTED);
		
		//draw slot cards
		y = CARD_Y;
		for(int i = 0; i < SaveManager.SLOT_COUNT ; i++) {
			SlotOverview slot = slots[i];

			boolean focused = i == selected;
			int frameColor = focused ? Palette.KEY : Palette.LINE;
			int numberColor = focused ? Palette.KEY : Palette.MUTED;
			int tagColor = focused ? Palette.TAG : Palette.MUTED;
			int textColor = switch (slot.status()) {
			    case READABLE -> focused ? Palette.KEY : Palette.TEXT;
			    case EMPTY -> Palette.MUTED;
			    case CORRUPT -> Palette.ERROR;
			};
			
			canvas.drawBox(CARD_X, y, CARD_W, CARD_H, frameColor);
			canvas.drawText(CARD_X + CARD_P, y+2 , String.valueOf(i+1), numberColor);
			
			int x = CARD_X + CARD_P + 5;
			String content = switch(slot.status()){
				case READABLE -> slot.name();
				case EMPTY -> "Empty Slot";
				case CORRUPT -> "Unreadable Save";
			};
			canvas.drawText(x, y+2, content, textColor);
			if(slot.status() == SlotStatus.READABLE) {
				String info = info(slot);
				canvas.drawText(CARD_X + CARD_W - CARD_P - info.length(), y+2, info, Palette.MUTED);
				canvas.drawText(x, y+4, slot.location(), tagColor);
			}
			
			ui.addButton(CARD_X, y, CARD_W, CARD_H, new UIHandler.Button(i));
			y += CARD_STEP;

		}
		
		//draw buttons
		String[] labels = {mode.toString(), "DELETE", "BACK"};
		boolean[] enabled = {canConfirm(), canDelete(), true};
		
		for(int i=0; i < labels.length; i++) {
			int x = BUTTON_X + i * (Canvas.BUTTON_WIDTH + BUTTON_SPACING);
			int abs_i = SaveManager.SLOT_COUNT + i;
			
			ui.drawButton(x, BUTTON_Y, labels[i], enabled[i], new UIHandler.Button(abs_i));
		}
		String modeHint = Character.toUpperCase(mode.toString().charAt(0)) + mode.toString().substring(1);
		canvas.drawHints(List.of(
				Map.entry("1-" + slots.length, "Select"),
				Map.entry("↑/↓", "Move"),
				Map.entry("ENTER", modeHint),
				Map.entry("DEL","Delete"),
				Map.entry("ESC","Back")));
		ui.present();
	}

	private boolean canConfirm() {
	    SlotStatus s = slots[selected].status();
	    return mode == Mode.SAVE || s == SlotStatus.READABLE;
	}
	private boolean canDelete() {
	    return slots[selected].status() != SlotStatus.EMPTY;
	}
	
	private static String info(SlotOverview s) {
		return "Level %d   |   Turn %d".formatted(s.level(), s.turn());
	}

	/** dialog lines naming the slot: the question, then the character and progress, or that the save is unreadable */
	private static List<String> describe(String verb, SlotOverview s) {
		String question = verb + " slot " + s.slot() + "?";
		if (s.status() != SlotStatus.READABLE) {
			return List.of(question, "Unreadable save");
		}
		return List.of(question, s.name(),info(s));
	}
}
