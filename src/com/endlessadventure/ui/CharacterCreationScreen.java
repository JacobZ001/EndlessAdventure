package com.endlessadventure.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.endlessadventure.StatRules;
import com.endlessadventure.entity.Attribute;
import com.endlessadventure.entity.Player;
import com.endlessadventure.entity.Skill;

/** step-by-step character creation: name, attributes, combat skills, general skills, review */
public final class CharacterCreationScreen extends Screen {
	private static final String[] STEPS = {"ATTRIBUTES", "COMBAT SKILLS", "GENERAL SKILLS", "REVIEW"};
	private static final int ATTRIBUTES = 0, COMBAT = 1, GENERAL = 2, REVIEW = 3;

	//layout: two columns from just under the step bar to near the bottom border;
	//the right column holds the details box and, under it, an action bar whose buttons end level with the panel
	private static final int STEP_BAR_Y = 2;
	private static final int PANEL_W = 86, DETAIL_W = 86, DETAIL_SPACING = 3;
	private static final int PANEL_X = (Canvas.WIDTH - PANEL_W - DETAIL_W - DETAIL_SPACING) / 2;
	private static final int DETAIL_X = PANEL_X + PANEL_W + DETAIL_SPACING;
	private static final int PANEL_Y = 4;
	private static final int PANEL_BOTTOM = Canvas.HEIGHT - 4; //last row of both columns
	private static final int PANEL_H = PANEL_BOTTOM - PANEL_Y + 1;

	private static final int BUTTON_SPACING = 2;
	private static final int BUTTON_Y = PANEL_BOTTOM - Canvas.BUTTON_HEIGHT + 1;
	private static final int NEXT_X = DETAIL_X + DETAIL_W - Canvas.BUTTON_WIDTH;
	private static final int BACK_X = NEXT_X - BUTTON_SPACING - Canvas.BUTTON_WIDTH;
	private static final int DETAIL_H = BUTTON_Y - 1 - PANEL_Y; //leaves one empty row above the action bar

	//name input box, centered
	private static final int NAME_BOX_W = 52;
	private static final int NAME_BOX_X = (Canvas.WIDTH - NAME_BOX_W) / 2;
	private static final int NAME_BOX_Y = Canvas.HEIGHT / 2 - 1;

	private static final int PIP_EMPTY = Palette.mix(Palette.BG, Palette.MUTED, 0.6);
	
	/** clickable actions: [-] and [+] on a row (a click elsewhere on the row only focuses it), and the two buttons */
	private enum Action { MINUS, PLUS, BACK, NEXT }

	private Player.PlayerBuilder playerBuilder;
	private int step = ATTRIBUTES;
	private int focus = 0; //focused row

	public CharacterCreationScreen(UIHandler ui) {
		super(ui, "CREATE YOUR CHARACTER");
	}

	/** returns the new player, or null if the player left for the main menu */
	public Player run() {
		while (true) {
			if (playerBuilder == null && !askName()) {
				return null;
			}
			Player player = allocate();
			if (player != null) {
				return player;
			}
			playerBuilder = null; //backed out of the first step: ask for the name again
		}
	}

	/** asks for a name until it is valid; returns false if the player pressed Esc */
	private boolean askName() {
		TextField name = new TextField(ui, NAME_BOX_W - 6, 1);
		while (true) {
			drawNameFrame(name);
			UIHandler.Input in = ui.readInput();
			if (name.handleInput(in)) {
				continue; //typed or deleted a character
			}
			switch (in.inputKey()) {
				case ESC -> {
					return false;
				}
				case ENTER -> {
					try {
						playerBuilder = new Player.PlayerBuilder(name.getText());
						step = ATTRIBUTES;
						focus = 0;
						return true;
					} catch (IllegalArgumentException e) {
						ui.showWarning(e.getMessage());
					}
				}
				default -> {}
			}
		}
	}

	/** runs the allocation steps; returns the built player, or null if the player backed out of the first step */
	private Player allocate() {
		while (true) {
			drawAllocationFrame();
			UIHandler.Input in = ui.readInput();
			List<Enum<?>> rows = rows();
			
			switch (in.inputKey()) {
				case UP, DOWN, HOVER -> focus = moveFocus(in, focus, rows.size());
				case LEFT -> adjustStat(rows, focus, -1);
				case RIGHT -> adjustStat(rows, focus, +1);
				case ENTER -> {
					if (next() instanceof Player p) {
						return p;
					}
				}
				case ESC -> {
					if (!back()) {
						return null;
					}
				}
				case CLICK -> {
					if (in.button() instanceof Button b && b.action() instanceof Action a) {
						switch(a) {
							case BACK -> {
								if (!back()) {
									return null;
								}
							}
							case NEXT -> {
								if (next() instanceof Player p) {
									return p;
								}
							}
							case MINUS -> adjustStat(rows, focus, -1);
							case PLUS -> adjustStat(rows, focus, +1);
						}
					}
				}
				default -> {}
			}
		}
	}

