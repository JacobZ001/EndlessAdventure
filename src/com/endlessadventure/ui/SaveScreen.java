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
	//the main menu's picture, dimmed until it is barely there; it fades to Palette.BG under the line below the title and
	//the key hints (tools/imagegen/assets.json, "save_dim"), and the cards and buttons are solid panels over it
	private static final String BACKDROP = "save_dim";


	public enum Mode { LOAD, SAVE }
	public enum Action {CONFIRM, DELETE, BACK }
	public record Choice(Action action, int slot) {}

	private final Mode mode;
	private final SlotOverview[] slots;
	private int selected;
	private int hovered;
	
	public SaveScreen(UIHandler ui, Mode mode, SaveManager.SlotOverview[] slots, int selectedSlot) {
		super(ui);
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
				case UP, DOWN -> selected = moveFocus(in, selected, slots.length); //no HOVER: cards are objects, see moveFocus
				case HOVER -> hovered = moveFocus(in, hovered, slots.length);
				case ENTER -> action = Action.CONFIRM;
				case ESC -> action = Action.BACK;
				case DEL -> action = Action.DELETE;
				case CHAR -> {
					int digit = in.digit(slots.length);
					if (digit >= 0) {
						selected = digit;
					}
				}
				case CLICK -> {
					if (in.button() instanceof Button b) {
						if (b.action() instanceof Action a) {
							action = a; //a button
						} else {
							selected = b.index(); //a card
						}
					}
				}
			default -> {}
			}
			if(action != null && act(action) instanceof Choice c) {
				return c;
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
		PixelImage.backdrop(BACKDROP).ifPresent(art -> canvas.drawImage(1, Canvas.HEIGHT * 2 - 2 - art.getHeight(), art));

		int y = START_Y;
		canvas.drawTextCentered(y, "Choose a slot to " + mode.toString().toLowerCase(), Palette.MUTED);
		
		//draw slot cards
		y = CARD_Y;
		for(int i = 0; i < SaveManager.SLOT_COUNT ; i++) {
			SlotOverview slot = slots[i];

			boolean focused = i == selected;
			boolean h = i == hovered;
			int frameColor = focused ? Palette.KEY : h? Palette.MUTED : Palette.LINE;
			int markerColor = focused ? Palette.KEY : h? Palette.TEXT :Palette.MUTED;
			int locationColor = focused ? Palette.TAG : h? Palette.TEXT : Palette.MUTED;
			int textColor = switch (slot.status()) {
			    case READABLE -> focused ? Palette.KEY : Palette.TEXT;
			    case EMPTY -> Palette.MUTED;
			    case CORRUPT -> Palette.DANGER;
			};
			
			canvas.fill(CARD_X, y, CARD_W, CARD_H, Palette.BG); //drawBox only draws the frame, so clear the backdrop inside
			canvas.drawBox(CARD_X, y, CARD_W, CARD_H, frameColor);
			canvas.drawText(CARD_X + CARD_P, y+2 , String.valueOf(i+1), markerColor);
			
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
				canvas.drawText(x, y+4, slot.location(), locationColor);
			}
			
			ui.addButtonBox(CARD_X, y, CARD_W, CARD_H, new Button(i));
			y += CARD_STEP;

		}
		
		//draw buttons
		String[] labels = {mode.toString(), "DELETE", "BACK"};
		boolean[] enabled = {canConfirm(), canDelete(), true};
		
		for(int i=0; i < labels.length; i++) {
			int x = BUTTON_X + i * (Canvas.BUTTON_WIDTH + BUTTON_SPACING);
			canvas.fill(x, BUTTON_Y, Canvas.BUTTON_WIDTH, Canvas.BUTTON_HEIGHT, Palette.BG);
			ui.drawButton(x, BUTTON_Y, labels[i], enabled[i], new Button(Action.values()[i])); //labels follow Action order
		}
		String modeHint = Character.toUpperCase(mode.toString().charAt(0)) + mode.toString().substring(1);
		endFrame(List.of(
				Map.entry("1-" + slots.length, "Select"),
				Map.entry("↑/↓", "Move"),
				Map.entry("ENTER", modeHint),
				Map.entry("DEL","Delete"),
				Map.entry("ESC","Back")));
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
