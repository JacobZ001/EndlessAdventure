package com.endlessadventure.ui;

import com.endlessadventure.Displayable.Tone;
import com.endlessadventure.entity.Attribute;
import com.endlessadventure.item.Equipment;

/** shared rgb colors and helpers; a color is an int in 0xRRGGBB form */
public final class Palette {
	static final int BG = 0x0C1017;
	static final int MUTED = 0x5A6274;	//hints and disabled options
	static final int PANEL = mix(BG, 0xFFFFFF, 0.035);	//panels: a shade lighter than the background
	static final int PANEL_LIGHT = mix(BG, 0xFFFFFF, 0.075); //highlight on panels
	static final int FAINT = mix(MUTED, BG, 0.3); //panel titles
	static final int LINE = mix(BG, MUTED, 0.30); //frames and lines
	static final int TEXT = 0xD8DEE9;
	static final int KEY = 0xE8C268;		//GOLD
	static final int TAG = 0x7FB2D9;		//SKY
	static final int SUCCESS = 0x8FC27A;	//SAGE
	static final int WARNING = 0xD96F2B;	//AMBER
	static final int DANGER = 0xD9686B; 	//ROSE
	
	//attribute colors
	private static final int STR = 0xD9776B;
	private static final int DEX = 0x6CC4B8;
	private static final int CON = 0x8FC27A;
	private static final int INT = 0x7FB2D9;
	private static final int PER = 0xA98BD9;
	private static final int WIL = 0xE0A458;
	private static final int CHA = 0xD98BB8;
	
	//equipment quality colors: the usual RPG rarity order, more saturated than the interface colors so loot stands out
	private static final int T1 = 0xC8CDD5;	//pale steel
	private static final int T2 = 0x7DD97B;	//green
	private static final int T3 = 0x4FA3F7;	//azure
	private static final int T4 = 0xB878F5;	//violet
	private static final int T5 = 0xFF9A3C;	//amber
	
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
	
	/** static method to get the corresponding color of an enum for display */
	static int colorOf(Enum<?> meaning) {
		return switch(meaning) {
			case Tone t -> getToneColor(t);
			case Attribute a -> getAttributeColor(a);
			case Equipment.Quality q -> getQualityColor(q);
			default -> TEXT;
		};
	}

	private static int getToneColor(Tone t) {
		return switch(t) {
			case PLAIN -> TEXT;
			case NOTE -> MUTED;
			case KEYWORD -> TAG;
			case GOOD -> SUCCESS;
			case BAD -> DANGER;
		};
	}

	private static int getQualityColor(Equipment.Quality q) {
		return switch(q) {
			case COMMON -> T1;
			case FINE -> T2;
			case RARE -> T3;
			case MASTER -> T4;
			case EPIC -> T5;
		};
	}
	
	private static int getAttributeColor(Attribute a) {
		return switch(a) {
			case STR -> STR;
			case DEX -> DEX;
			case CON -> CON;
			case INT -> INT;
			case PER -> PER;
			case WIL -> WIL;
			case CHA -> CHA;
			default -> {
				throw new IllegalArgumentException("Illegal attribute to get color: " + a);
			}
		};
	}
}