	/** moves to the next step if this one is complete; on the review step, builds the player */
	private Player next() {
		if (remaining(step) > 0) {
			ui.showWarning("Spend every point to continue (" + remaining(step) + " left).");
			return null;
		}
		if (step == REVIEW) {
			Player player = playerBuilder.build();
			ui.showSuccess("Starting your adventure...");
			drawAllocationFrame(); //keep this screen up with the message while the story request runs
			return player;
		}
		step++;
		focus = 0;
		return null;
	}

	/** moves to the previous step; returns false when already on the first step */
	private boolean back() {
		if (step == ATTRIBUTES) {
			return false;
		}
		step--;
		focus = 0;
		return true;
	}

	private void adjustStat(List<Enum<?>> rows, int index, int delta) {
		if (index < 0 || index >= rows.size()) {
			return;
		}
		try {
			switch (rows.get(index)) {
			case Attribute a -> {
				if (delta > 0) {
					playerBuilder.incrAttribute(a);
				} else {
					playerBuilder.decrAttribute(a);
				}
			}
			case Skill s -> {
				if (delta > 0) {
					playerBuilder.incrSkill(s);
				} else {
					playerBuilder.decrSkill(s);
				}
			}
			default -> {}
			}
		} catch (IllegalArgumentException e) {
			ui.showWarning(e.getMessage());
		}
	}

	/** adjustable rows of the current step, in display order */
	private List<Enum<?>> rows() {
		List<Enum<?>> rows = new ArrayList<>();
		switch (step) {
			case ATTRIBUTES -> rows.addAll(List.of(Attribute.values()));
			case COMBAT -> rows.addAll(skillsIn(Skill.Group.COMBAT));
			case GENERAL -> rows.addAll(skillsIn(Skill.Group.GENERAL));
			default -> {}
		}
		return rows;
	}

	private static List<Skill> skillsIn(Skill.Group group) {
		List<Skill> skills = new ArrayList<>();
		for (Skill s : Skill.values()) {
			if (s.group == group) {
				skills.add(s);
			}
		}
		return skills;
	}

	private int remaining(int step) {
		return switch (step) {
			case ATTRIBUTES -> playerBuilder.getRemainingAttributePoints();
			case COMBAT -> playerBuilder.getRemainingSkillPoints(Skill.Group.COMBAT);
			case GENERAL -> playerBuilder.getRemainingSkillPoints(Skill.Group.GENERAL);
			default -> 0;
		};
	}

	private int budget(int step) {
		return switch (step) {
			case ATTRIBUTES -> StatRules.ATTRIBUTE_START_BUDGET;
			case COMBAT -> StatRules.COMBAT_SKILL_START_BUDGET;
			case GENERAL -> StatRules.GENERAL_SKILL_START_BUDGET;
			default -> 0;
		};
	}


	//drawing
	/** question, input box with the typed name and a caret, centered */
	private void drawNameFrame(TextField name) {
		beginFrame();
		canvas.drawTextCentered(NAME_BOX_Y - 3, "What is your name, adventurer?", Palette.TEXT);
		canvas.drawBox(NAME_BOX_X, NAME_BOX_Y, NAME_BOX_W, 3, Palette.KEY);
		name.drawField(canvas, NAME_BOX_X + 2, NAME_BOX_Y + 1, Palette.TEXT, true);
		endFrame(List.of(
				Map.entry("ENTER", "Confirm"),
				Map.entry("BACKSPACE", "Delete"),
				Map.entry("ESC", "Main Menu")));
	}
	
	private void drawAllocationFrame() {
		beginFrame();
		drawStepBar();

		//draw rows
		List<Enum<?>> rows = rows();
		if (step == REVIEW) {
			drawReview();
		} else {
			drawAllocation(rows);
			drawDetails(rows.get(focus));
		}

		//draw BACK and NEXT buttons
		boolean complete = remaining(step) == 0;
		ui.drawButton(BACK_X, BUTTON_Y, "BACK", true, new Button(Action.BACK));
		ui.drawButton(NEXT_X, BUTTON_Y, step == REVIEW ? "CONFIRM" : "NEXT", complete, new Button(Action.NEXT));
		if (step != REVIEW) {
			drawPointsLeft();
		}


		endFrame(List.of(
				Map.entry("↑/↓", "Select"),
				Map.entry("←/→", "- / +"),
				Map.entry("ENTER", step == REVIEW ? "Confirm" : "Next"),
				Map.entry("ESC", "Back")));
	}

