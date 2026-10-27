package com.endlessadventure.ui;

import java.util.List;
import java.util.Map;

/** main menu screen: draws itself and reads input until the player picks an option */
public final class MainMenuScreen extends Screen {
	//title bitmaps: ENDLESS in a serif face drawn 2x2 pixels per '#', ADVENTURE in small spaced capitals 1 pixel per '#'
	private static final String[] TITLE_ENDLESS = {
			"######.###..###.#####...####...######..####...####.",
			".##..#..##...#...##..#...##.....##..#.##...#.##...#",
			".##.....###..#...##...#..##.....##....##.....##....",
			".##.#...#.##.#...##...#..##.....##.#..###....###...",
			".####...#..###...##...#..##.....####...####...####.",
			".##.#...#...##...##...#..##.....##.#.....###....###",
			".##.....#....#...##...#..##.....##........##.....##",
			".##..#..#....#...##..#...##..#..##..#.#...##.#...##",
			"######.###...#..#####...######.######..####...####."};
	private static final String[] TITLE_ADVENTURE = {
			".###.....####.....#...#....#####....#...#....#####....#...#....####.....#####",
			"#...#....#...#....#...#....#........##..#......#......#...#....#...#....#....",
			"#...#....#...#....#...#....#........##..#......#......#...#....#...#....#....",
			"#####....#...#....#...#....####.....#.#.#......#......#...#....####.....####.",
			"#...#....#...#.....#.#.....#........#..##......#......#...#....#.#......#....",
			"#...#....#...#.....#.#.....#........#..##......#......#...#....#..#.....#....",
			"#...#....####.......#......#####....#...#......#.......###.....#...#....#####"};
	//moonlit silver over ember, to match the backdrop's night sky and the town's warm windows
	private static final int TITLE_TOP = 0xEEF1F5, TITLE_BOTTOM = 0x98A6BA;
	private static final int SUBTITLE_TOP = 0xD99058, SUBTITLE_BOTTOM = 0xB25A34;
	//the backdrop fades to Palette.BG under the divider, the menu rows and the key hints; moving them means converting
	//the backdrop again with the new rows (tools/imagegen/assets.json, "main_menu")
	private static final int DIVIDER_Y = 29, MENU_Y = 34;

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
		super(ui);
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

		//the backdrop rests on the bottom border; its sky is Palette.BG, so the title and the text sit on it cleanly
		PixelImage.backdrop("main_menu").ifPresent(art -> canvas.drawImage(1, Canvas.HEIGHT * 2 - 2 - art.getHeight(), art));

		canvas.drawPixelTitle(TITLE_ENDLESS, 22, TITLE_TOP, TITLE_BOTTOM);
		canvas.drawPixelTitle(TITLE_ADVENTURE, 45, SUBTITLE_TOP, SUBTITLE_BOTTOM, 1);
		canvas.drawTextCentered(DIVIDER_Y, "────────────────────  ·  ────────────────────", Palette.LINE);
		canvas.drawTextCentered(DIVIDER_Y, "·", Palette.KEY);

		int x = (Canvas.WIDTH / 2) - 9;
		int y = MENU_Y;
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
