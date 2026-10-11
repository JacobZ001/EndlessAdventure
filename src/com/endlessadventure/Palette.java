package com.endlessadventure;

/** shared rgb colors and helpers; a color is an int in 0xRRGGBB form */
public final class Palette {
	public static final int BG = 0x0C1017;
	public static final int MUTED = 0x5A6274; //frames, hints and disabled options
	public static final int TEXT = 0xD8DEE9;
	public static final int GOLD = 0xE8C268; //keys and highlights
	public static final int EMBER = 0xD96F2B;

	private Palette() {
	}

	/** red channel, 0-255 */
	public static int red(int color) {
		return (color >> 16) & 0xFF;
	}

	/** green channel, 0-255 */
	public static int green(int color) {
		return (color >> 8) & 0xFF;
	}

	/** blue channel, 0-255 */
	public static int blue(int color) {
		return color & 0xFF;
	}

	/** pack three 0-255 channels into one color */
	public static int rgb(int red, int green, int blue) {
		return (red << 16) | (green << 8) | blue;
	}

	/** blend two colors, ratio = 0 gives a, ratio = 1 gives b */
	public static int mix(int a, int b, double ratio) {
		return rgb(
				(int) Math.round(red(a) * (1 - ratio) + red(b) * ratio),
				(int) Math.round(green(a) * (1 - ratio) + green(b) * ratio),
				(int) Math.round(blue(a) * (1 - ratio) + blue(b) * ratio));
	}
}