	private void drawStepBar() {
		int x = PANEL_X + 2;
		for (int i = 0; i < STEPS.length; i++) {
			int color = i == step ? Palette.KEY : (i < step ? Palette.SUCCESS : Palette.MUTED);
			String label = (i + 1) + "  " + STEPS[i];
			canvas.drawText(x, STEP_BAR_Y, label, color);
			x += label.length();
			if (i < STEPS.length - 1) {
				canvas.drawText(x + 2, STEP_BAR_Y, "──────", Palette.MUTED);
				x += 10;
			}
		}
		String name = playerBuilder.getName();
		canvas.drawText(DETAIL_X + DETAIL_W - 2 - name.length(), STEP_BAR_Y, name, Palette.TEXT);
	}

	private void drawAllocation(List<Enum<?>> rows) {
		int left = remaining(step);
		canvas.drawBox(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, Palette.MUTED);
		canvas.drawText(PANEL_X + 2, PANEL_Y, " " + STEPS[step] + " ", Palette.KEY);

		int x = PANEL_X + 2;
		int w = PANEL_W - 4;
		switch (step) {
		case ATTRIBUTES -> {
			canvas.drawText(x + 2, PANEL_Y + 2, "Spend " + budget(step) + " points. An attribute starts at most at "
					+ StatRules.ATTRIBUTE_START_CAP + ".", Palette.MUTED);
			for (int i = 0; i < rows.size(); i++) {
				Attribute a = (Attribute) rows.get(i);
				drawRow(i, x, PANEL_Y + 5 + i * 3, w, a.displayName, a, true, playerBuilder.getAttribute(a),
						StatRules.ATTRIBUTE_START_CAP, left);
			}
		}
		case COMBAT -> {
			canvas.drawText(x + 2, PANEL_Y + 2, "Spend " + budget(step) + " points on fighting skills, up to "
					+ StatRules.SKILL_START_CAP + " each.", Palette.MUTED);
			for (int i = 0; i < rows.size(); i++) {
				Skill s = (Skill) rows.get(i);
				drawRow(i, x, PANEL_Y + 5 + i * 3, w, s.displayName, s.defaultAttribute, true, playerBuilder.getSkill(s),
						StatRules.SKILL_START_CAP, left);
			}
		}
		case GENERAL -> {
			canvas.drawParagraph(x + 2, PANEL_Y + 2, w, "Spend " + budget(step) + " points, up to " + StatRules.SKILL_START_CAP
					+ " each. Group headers show your matching attribute."
					+ " The attribute used in actual gameplay can change based on the scenario", Palette.MUTED);
			int y = PANEL_Y + 4;
			Attribute group = null;
			for (int i = 0; i < rows.size(); i++) {
				Skill s = (Skill) rows.get(i);
				if (s.defaultAttribute != group) {
					group = s.defaultAttribute;
					canvas.drawText(x + 2, ++y, group.name() + "  " + playerBuilder.getAttribute(group), Palette.colorOf(group));
					canvas.drawText(x + 10, y, "─".repeat(w - 14), Palette.mix(Palette.BG, Palette.MUTED, 0.5));
					y+=2;
				}
				drawRow(i, x, y, w, s.displayName, s.defaultAttribute, false, playerBuilder.getSkill(s), StatRules.SKILL_START_CAP, left);
				y++;
			}
		}
		default -> {}
		}
	}

	/** one row: label, optional attribute tag, then [-] pips [+] value grouped on the right; registers its click areas under index.
	 *  the attribute colors the tag and the filled pips; general skills hide the tag because their group header shows it */
	private void drawRow(int index, int x, int y, int w, String label, Attribute attribute, boolean showTag, int value, int cap, int left) {
		boolean focused = index == focus;
		int attributeColor = Palette.colorOf(attribute);
		if (focused) {
			canvas.drawText(x, y, FOCUS_INDICATOR, Palette.KEY);
		}
		canvas.drawText(x + 2, y, label, focused ? Palette.KEY : Palette.TEXT);
		if (showTag) {
			canvas.drawText(x + 22, y, attribute.name(), attributeColor);
		}

		//right-aligned group: [-] pips [+] value
		int valueX = x + w - 4;
		int plusX = valueX - 5;
		int pipsX = plusX - 2 - (cap * 2 - 1);
		int minusX = pipsX - 5;
		canvas.drawText(minusX, y, "[-]", value > 0 ? Palette.KEY : Palette.MUTED);
		for (int i = 0; i < cap; i++) {
			//a lower half block is a square half a row tall, so stacked rows stay visibly apart
			canvas.drawText(pipsX + i * 2, y, "\u2584", i < value ? Palette.KEY : PIP_EMPTY);
		}
		canvas.drawText(plusX, y, "[+]", left > 0 && value < cap ? Palette.KEY : Palette.MUTED);
		canvas.drawText(valueX, y, "%2d".formatted(value), Palette.TEXT);

		//specific areas first: the first matching area wins
		ui.addButtonBox(minusX, y, 3, 1, new Button(index, Action.MINUS));
		ui.addButtonBox(plusX, y, 3, 1, new Button(index, Action.PLUS));
		ui.addButtonBox(x, y, w, 1, new Button(index));
	}

