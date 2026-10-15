package com.endlessadventure.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Canvas {
	public static final int HEIGHT = 51;
	public static final int WIDTH = 207;
	public static final int BUTTON_HEIGHT = 3;
	public static final int BUTTON_WIDTH = 18;
	
	//ANSI escape codes
	public static final String ESC = "\033[";
	public static final int FOREGROUND = 38;
	public static final int BACKGROUND = 48;
	
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
	void clear(int bg) {
		for (int i = 0; i < HEIGHT; i++) {
			for (int j = 0; j < WIDTH; j++) {
				set(j, i, ' ', bg, bg);
			}
		}
	}

	/**
	 * draw the given string in the canvas grid at x,y cut off at edge
	 */
	void drawText(int x, int y, String s, int fg, int bg) {
		for (int i = 0; i < s.length() && x + i < WIDTH; i++) {
			set(x + i, y, s.charAt(i), fg, bg);
		}
	}
	
	/** write text centered horizontally on canvas row y */
	void drawTextCentered(int y, String text, int fg, int bg) {
		drawText((Canvas.WIDTH - text.length()) / 2, y, text, fg, bg);
	}
	
	/** draw "[key] description" hints on the bottom border, left to right */
	void drawHints(List<Map.Entry<String, String>> hints) {
		int x = 2;
		for (Map.Entry<String, String> hint : hints) {
			String key = " [" + hint.getKey() + "] ";
			String value = hint.getValue() + "   ";
			drawText(x, HEIGHT - 1, key, Palette.KEY, Palette.BG);
			drawText(x + key.length(), HEIGHT - 1, value, Palette.MUTED, Palette.BG);
			x += key.length() + value.length();
		}
	}

	/** draws a box border at x,y with width w and height h */
	void drawBox(int x, int y, int w, int h, int fg, int bg) {
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
	
	/** draw the border around the whole canvas */
	void drawFrame() {
		drawBox(0, 0, WIDTH, HEIGHT, Palette.MUTED, Palette.BG);
	}

	/** draw a 3-row boxed button with a centered label; the border turns gold when highlighted */
	void drawButton(int x, int y, int w, String label, boolean enabled, boolean highlighted) {
		int border = highlighted && enabled ? Palette.KEY : Palette.MUTED;
		drawBox(x, y, w, 3, border, Palette.BG);
		drawText(x + (w - label.length()) / 2, y + 1, label, enabled ? Palette.TEXT : Palette.MUTED, Palette.BG);
	}
	
	//pixel drawing
	/** draw the pixel at px,py */
	void drawPixel(int px, int py, int rgb) {
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
	
	/** draw a '#' bitmap centered horizontally at pixel row py0, with a shadow one pixel down-right;
	 *  each '#' becomes 2x2 pixels, colored from topColor to bottomColor */
	void drawPixelTitle(String[] bitmap, int py0, int topColor, int bottomColor) {
		int px0 = (WIDTH - bitmap[0].length() * 2) / 2;
		int shadow = Palette.mix(bottomColor, Palette.BG, 0.75);
		drawBitmap(bitmap, px0 + 1, py0 + 1, shadow, shadow); //drop shadow
		drawBitmap(bitmap, px0, py0, topColor, bottomColor);
	}
	
	/** draw each '#' of the bitmap as 2x2 pixels, colored from top to bottom as a vertical gradient */
	private void drawBitmap(String[] bitmap, int px0, int py0, int topColor, int bottomColor) {
		for (int row = 0; row < bitmap.length; row++) {
			for (int col = 0; col < bitmap[row].length(); col++) {
				if (bitmap[row].charAt(col) != '#') {
					continue;
				}
				for (int dy = 0; dy < 2; dy++) {
					for (int dx = 0; dx < 2; dx++) {
						int py = row * 2 + dy;
						double ratio = (double) py / (bitmap.length * 2 - 1); //ratio of current pixel y to last row
						drawPixel(px0 + col * 2 + dx, py0 + py, Palette.mix(topColor, bottomColor, ratio));
					}
				}
			}
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
					sb.append(Palette.rgbToAnsi(FOREGROUND, foreground[y][x]));
					lastFg = foreground[y][x];
				}
				if (background[y][x] != lastBg) {
					sb.append(Palette.rgbToAnsi(BACKGROUND, background[y][x]));
					lastBg = background[y][x];
				}
				sb.append(characters[y][x]);
			}
		}
		return sb.append(ESC).append("0m").toString();
	}

	/** draws wrapped text and returns the row after the last line */
	int drawParagraph(int x, int y, int width, String text, int fg, int bg) {
		for (String line : wrapText(text, width)) {
			drawText(x, y++, line, fg, bg);
		}
		return y;
	}

	/** split text into lines of at most width characters, breaking at spaces; longer words are cut */
	private static List<String> wrapText(String text, int width) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.strip().split("\\s+")) {
			while (word.length() > width) {
				if (!line.isEmpty()) {
					lines.add(line.toString());
					line.setLength(0);
				}
				lines.add(word.substring(0, width));
				word = word.substring(width);
			}
			if (word.isEmpty()) {
				continue;
			}
			if (!line.isEmpty() && line.length() + 1 + word.length() > width) {
				lines.add(line.toString());
				line.setLength(0);
			}
			if (!line.isEmpty()) {
				line.append(' ');
			}
			line.append(word);
		}
		if (!line.isEmpty()) {
			lines.add(line.toString());
		}
		return lines;
	}
}
