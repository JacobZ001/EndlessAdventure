package com.endlessadventure.ui;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.endlessadventure.GameState;
import com.endlessadventure.entity.Player;
import com.endlessadventure.story.CurrentScene;

public final class AdventureScreen extends Screen {
	private static final int CONTENT_W = 150,
							 CONTENT_X = (Canvas.WIDTH - CONTENT_W) / 2,
							 HEADER_Y = 2,
							 ART_Y = HEADER_Y + 3, //5
							 ART_H = 28,
							 DES_Y = ART_Y + ART_H + 1, //34
							 DES_LINES = 4, //500 characters at width 150; more is cut
							 INS_Y = 39,
							 OPT_Y = INS_Y + 2, //41, one option every 2 rows
							 INPUT_W = CONTENT_W - ROW_TEXT_OFFSET, //inputField text lines up with the option texts
							 INPUT_LINES = 2; //right after the last option: rows 47-48 with 3 options
	
	public enum Action { ACT, SAVE, INV, CHAR, FIGHT, BACK }// TODO remove FIGHT when encounters arrive (step 4)
	public record Choice(Action action, String text) {} // text: the chosen option or the typed action, null otherwise

	private final CurrentScene scene;
	private final Player player;
	private final String[] options;
	private final TextField inputField;
	private int focus = 0; //0..options.length - 1: an option; options.length: the inputField row
	private boolean waiting = false;

	public AdventureScreen(UIHandler ui, GameState state) {
		super(ui);
		scene = state.getCurrentScene();
		player = state.getPlayer();
		options = scene.getOptions();
		inputField = new TextField(ui, INPUT_W, INPUT_LINES);
	}
	
	private boolean typing() {
		return focus == options.length;
	}

