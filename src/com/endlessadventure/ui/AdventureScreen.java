package com.endlessadventure.ui;

import java.util.List;
import java.util.Map;

import com.endlessadventure.GameState;
import com.endlessadventure.entity.Player;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.ui.UIHandler.Button;

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
							 OPTION_NUM_X = CONTENT_X + 3,
							 OPTION_TEXT_X = OPTION_NUM_X + 4,
							 INPUT_W = CONTENT_X + CONTENT_W - OPTION_TEXT_X,
							 INPUT_LINES = 2; //right after the last option: rows 47-48 with 3 options
	
	public enum Action { ACT, SAVE, BACK }
	public record Choice(Action action, String text) {} // text: the chosen option or the typed action, null otherwise

	private final CurrentScene scene;
	private final Player player;
	private final String[] options;
	private int selected = 0; // option
	private StringBuilder typed = new StringBuilder();
	private boolean isTyping = false;

	public AdventureScreen(UIHandler ui, GameState state) {
		super(ui, "ENDLESS ADVENTURE");
		scene = state.getCurrentScene();
		player = state.getPlayer();
		options = scene.getOptions();
	}

	public Choice run() {
		while (true) {
			drawFrame();
			UIHandler.Input in = ui.readInput();
			Action action = null;
			switch (in.inputKey()) {
				case UP -> selected = Math.floorMod(selected - 1, options.length + 1);
				case DOWN -> selected = Math.floorMod(selected + 1, options.length + 1);
				case ENTER -> action = Action.ACT;
				case ESC -> action = Action.BACK;
				case CHAR -> {
					int digit = in.digit();
					if(isTyping) {
						if(fits(typed.toString() + in.ch())) {
							typed.append(in.ch());
						}
						else {
							ui.showWarning("The input box is full.");
							continue;
						}
					}
					else if (digit >= 0 && digit < options.length) {
						selected = digit;
					}
				}
				case CLICK -> {
					UIHandler.Button b = in.button();
					if (b != null) {
						selected = b.index();
					}
				}
				case HOVER -> {}
				case BACKSPACE -> {
					if(isTyping && !typed.isEmpty()) {
						typed.deleteCharAt(typed.length()-1);
					}
				}
				default -> {}
			}
			
			isTyping = selected == options.length;
			if (action != null) {
				Choice choice = act(action);
				if (choice != null) {
					return choice;
				}
			}
		}
	}

	private Choice act(Action a) {
		return switch(a) {
			case ACT -> {
				String playerAction;
				if(isTyping) {
					if(!typed.isEmpty()) {
						playerAction = typed.toString().strip();
					}
					else {
						ui.showWarning("Describe your action to proceed.");
						yield null;
					}
				}
				else {
					playerAction = options[selected];
				}
				yield new Choice(Action.ACT, playerAction);
			}
			case SAVE -> new Choice(Action.SAVE, null);
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
		
		//draw top panel
		String location = scene.getLocation();
		canvas.drawText(CONTENT_X, HEADER_Y, location, Palette.KEY);
		canvas.drawText(CONTENT_X + location.length(), HEADER_Y, "  Turn " + scene.getTurn(), Palette.MUTED);
		
		//TODO draw scene visuals, stub for now
		canvas.fill(CONTENT_X, ART_Y, CONTENT_W, ART_H, Palette.FOCUSED_BG);
		
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
			canvas.drawText(OPTION_NUM_X, y, String.valueOf(i+1), selected == i? Palette.KEY : Palette.MUTED);
			canvas.drawText(OPTION_TEXT_X, y, options[i], selected == i? Palette.KEY : Palette.TEXT);
			ui.addButton(CONTENT_X, y, CONTENT_W, 1, new Button(i));
			y += 2;
		}
		
		//draw freeform input right after the options
		boolean highlighted = selected == options.length;
		int color = highlighted? Palette.KEY : Palette.MUTED;
		canvas.drawText(OPTION_NUM_X, y, ">", color);
		if (typed.isEmpty() && !highlighted) {
			canvas.drawText(OPTION_TEXT_X, y, "Or describe your own action...", color);
		} else {
			List<String> inputLines = Canvas.wrapText(typed.toString(), INPUT_W);
			for(int i = 0; i < inputLines.size(); i++) {
				canvas.drawText(OPTION_TEXT_X, y + i, inputLines.get(i), color);
			}
			if(highlighted) {
				//wrapText drops trailing spaces; move the caret one cell so a typed space shows
				String last = inputLines.getLast();
				boolean space = !last.isEmpty() && typed.charAt(typed.length() - 1) == ' ' && last.length() < INPUT_W;
				int caretX = OPTION_TEXT_X + last.length() + (space ? 1 : 0);
				canvas.drawText(caretX, y + inputLines.size() - 1, "_", color);
			}
		}
		ui.addButton(CONTENT_X, y, CONTENT_W, INPUT_LINES, new Button(options.length));

		canvas.drawHints(List.of(
				Map.entry("1-" + options.length, "Select"),
				Map.entry("↑/↓", "Move"),
				Map.entry("ENTER", "Act"),
				Map.entry("TYPE", "Freeform Action"),
				Map.entry("ESC","Main Menu")));
		ui.present();
	}
	
	/** the typed text may fill the input box up to the end of its last line, but not wrap past it */
	private static boolean fits(String text) {
		return Canvas.wrapText(text, INPUT_W).size() <= INPUT_LINES;
	}
}
