package com.endlessadventure.ui;

public class Canvas {
	public static final int HEIGHT = 48;
	public static final int WIDTH = 207;
	
	//ANSI escape codes
	private static final String ESC = "\033[";
	
	//Canvas component ASCII	
	private static final char[] CORNERS = {'┌','┐',
		         						   '└','┘'};
	private static final char ROW_BORDER = '─';
	private static final char COL_BORDER = '│';
	
	private final char[][] characters = new char[HEIGHT][WIDTH];
	private final int[][] foreground = new int[HEIGHT][WIDTH];
	private final int[][] background = new int[HEIGHT][WIDTH];

	/** local helper method to set one character in canvas grid at x,y */
	private void set(int x, int y, char c, int fg, int bg) {
		if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
			return;
		}
		characters[y][x] = c;
		foreground[y][x] = fg;
		background[y][x] = bg;
	}

	/** clear the whole canvas with whitespace with bg as foreground and background */
	public void clear(int bg) {
		for (int i = 0; i < HEIGHT; i++) {
			for (int j = 0; j < WIDTH; j++) {
				set(j, i, ' ', bg, bg);
			}
		}
	}

	/**
	 * draw the given string in the canvas grid at x,y cut off at edge
	 */
	public void drawText(int x, int y, String s, int fg, int bg) {
		for (int i = 0; i < s.length() && x + i < WIDTH; i++) {
			set(x + i, y, s.charAt(i), fg, bg);
		}
	}
	
	/** write text centered horizontally on canvas row y */
	public void drawTextCentered(int y, String text, int fg, int bg) {
		drawText((Canvas.WIDTH - text.length()) / 2, y, text, fg, bg);
	}

	/** draws a box border at x,y with width w and height h */
	public void drawBox(int x, int y, int w, int h, int fg, int bg) {
		if (x < 0 || y < 0 || w < 0 || h < 0 || x + w > WIDTH || y + h > HEIGHT) {
			throw new IllegalArgumentException(
					"Illegal dimensions, box out of bound: (%d,%d) (%d, %d)".formatted(x, y, w, h));
		}
		for (int i = y; i < y + h; i++) {
			if (i == y || i == y + h - 1) {
				for (int j = x; j < x + w; j++) {
					if (i == y && j == x) {
						set(j, i, CORNERS[0], fg, bg);
					} else if (i == y && j == x + w - 1) {
						set(j, i, CORNERS[1], fg, bg);
					} else if (i == y + (h - 1) && j == x) {
						set(j, i, CORNERS[2], fg, bg);
					} else if (i == y + (h - 1) && j == x + w - 1) {
						set(j, i, CORNERS[3], fg, bg);
					} else {
						set(j, i, ROW_BORDER, fg, bg);
					}
				}
			} else {
				set(x, i, COL_BORDER, fg, bg);
				set(x + w - 1, i, COL_BORDER, fg, bg);
			}
		}
	}

	/** draw the pixel at px,py */
	public void drawPixel(int px, int py, int rgb) {
		int y = py / 2;
		if (px < 0 || px >= WIDTH || py < 0 || y >= HEIGHT) {
			return;
		}
		if (characters[y][px] != '▀') {
			characters[y][px] = '▀';
			foreground[y][px] = background[y][px];
		}
		if (py % 2 == 0) {
			foreground[y][px] = rgb;
		} else {
			background[y][px] = rgb;
		}
	}

	/** convert the canvas into a string block for rendering */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(WIDTH * HEIGHT * 4);
		int lastFg = -1;
		int lastBg = -1;

		for (int y = 0; y < HEIGHT; y++) {
			//relocate cursor to new line
			//does not use \n to avoid undesired terminal screen scroll 
			sb.append(ESC).append(y + 1).append(";1H"); 
			for (int x = 0; x < WIDTH; x++) {
				if (foreground[y][x] != lastFg) {
					appendColor(sb, 38, foreground[y][x]);
					lastFg = foreground[y][x];
				}
				if (background[y][x] != lastBg) {
					appendColor(sb, 48, background[y][x]);
					lastBg = background[y][x];
				}
				sb.append(characters[y][x]);
			}
		}
		return sb.append(ESC).append("0m").toString();
	}

	/** helper function to append color from a rgb value
	 *  sets the following characters to this color */
	private void appendColor(StringBuilder sb, int type, int color) {
		sb.append(ESC).append(type).append(";2;")
				.append(Palette.red(color)).append(';')
				.append(Palette.green(color)).append(';')
				.append(Palette.blue(color)).append('m');
	}
}