	public Choice run() {
		while (true) {
			drawFrame();
			UIHandler.Input in = ui.readInput();
			Action action = null;
			if (typing() && inputField.handleInput(in)) {
				continue; //typed or deleted a character
			}
			switch (in.inputKey()) {
				case UP, DOWN -> focus = moveFocus(in, focus, options.length + 1);
				case HOVER -> {
					if (!typing()) {
						focus = moveFocus(in, focus, options.length); //hovering never leaves or enters the inputField row
					}
				}
				case ENTER -> action = Action.ACT;
				case ESC -> {
					if (typing()) {
						focus = options.length - 1; //leave the inputField row; the typed text stays
					} else {
						action = Action.BACK;
					}
				}
				case CHAR -> {
					int digit = in.digit(options.length);
					if (digit >= 0) {
						focus = digit;
						action = Action.ACT;
					}
					else if(!typing()) {
						switch(Character.toUpperCase(in.ch())){
							case 'S' -> action = Action.SAVE;
							case 'I' -> action = Action.INV;
							case 'C' -> action = Action.CHAR;
							//TODO implement other hot keys
						}
					}
				}
				case CLICK -> {
					if (in.button() instanceof Button b) {
						if(b.action() instanceof Action a) {
							action = a;
						}
						else if(b.index() >= 0) {
							//click on a option acts at once; click on the inputField row only takes the focus
							if (!typing()) {
								focus = b.index();
								action = Action.ACT; 
							}
							else {
								focus = b.index();
							}
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
		return switch(a) {
			case ACT -> {
				String playerAction;
				if(typing()) {
					if(!inputField.getText().isBlank()) {
						playerAction = inputField.getText().strip();
					}
					else {
						ui.showWarning("Describe your action to proceed.");
						yield null;
					}
				}
				else {
					playerAction = options[focus];
				}
				ui.showSuccess("Story progressing...");
				waiting = true;
				drawFrame();
				yield new Choice(Action.ACT, playerAction);
			}
			case SAVE -> new Choice(Action.SAVE, null);
			case INV -> new Choice(Action.INV, null);
			case CHAR -> new Choice(Action.CHAR, null);
			case FIGHT -> new Choice(Action.FIGHT, null);// TODO remove when encounters arrive (step 4)
			case BACK -> {
				boolean confirmed = ConfirmDialog.show(ui,
						"RETURN TO MAIN MENU",
						List.of("Return to Main Menu?"),
						"Unsaved progress will be lost",
						"RETURN");
				yield confirmed? new Choice(Action.BACK, null) : null;
			}
		};

	}

	
	//drawing
	private void drawFrame() {
		beginFrame();
		
		//draw header: where and when on the left, the toolbar on the right, a rule underneath
		String location = scene.getLocation().toUpperCase();
		canvas.drawText(CONTENT_X, HEADER_Y, location, Palette.TAG);
		canvas.drawText(CONTENT_X + location.length(), HEADER_Y, "  /  TURN %02d".formatted(scene.getTurn()), Palette.MUTED);
		
		//draw toolbar
		String[] labels = {"Main Menu", "Save", "Inventory", "Character", "Fight"}; //TODO add new tool bar buttons // TODO remove FIGHT when encounters arrive (step 4)
		Action[] actions = {Action.BACK, Action.SAVE, Action.INV, Action.CHAR, Action.FIGHT};// TODO remove FIGHT when encounters arrive (step 4)
		String divider = "  |  ";
		drawToolBar(CONTENT_X + CONTENT_W, HEADER_Y, labels, actions, divider);
		
		Optional<PixelImage> art = PixelImage.scene("forest_path");   //TODO the scene id will come from the story
		if (art.isPresent()) {
		    canvas.drawImage(CONTENT_X, ART_Y * 2, art.get());
		} else {
		    canvas.fill(CONTENT_X, ART_Y, CONTENT_W, ART_H, Palette.LINE);
		}
		
		//draw description
		List<String> description = Canvas.wrapText(scene.getDescription(), CONTENT_W);
		for (int i = 0; i < Math.min(DES_LINES, description.size()); i++) {
			canvas.drawText(CONTENT_X, DES_Y + i, description.get(i), Palette.TEXT);
		}
		
		//draw instruction
		String instruction = player.getName() + ", what will you do? ";
		canvas.drawText(CONTENT_X, INS_Y, instruction , Palette.TAG);
		canvas.drawDivider(CONTENT_X + instruction.length(), INS_Y, CONTENT_W - instruction.length());
		
		//draw options
		int y = OPT_Y;
		for(int i = 0; i < options.length; i++) {
			drawListRow(i, CONTENT_X, y, CONTENT_W, String.valueOf(i + 1), options[i], focus == i, true);
			y += 2;
		}
		
		//draw freeform inputField right after the options, in the same row style
		if (typing()) {
			canvas.drawText(CONTENT_X, y, FOCUS_INDICATOR, Palette.KEY);
		}
		canvas.drawText(CONTENT_X + ROW_MARKER_OFFSET, y, ">", typing() ? Palette.KEY : Palette.MUTED);
		if (inputField.isEmpty() && !typing()) {
			canvas.drawText(CONTENT_X + ROW_TEXT_OFFSET, y, "Or describe your own action...", Palette.MUTED);
		} else {
			inputField.drawField(canvas, CONTENT_X + ROW_TEXT_OFFSET, y, typing() ? Palette.KEY : Palette.TEXT, typing());
		}
		ui.addButtonBox(CONTENT_X, y, CONTENT_W, INPUT_LINES, new Button(options.length));
		
		if(waiting) {
			ui.clearButtons();
			canvas.dim(0, 0, Canvas.WIDTH, Canvas.HEIGHT - 1);
		}

		endFrame(typing()?
				List.of(
						Map.entry("TYPE", "Freeform Action"),
						Map.entry("BACKSPACE", "Delete"),
						Map.entry("ENTER", "Act"),
						Map.entry("ESC","Back to Options")) :
				List.of(
						Map.entry("1-" + options.length, "Select"),
						Map.entry("↑/↓", "Move"),
						Map.entry("ENTER", "Act"),
						Map.entry("I", "Inventory"),
						Map.entry("C", "Character"),
						Map.entry("S", "Save"),
						Map.entry("ESC","Main Menu")));
	}

}
