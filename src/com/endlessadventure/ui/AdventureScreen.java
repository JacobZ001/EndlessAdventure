package com.endlessadventure.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.endlessadventure.GameState;
import com.endlessadventure.StatRules;
import com.endlessadventure.combat.Combatant;
import com.endlessadventure.entity.Attribute;
import com.endlessadventure.entity.Player;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.story.PastScene;

/** the story screen: the scene fills the screen, the story column on the left reads top to bottom into the choices,
 *  which stay anchored at the bottom. layout in design/interface.md (adventure screen) */
public final class AdventureScreen extends Screen {
	private static final int COL_X = 6, COL_W = 96,     //the story column on the left
							 TOP_Y = 1,                  //top bar: the character on the left, the toolbar on the right
							 HEADING_Y = 4,              //location and turn, a rule underneath
							 STORY_Y = 7,                //last action, then the description
							 INPUT_LINES = 2,
							 INPUT_Y = Canvas.HEIGHT - 1 - INPUT_LINES, //48: the inputField sits at the bottom, rows stack above it
							 INPUT_W = COL_W - ROW_TEXT_OFFSET, //inputField text lines up with the option texts
							 HP_BAR_W = 24,
							 HINT_RIGHT = Canvas.WIDTH - 3; //the key hint column ends here, its last row just above the bottom row
	
	//the scene darkens behind the story column (fading out toward the right), the top bar and the key hints
	private static final int FADE_FROM_X = 96, FADE_TO_X = 150;
	private static final double FADE = 0.60, TOP_SHADE = 0.55, HINT_SHADE = 0.35;

	private final CurrentScene scene;
	private final Player player;
	private final String[] options;
	private final String enemyText;  //e.g. "Bandit x2"; null when no enemy is here
	private final String lastAction; //the action that led to this scene, null at the opening
	private final int commandRow;      //the Fight/Rest row right after the options;
	private final int inputRow;      //the inputField row, always last
	private final TextField inputField;
	private int focus = 0; //0..options.length - 1: an option; commandRow: Fight/Rest; inputRow: the inputField
	private boolean waiting = false;

	public enum Action { ACT, SAVE, INV, CHAR, FIGHT, REST, BACK }
	public record Choice(Action action, String text) {} // text: the chosen option or the typed action, null otherwise

	public AdventureScreen(UIHandler ui, GameState state, String enemyText) {
		super(ui);
		scene = state.getCurrentScene();
		player = state.getPlayer();
		options = scene.getOptions();
		this.enemyText = enemyText;
		List<PastScene> history = state.getHistory();
		lastAction = history.isEmpty()? null : history.getLast().getPlayerAction();
		commandRow = enemyText != null || player.getHp() != player.getMaxHp()? options.length : -1; //show rest / fight option if enemy is present or hp not full
		inputRow = options.length + (commandRow == -1? 0 : 1);
		inputField = new TextField(ui, INPUT_W, INPUT_LINES);
	}

	/** draw this scene under the waiting card and return at once; the caller then asks the model.
	 *  run() does this itself after an action; call it directly for requests that start elsewhere, such as after a fight */
	public void showWaiting() {
		waiting = true;
		drawFrame();
	}