	/** points left in this step, at the left of the action bar, level with the button labels */
	private void drawPointsLeft() {
		int left = remaining(step);
		if (left > 0) {
			canvas.drawText(DETAIL_X + 2, BUTTON_Y + 1, left + " / " + budget(step) + "  POINTS LEFT", Palette.TAG);
		} else {
			canvas.drawText(DETAIL_X + 2, BUTTON_Y + 1, "ALL POINTS SPENT", Palette.SUCCESS);
		}
	}

	private void drawDetails(Enum<?> row) {
		int x = DETAIL_X + 3;
		int width = DETAIL_W - 6;
		canvas.drawBox(DETAIL_X, PANEL_Y, DETAIL_W, DETAIL_H, Palette.MUTED);
		canvas.drawText(DETAIL_X + 2, PANEL_Y, " DETAILS ", Palette.KEY);

		int y = PANEL_Y + 2;
		switch (row) {
		case Attribute a -> {
			canvas.drawText(x, y, a.displayName.toUpperCase() + "  (" + a.name() + ")", Palette.colorOf(a));
			y = canvas.drawParagraph(x, y + 2, width, a.description, Palette.TEXT);
			canvas.drawText(x, y + 1, "In combat", Palette.TAG);
			canvas.drawParagraph(x, y + 2, width, a.combatRole, Palette.TEXT);
		}
		case Skill s -> {
			canvas.drawText(x, y, s.displayName.toUpperCase(), Palette.KEY);
			String group = s.group == Skill.Group.COMBAT ? "Combat skill" : "General skill";
			Attribute usual = s.defaultAttribute;
			canvas.drawText(x, y + 1, group, Palette.TAG);
			String prefix = "   Default Attribute ";
			canvas.drawText(x + group.length(), y + 1, prefix, Palette.MUTED);
			canvas.drawText(x + group.length() + prefix.length(), y + 1, usual.name() + " (" + playerBuilder.getAttribute(usual) + ")",
					Palette.colorOf(usual));
			canvas.drawParagraph(x, y + 3, width, s.description, Palette.TEXT);
		}
		default -> {}
		}
	}

	private void drawReview() {
		canvas.drawBox(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, Palette.MUTED);
		canvas.drawText(PANEL_X + 2, PANEL_Y, " REVIEW ", Palette.KEY);

		int x = PANEL_X + 4;
		int y = PANEL_Y + 2;
		canvas.drawText(x, y, playerBuilder.getName(), Palette.KEY);
		canvas.drawText(x, y + 1, "Level 1   Max HP " + StatRules.getPlayerMaxHp(1, playerBuilder.getAttribute(Attribute.CON)), Palette.TEXT);

		y += 3;
		canvas.drawText(x, y++, "ATTRIBUTES", Palette.TAG);
		for (Attribute a : Attribute.values()) {
			canvas.drawText(x, y, a.displayName, Palette.TEXT);
			canvas.drawText(x + 15, y++, "%2d".formatted(playerBuilder.getAttribute(a)), Palette.TEXT);
		}
		y++;
		canvas.drawText(x, y++, "COMBAT SKILLS", Palette.TAG);
		for (Skill s : skillsIn(Skill.Group.COMBAT)) {
			canvas.drawText(x, y++, "%-14s %2d".formatted(s.displayName, playerBuilder.getSkill(s)), Palette.TEXT);
		}

		int x2 = PANEL_X + 44;
		int y2 = PANEL_Y + 5;
		canvas.drawText(x2, y2++, "GENERAL SKILLS", Palette.TAG);
		for (Skill s : skillsIn(Skill.Group.GENERAL)) {
			if (playerBuilder.getSkill(s) > 0) {
				canvas.drawText(x2, y2++, "%-18s %2d".formatted(s.displayName, playerBuilder.getSkill(s)), Palette.TEXT);
			}
		}

		canvas.drawBox(DETAIL_X, PANEL_Y, DETAIL_W, DETAIL_H, Palette.MUTED);
		canvas.drawText(DETAIL_X + 2, PANEL_Y, " READY? ", Palette.KEY);
		canvas.drawParagraph(DETAIL_X + 3, PANEL_Y + 2, DETAIL_W - 6,
				"Confirm to begin your adventure. Use BACK to change any earlier step; your points are kept.", Palette.TEXT);
	}
}
