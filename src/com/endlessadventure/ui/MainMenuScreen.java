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

	private static final Choice[] CHOICES = Choice.values();

	public enum Choice {
		NEW_GAME("New Game"),
		LOAD_GAME("Load Game"),
		EXIT("Exit");

		private final String label;

		private Choice(String label) {
			this.label = label;
		}
	}

	private final boolean hasSave;
	private int selected = 0;

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
				case UP -> selected = Math.floorMod(selected - 1, CHOICES.length);
				case DOWN -> selected = Math.floorMod(selected + 1, CHOICES.length);
				case ENTER -> choice = selected;
				case CHAR -> {
					int digit = in.digit();
					if (digit >= 0 && digit < CHOICES.length) {
						choice = digit;
					}
				}
				case CLICK -> {
					UIHandler.Button b = in.button();
					if (b != null) {
						choice = b.index();
					}
				}
				case HOVER -> {
					UIHandler.Button b = in.button();
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

	private void drawFrame() {
		beginFrame();

		int y = 20;
		canvas.drawPixelTitle(TITLE_ENDLESS, y, Palette.KEY, Palette.WARNING);
		canvas.drawPixelTitle(TITLE_ADVENTURE, y + 16, Palette.mix(Palette.TEXT, Palette.TAG, 0.5), Palette.TAG);

		y += TITLE_ENDLESS.length + TITLE_ADVENTURE.length - 3;
		canvas.drawTextCentered(y, "────────────────────  +  ────────────────────", Palette.MUTED);

		int x = (Canvas.WIDTH / 2) - 7;
		y += 5;
		for (int i = 0; i < CHOICES.length; i++) {
			boolean enabled = CHOICES[i] != Choice.LOAD_GAME || hasSave; //Load Game is dimmed without saves
			boolean focused = i == selected;

			int width = 20;
			int rowBg = focused ? Palette.FOCUSED_BG : Palette.BG;

			canvas.drawText(x - 2, y, " ".repeat(width), rowBg, rowBg); //highlight bar
			canvas.drawText(x, y, "[" + (i + 1) + "]", enabled ? Palette.KEY : Palette.MUTED, rowBg);
			canvas.drawText(x + 5, y, CHOICES[i].label, enabled ? Palette.TEXT : Palette.MUTED, rowBg);
			ui.addButton(x - 2, y, width, 1, new UIHandler.Button(i));
			
			y+=2;
		}

		canvas.drawHints(List.of(
				Map.entry("1-" + CHOICES.length, "Select"),
				Map.entry("↑/↓", "Move"),
				Map.entry("ENTER", "Confirm")));
		ui.present();
	}
}
