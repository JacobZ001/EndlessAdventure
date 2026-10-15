package com.endlessadventure.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.endlessadventure.StatRules;
import com.endlessadventure.entity.Attribute;
import com.endlessadventure.entity.Player;
import com.endlessadventure.entity.Skill;

/** step-by-step character creation: name, attributes, combat skills, general skills, review */
public final class CharacterCreationScreen {
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

	private final UIHandler ui;
	private final Canvas canvas;
	private Player.Builder builder;
	private int step = ATTRIBUTES;
	private int focus = 0; //focused row

	public CharacterCreationScreen(UIHandler ui) {
		this.ui = ui;
		canvas = ui.getCanvas();
	}

	/** returns the new player, or null if the player left for the main menu */
	public Player run() {
		while (true) {
			if (builder == null && !askName()) {
				return null;
			}
			Player player = allocate();
			if (player != null) {
				return player;
			}
			builder = null; //backed out of the first step: ask for the name again
		}
	}

	/** asks for a name until it is valid; returns false if the player left it empty */
	private boolean askName() {
		while (true) {
			ui.clearButtons();
			canvas.clear(Palette.BG);
			canvas.drawFrame();
			canvas.drawText(2, 0, " CREATE YOUR CHARACTER ", Palette.KEY, Palette.BG);
			canvas.drawTextCentered(NAME_BOX_Y - 3, "What is your name, adventurer?", Palette.TEXT, Palette.BG);
			canvas.drawBox(NAME_BOX_X, NAME_BOX_Y, NAME_BOX_W, 3, Palette.KEY, Palette.BG);
			canvas.drawTextCentered(NAME_BOX_Y + 4, "Leave it empty to return to the main menu.", Palette.MUTED, Palette.BG);
			canvas.drawHints(List.of(Map.entry("ENTER", "confirm")));
			ui.present();

			String input = ui.promptAt(NAME_BOX_X + 2, NAME_BOX_Y + 1, "", true);
			if (input.isEmpty()) {
				return false;
			}
			try {
				builder = new Player.Builder(input);
				step = ATTRIBUTES;
				focus = 0;
				return true;
			} catch (IllegalArgumentException e) {
				ui.showWarning(e.getMessage());
			}
		}
	}