	private boolean typing() {
		return focus == inputRow;
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
				case UP, DOWN -> focus = moveFocus(in, focus, inputRow + 1);
				case HOVER -> {
					if (!typing()) {
						focus = moveFocus(in, focus, inputRow); //hovering never leaves or enters the inputField row
					}
				}
				case ENTER -> action = Action.ACT;
				case ESC -> {
					if (typing()) {
						focus = inputRow - 1; //leave the inputField row for the row above it; the typed text stays
					} else {
						action = Action.BACK;
					}
				}
				case CHAR -> {
					int digit = in.digit(inputRow); //the options, then Fight
					if (digit >= 0) {
						focus = digit;
						action = Action.ACT;
					}
					else if(!typing()) {
						switch(Character.toUpperCase(in.ch())){
							case 'S' -> action = Action.SAVE;
							case 'I' -> action = Action.INV;
							case 'C' -> action = Action.CHAR;
						}
					}
				}
				case CLICK -> {
					if (in.button() instanceof Button b) {
						if(b.action() instanceof Action a) {
							action = a;
						}
						else if(b.index() >= 0) {
							//click on a row acts at once; while typing it only selects that row,
							//and a click on the inputField row only takes the focus
							boolean wasTyping = typing(); //before the click moves the focus
							focus = b.index();
							if (!wasTyping && !typing()) {
								action = Action.ACT;
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
				if(focus == commandRow) {
					if (enemyText != null) {
						yield new Choice(Action.FIGHT, null); //no story request: the fight starts at once
					}
					showWaiting(); //resting asks the model for the scene after it, like an action
					yield new Choice(Action.REST, null);
				}
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
				showWaiting();
				yield new Choice(Action.ACT, playerAction);
			}
			case SAVE -> new Choice(Action.SAVE, null);
			case INV -> new Choice(Action.INV, null);
			case CHAR -> new Choice(Action.CHAR, null);
			case FIGHT -> new Choice(Action.FIGHT, null);
			case REST -> new Choice(Action.REST, null);
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

		//rows stack up from the inputField at the bottom, two rows each, under an instruction line
		int rowsY = INPUT_Y - 2 * inputRow;
		int instructionY = rowsY - 2;

		//key hints for the current state, bottom right; none while waiting for the story
		List<Map.Entry<String, String>> hints = waiting? List.of() : typing()?
				List.of(Map.entry("ENTER", "Act"), Map.entry("ESC", "Back to choices")) :
				List.of(Map.entry("ENTER", "Act"),
						Map.entry("1-" + inputRow, "Choose"),
						Map.entry("↑↓", "Move"),
						Map.entry("I", "Inventory"),
						Map.entry("C", "Character"),
						Map.entry("S", "Save"),
						Map.entry("ESC", "Menu"));
		int capW = hints.stream().mapToInt(h -> h.getKey().length()).max().orElse(0) + 2; //a key cap: " KEY "
		int labelW = hints.stream().mapToInt(h -> h.getValue().length()).max().orElse(0);
		int hintX = HINT_RIGHT - labelW - 1 - capW, hintY = Canvas.HEIGHT - 1 - hints.size();

		//draw the scene over the whole screen, then darken it where text goes; every text comes after this
		PixelImage.scene("grassland_road").ifPresent(art -> canvas.drawImage(0, 0, art)); //TODO the scene id will come from the story
		canvas.fadeLeft(FADE_FROM_X, FADE_TO_X, FADE);
		canvas.shadeAround(0, 0, Canvas.WIDTH, 2, TOP_SHADE, 4);
		if (!hints.isEmpty()) {
			canvas.shadeAround(hintX, hintY, HINT_RIGHT - hintX, hints.size(), HINT_SHADE, 14);
		}

		//draw top bar: the character on the left, the toolbar on the right
		{
			int color = Palette.colorOf(Combatant.Side.PLAYER);
			String level = "Level " + player.getLevel();
			int x = 3;
			canvas.drawText(x, TOP_Y, player.getName(), color);
			x += player.getName().length() + 2;
			canvas.drawText(x, TOP_Y, level, Palette.SUBTLE);
			x += level.length() + 3;
			canvas.drawText(x, TOP_Y, "HP", Palette.SUBTLE);
			drawBar(x + 3, TOP_Y, HP_BAR_W, player.getHp(), player.getMaxHp(), color);
			canvas.drawText(x + 3 + HP_BAR_W + 2, TOP_Y, player.getHp() + " / " + player.getMaxHp(), Palette.TEXT);

			String[] labels = {"Main Menu", "Save", "Inventory", "Character"};
			Action[] actions = {Action.BACK, Action.SAVE, Action.INV, Action.CHAR};
			drawToolBar(Canvas.WIDTH - 3, TOP_Y, labels, actions, "  |  ");

			//draw heading: where and when, a rule underneath
			String location = scene.getLocation().toUpperCase();
			canvas.drawText(COL_X, HEADING_Y, location, Palette.TAG);
			canvas.drawText(COL_X + location.length(), HEADING_Y, "  /  TURN %02d".formatted(scene.getTurn()), Palette.SUBTLE);
			canvas.drawDivider(COL_X, HEADING_Y + 1, COL_W, Palette.MUTED); //Palette.LINE vanishes over the picture
		}

		//draw last action
		int y = STORY_Y;
		if (lastAction != null) {
			canvas.drawText(COL_X, y, "You:", Palette.SUBTLE);
			for (String line : Canvas.wrapText(lastAction, COL_W - 6)) {
				canvas.drawText(COL_X + 5, y++, line, Palette.SUBTLE);
			}
			y++;
		}

		//draw description: one sentence per paragraph with a blank row between; without the blank rows when they do not fit
		int bottom = instructionY - 1; //keep a blank row above the instruction
		List<List<String>> paragraphs = new ArrayList<>();
		for (String sentence : scene.getDescription().split("(?<=[.!?])\\s+")) {
			paragraphs.add(Canvas.wrapText(sentence, COL_W));
		}
		int lines = paragraphs.stream().mapToInt(List::size).sum();
		int gap = lines + paragraphs.size() - 1 <= bottom - y ? 1 : 0;
		for (List<String> paragraph : paragraphs) {
			for (String line : paragraph) {
				if (y < bottom) { //an extreme description loses its last lines rather than covering the choices
					canvas.drawText(COL_X, y, line, Palette.TEXT);
				}
				y++;
			}
			y += gap;
		}

		//draw instruction
		String instruction = player.getName() + ", what will you do? ";
		canvas.drawText(COL_X, instructionY, instruction, Palette.TAG);
		canvas.drawDivider(COL_X + instruction.length(), instructionY, COL_W - instruction.length(), Palette.MUTED);

		//draw options, then Fight in the enemies' color; all quiet while waiting
		y = rowsY;
		for (int i = 0; i < options.length; i++) {
			drawListRow(i, COL_X, y, COL_W, String.valueOf(i + 1), options[i], focus == i && !waiting, !waiting);
			y += 2;
		}
		if(commandRow != -1) {
			String text;
			boolean focused = focus == commandRow && !waiting;
			int color;
			if (enemyText != null) {
				text = "Fight " + enemyText;
				color = Palette.colorOf(Combatant.Side.ENEMY);
			}
			else {
				text = "Take a rest (+%d HP)".formatted(Math.min(player.getMaxHp() - player.getHp(), (int) Math.round(player.getMaxHp() * StatRules.REST_RECOVERY))); //the same amount GameEngine restores
				color = Palette.colorOf(Attribute.CON);
			}
			drawListRow(commandRow, COL_X, y, COL_W, String.valueOf(commandRow + 1), text, focused, !waiting);
			if (!focused && !waiting) {
				canvas.drawText(COL_X + ROW_TEXT_OFFSET, y, text, color);
			}

			y += 2;
		}

		//draw freeform inputField at the bottom, in the same row style
		if (typing()) {
			canvas.drawText(COL_X, INPUT_Y, FOCUS_INDICATOR, Palette.KEY);
		}
		canvas.drawText(COL_X + ROW_MARKER_OFFSET, INPUT_Y, ">", typing() ? Palette.KEY : Palette.SUBTLE);
		if (inputField.isEmpty() && !typing()) {
			canvas.drawText(COL_X + ROW_TEXT_OFFSET, INPUT_Y, "Or describe your own action...", Palette.SUBTLE);
		} else {
			inputField.drawField(canvas, COL_X + ROW_TEXT_OFFSET, INPUT_Y, typing() ? Palette.KEY : Palette.TEXT, typing());
		}
		ui.addButtonBox(COL_X, INPUT_Y, COL_W, INPUT_LINES, new Button(inputRow));

		//draw key hints: a gold key cap, then what it does
		for (int i = 0; i < hints.size(); i++) {
			String key = hints.get(i).getKey();
			canvas.drawText(hintX + capW - key.length() - 2, hintY + i, " " + key + " ", Palette.KEY);
			canvas.drawText(hintX + capW + 1, hintY + i, hints.get(i).getValue(), Palette.SUBTLE);
		}

		//while the story is being asked for: no clicks, the whole screen dims and a card waits in the middle
		if (waiting) {
			ui.clearButtons();
			canvas.dim();
			String message = "Story progressing...";
			int cardW = 3 + "STORY".length() + 3 + message.length() + 3;
			int cardX = (Canvas.WIDTH - cardW) / 2, cardY = Canvas.HEIGHT / 2 - 1;
			canvas.fill(cardX, cardY, cardW, 3, Palette.PANEL);
			for (int i = 0; i < 3; i++) {
				canvas.drawText(cardX, cardY + i, FOCUS_INDICATOR, Palette.KEY);
			}
			canvas.drawText(cardX + 3, cardY + 1, "STORY", Palette.KEY);
			canvas.drawText(cardX + 3 + "STORY".length() + 3, cardY + 1, message, Palette.TEXT);
		}

		ui.present(); //no bottom hint bar: the hints are the column above
	}

}
