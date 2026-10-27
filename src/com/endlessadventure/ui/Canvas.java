package com.endlessadventure.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.endlessadventure.Displayable.Span;

public class Canvas {
	public static final int HEIGHT = 51;
	public static final int WIDTH = 209;
	public static final int BUTTON_HEIGHT = 3;
	public static final int BUTTON_WIDTH = 18;

	//ANSI escape codes
	public static final String ESC = "\033[";
	public static final int FOREGROUND = 38;
	public static final int BACKGROUND = 48;

	//Canvas component ASCII	
	private static final char[] CORNERS = {'┌','┐',
										   '└','┘'};
	private static final int SHADOW = 0x030403;	//ground shadows pull colors toward this
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
		fill(0, 0, WIDTH, HEIGHT, bg);
	}

	/** fills the area starting from {@code x, y} to {@code x + w, y + h} with whitespace with {@code c} */
	void fill(int x, int y, int width, int height, int color) {
		for (int i = y; i < y + height; i++) {
			for (int j = x; j < x + width; j++) {
				set(j, i, ' ', color, color);
			}
		}
	}

	/** scrim: pull the colors of every cell in the rectangle toward the background */
	void dim(int x, int y, int width, int height) {
		for (int i = Math.max(0, y); i < Math.min(HEIGHT, y + height); i++) {
			for (int j = Math.max(0, x); j < Math.min(WIDTH, x + width); j++) {
				set(j, i, characters[i][j], Palette.mix(foreground[i][j], Palette.BG, 0.55), Palette.mix(background[i][j], Palette.BG, 0.55));
			}
		}
	}

	/** overload of dim that defaults to dim the whole canvas */
	void dim() {
		dim(0, 0, WIDTH, HEIGHT);
	}

	/** draws a box border at x,y with width w and height h */
	void drawBox(int x, int y, int width, int height, int fg, int bg) {
		if (x < 0 || y < 0 || width < 0 || height < 0 || x + width > WIDTH || y + height > HEIGHT) {
			throw new IllegalArgumentException(
					"Illegal dimensions: box out of bound at (%d,%d), W = %d H = %d)".formatted(x, y, width, height));
		}
		for (int i = y; i < y + height; i++) {
			if (i == y || i == y + height - 1) {
				for (int j = x; j < x + width; j++) {
					if (i == y && j == x) {
						set(j, i, CORNERS[0], fg, bg);
					} else if (i == y && j == x + width - 1) {
						set(j, i, CORNERS[1], fg, bg);
					} else if (i == y + (height - 1) && j == x) {
						set(j, i, CORNERS[2], fg, bg);
					} else if (i == y + (height - 1) && j == x + width - 1) {
						set(j, i, CORNERS[3], fg, bg);
					} else {
						set(j, i, ROW_BORDER, fg, bg);
					}
				}
			} else {
				set(x, i, COL_BORDER, fg, bg);
				set(x + width - 1, i, COL_BORDER, fg, bg);
			}
		}
	}

	/** overload of drawBox() with default bg*/
	void drawBox(int x, int y, int width, int height, int fg) {
		drawBox(x, y, width, height, fg, background[y][x]);
	}

	/** draw a box border with title and default bg */
	void drawTitledBox(int x, int y, int width, int height, int fg, String title) {
		drawBox(x, y, width, height, fg);
		drawText(x + 2, y, " " + title + " ", Palette.TEXT);
	}


	/** draw the border around the whole canvas */
	void drawScreenBorder() {
		drawBox(0, 0, WIDTH, HEIGHT, Palette.LINE);
	}

	/** draw a divider from x,y to x+w, y*/
	void drawDivider(int x, int y, int width) {
		for(int i = 0; i < width; i++) {
			set(x + i, y, ROW_BORDER, Palette.LINE, background[y][x]);
		}
	}

	/** draw the given string in the canvas grid at x,y cut off at edge */
	void drawText(int x, int y, String text, int fg, int bg) {
		for (int i = 0; i < text.length() && x + i < WIDTH; i++) {
			set(x + i, y, text.charAt(i), fg, bg);
		}
	}

	/** overload of drawText without altering the bg*/
	void drawText(int x, int y, String text, int fg) {
		for (int i = 0; i < text.length() && x + i < WIDTH; i++) {
			set(x + i, y, text.charAt(i), fg, background[y][x + i]);
		}
	}

	/** write text centered horizontally on canvas row y */
	void drawTextCentered(int y, String text, int fg) {
		drawText((Canvas.WIDTH - text.length()) / 2, y, text, fg);
	}

	/** draws wrapped text and returns the row after the last line */
	int drawParagraph(int x, int y, int width, String text, int fg) {
		for (String line : wrapText(text, width)) {
			drawText(x, y++, line, fg);
		}
		return y;
	}
	
	/** the spans as one paragraph, wrapped at spaces within width; returns the row after it */
	int drawSpans(int x, int y, int width, List<Span> spans) {
		int col = 0;
		for (Span s : spans) {
			int color = Palette.colorOf(s.meaning());
			for (String word : s.text().split("(?<= )")) { //split after each space, so every word keeps its space
				if (col > 0 && col + word.stripTrailing().length() > width) {
					y++;
					col = 0;
				}
				drawText(x + col, y, word, color);
				col += word.length();
			}
		}
		return y + 1;
	}

	/** split text into lines of at most width characters, breaking at spaces; longer words are cut */
	static List<String> wrapText(String text, int width) {
		if(text.isBlank()) {
			return List.of("");
		}
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

	/** draw "[key] description" hints on the bottom border, left to right */
	void drawHints(List<Map.Entry<String, String>> hints) {
		int x = 2;
		for (Map.Entry<String, String> hint : hints) {
			String key = " [" + hint.getKey() + "] ";
			String value = hint.getValue() + "   ";
			drawText(x, HEIGHT - 1, key, Palette.KEY);
			drawText(x + key.length(), HEIGHT - 1, value, Palette.MUTED);
			x += key.length() + value.length();
		}
	}

	/** draw a boxed button with a centered label; the border turns gold when highlighted */
	void drawButton(int x, int y, String label, boolean enabled, boolean highlighted) {
		int border = highlighted && enabled ? Palette.KEY : Palette.MUTED;
		drawBox(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, border);
		drawText(x + (BUTTON_WIDTH - label.length()) / 2, y + 1, label, enabled ? highlighted? Palette.KEY : Palette.TEXT : Palette.MUTED);
	}

	//pixel drawing
	/** draw the image with its top-left sample at pixel px,py (one character column per sample, two samples per row);
	 *  transparent samples leave what is underneath, so images can be layered, e.g. a sprite over map ground */
	void drawImage(int px, int py, PixelImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int color = image.getColor(x, y);
				if (color >= 0) {
					drawPixel(px + x, py + y, color);
				}
			}
		}
	}

	/** the color showing at pixel px,py: a half of a '▀' cell, or the background of any other cell; -1 off the canvas */
	int getPixel(int px, int py) {
		int y = py / 2;
		if (px < 0 || px >= WIDTH || py < 0 || y >= HEIGHT) {
			return -1;
		}
		return characters[y][px] == '▀' && py % 2 == 0 ? foreground[y][px] : background[y][px];
	}

	/** pull the pixel at px,py toward black by amount (0 to 1), e.g. for a shadow on the ground */
	void shadePixel(int px, int py, double amount) {
		int color = getPixel(px, py);
		if (color >= 0) {
			drawPixel(px, py, Palette.mix(color, SHADOW, amount));
		}
	}

	/** a top-light contact shadow for an image whose bottom row stands at pixel row footPy: an oval three rows high,
	 *  as wide as what the figure stands on (its bottom share of rows), not its widest part. the middle row reaches one
	 *  pixel past the feet on each side; the rows above and below are as wide as the feet and a little lighter.
	 *  draw it before the image, so the image covers the part behind the feet */
	void drawContactShadow(int px, int footPy, PixelImage image, double share) {
		int[] foot = image.footprint(share);
		if (foot[0] < 0) {
			return;
		}
		for (int x = foot[0] - 1; x <= foot[1] + 1; x++) {
			boolean edge = x < foot[0] || x > foot[1];
			shadePixel(px + x, footPy, edge ? 0.5 : 0.66);
		}
		for (int dy : new int[] {-1, 1}) {
			for (int x = foot[0]; x <= foot[1]; x++) {
				boolean edge = x == foot[0] || x == foot[1];
				shadePixel(px + x, footPy + dy, edge ? 0.42 : 0.56);
			}
		}
	}

	/** draw the pixel at px,py */
	void drawPixel(int px, int py, int color) {
		int y = py / 2;
		if (px < 0 || px >= WIDTH || py < 0 || y >= HEIGHT) {
			return;
		}
		if (characters[y][px] != '▀') {
			characters[y][px] = '▀';
			foreground[y][px] = background[y][px];
		}
		if (py % 2 == 0) {
			foreground[y][px] = color;
		} else {
			background[y][px] = color;
		}
	}

	/** draw a '#' bitmap centered horizontally at pixel row py0, with a shadow one pixel down-right;
	 *  each '#' becomes 2x2 pixels, colored from topColor to bottomColor */
	void drawPixelTitle(String[] bitmap, int py0, int topColor, int bottomColor) {
		drawPixelTitle(bitmap, py0, topColor, bottomColor, 2);
	}

	/** same with each '#' as scale x scale pixels; at scale 1 the strokes are a single pixel wide, too thin for a shadow */
	void drawPixelTitle(String[] bitmap, int py0, int topColor, int bottomColor, int scale) {
		int px0 = (WIDTH - bitmap[0].length() * scale) / 2;
		if (scale > 1) {
			int shadow = Palette.mix(bottomColor, Palette.BG, 0.75);
			drawBitmap(bitmap, px0 + 1, py0 + 1, shadow, shadow, scale); //drop shadow
		}
		drawBitmap(bitmap, px0, py0, topColor, bottomColor, scale);
	}

	/** draw each '#' of the bitmap as scale x scale pixels, colored from top to bottom as a vertical gradient */
	private void drawBitmap(String[] bitmap, int px0, int py0, int topColor, int bottomColor, int scale) {
		for (int row = 0; row < bitmap.length; row++) {
			for (int col = 0; col < bitmap[row].length(); col++) {
				if (bitmap[row].charAt(col) != '#') {
					continue;
				}
				for (int dy = 0; dy < scale; dy++) {
					for (int dx = 0; dx < scale; dx++) {
						int py = row * scale + dy;
						double ratio = (double) py / (bitmap.length * scale - 1); //ratio of current pixel y to last row
						drawPixel(px0 + col * scale + dx, py0 + py, Palette.mix(topColor, bottomColor, ratio));
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
}
