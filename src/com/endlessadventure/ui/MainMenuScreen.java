package com.endlessadventure.ui;

import java.util.List;
import java.util.Map;

/** main menu screen: draws itself and reads input until the player picks an option */
public final class MainMenuScreen extends Screen {
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
	
	public enum Choice {
		NEW_GAME("New Game"),
		LOAD_GAME("Load Game"),
		EXIT("Exit");

		public final String label;

		private Choice(String label) {
			this.label = label;
		}
	}
	
	private static final Choice[] CHOICES = Choice.values();

	private final boolean hasSave;
	private int focus = 0;

	public MainMenuScreen(UIHandler ui, boolean hasSave) {
		super(ui, null);
		this.hasSave = hasSave;
	}

	/** show the menu until the player picks an available option */
	public Choice run() {
		while (true) {
			drawFrame();
			UIHandler.Input in = ui.readInput();
			int choice = -1;
			switch (in.inputKey()) {
				case UP, DOWN, HOVER -> focus = moveFocus(in, focus, CHOICES.length);
				case ENTER -> choice = focus;
				case CHAR -> choice = in.digit(CHOICES.length);
				case CLICK -> {
					if (in.button() != null) {
						choice = in.button().index();
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

	private void drawFrame() {
		beginFrame();

		int y = 20;
		canvas.drawPixelTitle(TITLE_ENDLESS, y, Palette.KEY, Palette.WARNING);
		canvas.drawPixelTitle(TITLE_ADVENTURE, y + 16, Palette.mix(Palette.TEXT, Palette.TAG, 0.5), Palette.TAG);

		y += TITLE_ENDLESS.length + TITLE_ADVENTURE.length - 3;
		canvas.drawTextCentered(y, "────────────────────  +  ────────────────────", Palette.MUTED);

		int x = (Canvas.WIDTH / 2) - 9;
		y += 5;
		for (int i = 0; i < CHOICES.length; i++) {
			boolean enabled = CHOICES[i] != Choice.LOAD_GAME || hasSave; //Load Game is dimmed without saves
			drawListRow(i, x, y, 20, String.valueOf(i + 1), CHOICES[i].label, i == focus, enabled);
			y += 2;
		}

		endFrame(List.of(
				Map.entry("1-" + CHOICES.length, "Select"),
				Map.entry("↑/↓", "Move"),
				Map.entry("ENTER", "Confirm")));
	}
}