	/** runs the allocation steps; returns the built player, or null if the player backed out of the first step */
	private Player allocate() {
		while (true) {
			draw();
			UIHandler.Input in = ui.readInput();
			List<Enum<?>> rows = rows();
			switch (in.inputKey()) {
			case UP -> focus = Math.max(0, focus - 1);
			case DOWN -> focus = Math.min(rows.size() - 1, focus + 1);
			case LEFT -> adjustStat(rows, focus, -1);
			case RIGHT -> adjustStat(rows, focus, +1);
			case ENTER -> {
				Player p = next();
				if (p != null) return p;
			}
			case ESC -> {
				if (!back()) return null;
			}
			case HOVER -> {
				UIHandler.Button b = ui.getSelectedButton(in.x(), in.y());
				if (b != null && b.index() < rows.size()) focus = b.index();
			}
			case CLICK -> {
				UIHandler.Button b = ui.getSelectedButton(in.x(), in.y());
				if (b == null) {
					break;
				}
				if (b.index() == rows.size()) { //BACK
					if (!back()) return null;
				} else if (b.index() == rows.size() + 1) { //NEXT or CONFIRM
					Player p = next();
					if (p != null) return p;
				} else {
					focus = b.index();
					switch (b.inputAction()) {
					case MINUS -> adjustStat(rows, focus, -1);
					case PLUS -> adjustStat(rows, focus, +1);
					case SELECT -> {}
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
			Player player = builder.build();
			ui.showSuccess("Starting your adventure...");
			draw(); //keep this screen up with the message while the story request runs
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
				if (delta > 0) builder.incrAttribute(a); else builder.decrAttribute(a);
			}
			case Skill s -> {
				if (delta > 0) builder.incrSkill(s); else builder.decrSkill(s);
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
			if (s.getGroup() == group) skills.add(s);
		}
		return skills;
	}

	private int remaining(int step) {
		return switch (step) {
			case ATTRIBUTES -> builder.getRemainingAttributePoints();
			case COMBAT -> builder.getRemainingSkillPoints(Skill.Group.COMBAT);
			case GENERAL -> builder.getRemainingSkillPoints(Skill.Group.GENERAL);
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

	private int maxHp() {
		return StatRules.getPlayerMaxHp(1, builder.getAttribute(Attribute.CON));
	}

	
	//drawing
	private void draw() {
		ui.clearButtons();
		canvas.clear(Palette.BG);
		
		canvas.drawFrame();
		canvas.drawText(2, 0, " CREATE YOUR CHARACTER ", Palette.KEY, Palette.BG);
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
		//buttons come after the rows so their indices are rows.size() and rows.size() + 1
		boolean complete = remaining(step) == 0;
		canvas.drawButton(BACK_X, BUTTON_Y, Canvas.BUTTON_WIDTH, "BACK", true, false);
		ui.addButton(BACK_X, BUTTON_Y, Canvas.BUTTON_WIDTH, Canvas.BUTTON_HEIGHT, new UIHandler.Button(rows.size(), UIHandler.InputAction.SELECT));
		canvas.drawButton(NEXT_X, BUTTON_Y, Canvas.BUTTON_WIDTH, step == REVIEW ? "CONFIRM" : "NEXT", complete, true);
		ui.addButton(NEXT_X, BUTTON_Y, Canvas.BUTTON_WIDTH, Canvas.BUTTON_HEIGHT, new UIHandler.Button(rows.size() + 1, UIHandler.InputAction.SELECT));
		if (step != REVIEW) {
			drawPointsLeft();
		}

		
		canvas.drawHints(List.of(
				Map.entry("↑/↓", "move"),
				Map.entry("←/→", "- / +"),
				Map.entry("Click", "select"),
				Map.entry("ENTER", step == REVIEW ? "confirm" : "next"),
				Map.entry("ESC", "back")));
		ui.present();
	}

	private void drawStepBar() {
		int x = PANEL_X + 2;
		for (int i = 0; i < STEPS.length; i++) {
			int color = i == step ? Palette.KEY : (i < step ? Palette.SUCCESS : Palette.MUTED);
			String label = (i + 1) + "  " + STEPS[i];
			canvas.drawText(x, STEP_BAR_Y, label, color, Palette.BG);
			x += label.length();
			if (i < STEPS.length - 1) {
				canvas.drawText(x + 2, STEP_BAR_Y, "──────", Palette.MUTED, Palette.BG);
				x += 10;
			}
		}
		String name = builder.getName();
		canvas.drawText(DETAIL_X + DETAIL_W - 2 - name.length(), STEP_BAR_Y, name, Palette.TEXT, Palette.BG);
	}

	private void drawAllocation(List<Enum<?>> rows) {
		int left = remaining(step);
		canvas.drawBox(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, Palette.MUTED, Palette.BG);
		canvas.drawText(PANEL_X + 2, PANEL_Y, " " + STEPS[step] + " ", Palette.KEY, Palette.BG);

		int x = PANEL_X + 2;
		int w = PANEL_W - 4;
		switch (step) {
		case ATTRIBUTES -> {
			canvas.drawText(x + 2, PANEL_Y + 2, "Spend " + budget(step) + " points. An attribute starts at most at "
					+ StatRules.ATTRIBUTE_START_CAP + ".", Palette.MUTED, Palette.BG);
			for (int i = 0; i < rows.size(); i++) {
				Attribute a = (Attribute) rows.get(i);
				drawRow(i, x, PANEL_Y + 5 + i * 3, w, a.getDisplayName(), a, true, builder.getAttribute(a),
						StatRules.ATTRIBUTE_START_CAP, left);
			}
		}
		case COMBAT -> {
			canvas.drawText(x + 2, PANEL_Y + 2, "Spend " + budget(step) + " points on fighting skills, up to "
					+ StatRules.SKILL_START_CAP + " each.", Palette.MUTED, Palette.BG);
			for (int i = 0; i < rows.size(); i++) {
				Skill s = (Skill) rows.get(i);
				drawRow(i, x, PANEL_Y + 5 + i * 3, w, s.getDisplayName(), s.getDefaultAttribute(), true, builder.getSkill(s),
						StatRules.SKILL_START_CAP, left);
			}
		}
		case GENERAL -> {
			canvas.drawText(x + 2, PANEL_Y + 2, "Spend " + budget(step) + " points, up to " + StatRules.SKILL_START_CAP
					+ " each. Group headers show your matching attribute.", Palette.MUTED, Palette.BG);
			int y = PANEL_Y + 4;
			Attribute group = null;
			for (int i = 0; i < rows.size(); i++) {
				Skill s = (Skill) rows.get(i);
				if (s.getDefaultAttribute() != group) {
					group = s.getDefaultAttribute();
					canvas.drawText(x + 2, ++y, group.name() + "  " + builder.getAttribute(group), Palette.getAttributeColor(group), Palette.BG);
					canvas.drawText(x + 10, y, "─".repeat(w - 14), Palette.mix(Palette.BG, Palette.MUTED, 0.5), Palette.BG);
					y+=2;
				}
				drawRow(i, x, y, w, s.getDisplayName(), s.getDefaultAttribute(), false, builder.getSkill(s), StatRules.SKILL_START_CAP, left);
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
		int bg = focused ? Palette.FOCUSED_BG : Palette.BG;
		int attributeColor = Palette.getAttributeColor(attribute);
		canvas.drawText(x, y, " ".repeat(w), bg, bg);
		canvas.drawText(x + 2, y, label, focused ? Palette.KEY : Palette.TEXT, bg);
		if (showTag) {
			canvas.drawText(x + 22, y, attribute.name(), attributeColor, bg);
		}

		//right-aligned group: [-] pips [+] value
		int valueX = x + w - 4;
		int plusX = valueX - 5;
		int pipsX = plusX - 2 - (cap * 2 - 1);
		int minusX = pipsX - 5;
		canvas.drawText(minusX, y, "[-]", value > 0 ? Palette.KEY : Palette.MUTED, bg);
		for (int i = 0; i < cap; i++) {
			//a lower half block is a square half a row tall, so stacked rows stay visibly apart
			canvas.drawText(pipsX + i * 2, y, "\u2584", i < value ? Palette.KEY : PIP_EMPTY, bg);
		}
		canvas.drawText(plusX, y, "[+]", left > 0 && value < cap ? Palette.KEY : Palette.MUTED, bg);
		canvas.drawText(valueX, y, "%2d".formatted(value), Palette.TEXT, bg);

		//specific areas first: the first matching area wins
		ui.addButton(minusX, y, 3, 1, new UIHandler.Button(index, UIHandler.InputAction.MINUS));
		ui.addButton(plusX, y, 3, 1, new UIHandler.Button(index, UIHandler.InputAction.PLUS));
		ui.addButton(x, y, w, 1, new UIHandler.Button(index, UIHandler.InputAction.SELECT));
	}

	/** points left in this step, at the left of the action bar, level with the button labels */
	private void drawPointsLeft() {
		int left = remaining(step);
		if (left > 0) {
			canvas.drawText(DETAIL_X + 2, BUTTON_Y + 1, left + " / " + budget(step) + "  POINTS LEFT", Palette.KEY, Palette.BG);
		} else {
			canvas.drawText(DETAIL_X + 2, BUTTON_Y + 1, "ALL POINTS SPENT", Palette.SUCCESS, Palette.BG);
		}
	}

	private void drawDetails(Enum<?> row) {
		int x = DETAIL_X + 3;
		int width = DETAIL_W - 6;
		canvas.drawBox(DETAIL_X, PANEL_Y, DETAIL_W, DETAIL_H, Palette.MUTED, Palette.BG);
		canvas.drawText(DETAIL_X + 2, PANEL_Y, " DETAILS ", Palette.KEY, Palette.BG);

		int y = PANEL_Y + 2;
		switch (row) {
		case Attribute a -> {
			canvas.drawText(x, y, a.getDisplayName().toUpperCase() + "  (" + a.name() + ")", Palette.getAttributeColor(a), Palette.BG);
			y = canvas.drawParagraph(x, y + 2, width, a.getDescription(), Palette.TEXT, Palette.BG);
			canvas.drawText(x, y + 1, "In combat", Palette.TAG, Palette.BG);
			canvas.drawParagraph(x, y + 2, width, a.getCombatRole(), Palette.TEXT, Palette.BG);
		}
		case Skill s -> {
			canvas.drawText(x, y, s.getDisplayName().toUpperCase(), Palette.KEY, Palette.BG);
			String group = s.getGroup() == Skill.Group.COMBAT ? "Combat skill" : "General skill";
			Attribute usual = s.getDefaultAttribute();
			canvas.drawText(x, y + 1, group, Palette.TAG, Palette.BG);
			String prefix = "   Default Attribute ";
			canvas.drawText(x + group.length(), y + 1, prefix, Palette.MUTED, Palette.BG);
			canvas.drawText(x + group.length() + prefix.length(), y + 1, usual.name() + " (" + builder.getAttribute(usual) + ")",
					Palette.getAttributeColor(usual), Palette.BG);
			canvas.drawParagraph(x, y + 3, width, s.getDescription(), Palette.TEXT, Palette.BG);
		}
		default -> {}
		}
	}

	private void drawReview() {
		canvas.drawBox(PANEL_X, PANEL_Y, PANEL_W, PANEL_H, Palette.MUTED, Palette.BG);
		canvas.drawText(PANEL_X + 2, PANEL_Y, " REVIEW ", Palette.KEY, Palette.BG);

		int x = PANEL_X + 4;
		int y = PANEL_Y + 2;
		canvas.drawText(x, y, builder.getName(), Palette.KEY, Palette.BG);
		canvas.drawText(x, y + 1, "Level 1   Max HP " + maxHp(), Palette.TEXT, Palette.BG);

		y += 3;
		canvas.drawText(x, y++, "ATTRIBUTES", Palette.TAG, Palette.BG);
		for (Attribute a : Attribute.values()) {
			canvas.drawText(x, y, a.getDisplayName(), Palette.getAttributeColor(a), Palette.BG);
			canvas.drawText(x + 15, y++, "%2d".formatted(builder.getAttribute(a)), Palette.TEXT, Palette.BG);
		}
		y++;
		canvas.drawText(x, y++, "COMBAT SKILLS", Palette.TAG, Palette.BG);
		for (Skill s : skillsIn(Skill.Group.COMBAT)) {
			canvas.drawText(x, y++, "%-14s %2d".formatted(s.getDisplayName(), builder.getSkill(s)), Palette.TEXT, Palette.BG);
		}

		int x2 = PANEL_X + 44;
		int y2 = PANEL_Y + 5;
		canvas.drawText(x2, y2++, "GENERAL SKILLS", Palette.TAG, Palette.BG);
		for (Skill s : skillsIn(Skill.Group.GENERAL)) {
			if (builder.getSkill(s) > 0) {
				canvas.drawText(x2, y2++, "%-18s %2d".formatted(s.getDisplayName(), builder.getSkill(s)), Palette.TEXT, Palette.BG);
			}
		}

		canvas.drawBox(DETAIL_X, PANEL_Y, DETAIL_W, DETAIL_H, Palette.MUTED, Palette.BG);
		canvas.drawText(DETAIL_X + 2, PANEL_Y, " READY? ", Palette.KEY, Palette.BG);
		canvas.drawParagraph(DETAIL_X + 3, PANEL_Y + 2, DETAIL_W - 6,
				"Confirm to begin your adventure. Use BACK to change any earlier step; your points are kept.", Palette.TEXT, Palette.BG);
	}
}
