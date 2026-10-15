package com.endlessadventure.ui;

import java.util.List;
import java.util.Map;

import com.endlessadventure.entity.Attribute;

/** main menu screen: draws itself and reads input until the player picks an option */
public final class MainMenuScreen {
	public enum Choice {
		NEW_GAME("New Game"),
		LOAD_GAME("Load Game"),
		EXIT("Exit");

		private final String label;

		private Choice(String label) {
			this.label = label;
		}
	}

	//title bitmaps, each '#' is drawn as 2x2 pixels
	private static final String[] TITLE_ENDLESS = {
			"#####.#...#.####..#.....#####..####..####",
			"#.....##..#.#...#.#.....#.....#.....#....",
			"#.....##..#.#...#.#.....#.....#.....#....",
			"####..#.#.#.#...#.#.....####...###...###.",
			"#.....#..##.#...#.#.....#.........#.....#",
			"#.....#..##.#...#.#.....#.........#.....#",
			"#####.#...#.####..#####.#####.####..####."};
	private static final String[] TITLE_ADVENTURE = {
			".###..####..#...#.#####.#...#.#####.#...#.####..#####",
			"#...#.#...#.#...#.#.....##..#...#...#...#.#...#.#....",
			"#...#.#...#.#...#.#.....##..#...#...#...#.#...#.#....",
			"#####.#...#.#...#.####..#.#.#...#...#...#.####..####.",
			"#...#.#...#..#.#..#.....#..##...#...#...#.#.#...#....",
			"#...#.#...#..#.#..#.....#..##...#...#...#.#..#..#....",
			"#...#.####....#...#####.#...#...#....###..#...#.#####"};

	private static final Choice[] CHOICES = Choice.values();

	private final UIHandler ui;
	private final boolean hasSave;
	private int selected = 0;

	public MainMenuScreen(UIHandler ui, boolean hasSave) {
		this.ui = ui;
		this.hasSave = hasSave;
	}

	/** show the menu until the player picks an available option */
	public Choice run() {
		while (true) {
			draw();
			UIHandler.Input in = ui.readInput();
			int choice = -1;
			switch (in.inputKey()) {
			case UP -> selected = (selected + CHOICES.length - 1) % CHOICES.length;
			case DOWN -> selected = (selected + 1) % CHOICES.length;
			case ENTER -> choice = selected;
			case CHAR -> {
				int digit = in.ch() - '1';
				if (digit >= 0 && digit < CHOICES.length) {
					choice = digit;
				}
			}
			case CLICK -> {
				UIHandler.Button b = ui.getSelectedButton(in.x(), in.y());
				if (b != null) {
					choice = b.index();
				}
			}
			case HOVER -> {
				UIHandler.Button b = ui.getSelectedButton(in.x(), in.y());
				if (b != null) {
					selected = b.index();
				}
			}
			default -> {}
			}

			if (choice < 0) {
				continue;
			}
			if (CHOICES[choice] == Choice.LOAD_GAME && !hasSave) {
				ui.showWarning("No saved game yet.");
				continue;
			}
			return CHOICES[choice];
		}
	}

	private void draw() {
		Canvas canvas = ui.getCanvas();
		ui.clearButtons();
		canvas.clear(Palette.BG);

		int row = 20;
		canvas.drawPixelTitle(TITLE_ENDLESS, row, Palette.KEY, Palette.WARNING);
		canvas.drawPixelTitle(TITLE_ADVENTURE, row + 16, Palette.getAttributeColor(Attribute.DEX), Palette.getAttributeColor(Attribute.INT));

		row += TITLE_ENDLESS.length + TITLE_ADVENTURE.length - 3;
		canvas.drawTextCentered(row, "────────────────────  +  ────────────────────", Palette.MUTED, Palette.BG);

		int x = (Canvas.WIDTH / 2) - 7;
		row += 5;
		for (int i = 0; i < CHOICES.length; i++) {
			boolean enabled = CHOICES[i] != Choice.LOAD_GAME || hasSave; //Load Game is dimmed without saves
			boolean focused = i == selected;

			int y = row + i * 2;
			int width = 20;
			int rowBg = focused ? Palette.FOCUSED_BG : Palette.BG;

			canvas.drawText(x - 2, y, " ".repeat(width), rowBg, rowBg); //highlight bar
			canvas.drawText(x, y, "[" + (i + 1) + "]", enabled ? Palette.KEY : Palette.MUTED, rowBg);
			canvas.drawText(x + 5, y, CHOICES[i].label, enabled ? Palette.TEXT : Palette.MUTED, rowBg);
			ui.addButton(x - 2, y, width, 1, new UIHandler.Button(i, UIHandler.InputAction.SELECT));
		}

		canvas.drawFrame();
		canvas.drawHints(List.of(
				Map.entry("1-" + CHOICES.length, "select"),
				Map.entry("↑/↓", "move"),
				Map.entry("ENTER", "confirm"),
				Map.entry("Click", "select")));
		ui.present();
	}
}
