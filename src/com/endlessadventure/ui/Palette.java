package com.endlessadventure.ui;

import com.endlessadventure.entity.Attribute;

/** shared rgb colors and helpers; a color is an int in 0xRRGGBB form */
public final class Palette {
	static final int BG = 0x0C1017;
	static final int MUTED = 0x5A6274;	//frames, hints and disabled options
	static final int LINE = mix(BG, MUTED, 0.45);
	static final int TEXT = 0xD8DEE9;
	static final int KEY = 0xE8C268;		//GOLD
	static final int TAG = 0x7FB2D9;		//SKY
	static final int SUCCESS = 0x8FC27A;	//SAGE
	static final int WARNING = 0xD96F2B;	//EMBER
	static final int ERROR = 0xD9686B; 	//ROSE
	
	private static final int STR = 0xD9776B;
	private static final int DEX = 0x6CC4B8;
	private static final int CON = 0x8FC27A;
	private static final int INT = 0x7FB2D9;
	private static final int PER = 0xA98BD9;
	private static final int WIL = 0xE0A458;
	private static final int CHA = 0xD98BB8;
	
	private Palette() {
	}

	/** red channel, 0-255 */
	static int red(int color) {
		return (color >> 16) & 0xFF;
	}

	/** green channel, 0-255 */
	static int green(int color) {
		return (color >> 8) & 0xFF;
	}

	/** blue channel, 0-255 */
	static int blue(int color) {
		return color & 0xFF;
	}

	/** pack three 0-255 channels into one color */
	static int rgb(int red, int green, int blue) {
		return (red << 16) | (green << 8) | blue;
	}

	/** blend two colors, ratio = 0 gives a, ratio = 1 gives b */
	static int mix(int a, int b, double ratio) {
		return rgb(
				(int) Math.round(red(a) * (1 - ratio) + red(b) * ratio),
				(int) Math.round(green(a) * (1 - ratio) + green(b) * ratio),
				(int) Math.round(blue(a) * (1 - ratio) + blue(b) * ratio));
	}
	
	/** helper function to parse a rgb value to ANSI code */
	static String rgbToAnsi(int type, int color) {
		return new StringBuilder(Canvas.ESC).append(type).append(";2;")
				.append(red(color)).append(';')
				.append(green(color)).append(';')
				.append(blue(color)).append('m')
				.toString();
	}
	
	static int getAttributeColor(Attribute a) {
		return switch(a) {
			case STR -> STR;
			case DEX -> DEX;
			case CON -> CON;
			case INT -> INT;
			case PER -> PER;
			case WIL -> WIL;
			case CHA -> CHA;
			default -> {
				throw new IllegalArgumentException("Unexpected value: " + a);
			}
		};
	}
}
