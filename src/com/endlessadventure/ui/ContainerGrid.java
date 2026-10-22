package com.endlessadventure.ui;

import java.util.List;
import java.util.function.Predicate;

import com.endlessadventure.item.Item;

/** a fixed grid of separate item boxes, each holding one icon; empty boxes show the container's capacity */
record ContainerGrid(int cols, int rows) {
	static final int ICON_W = 12, ICON_H = 6; //a 12 x 12 pixel icon, two pixels per character row
	static final int BOX_W = ICON_W + 2, BOX_H = ICON_H + 2; //the icon and its frame
	static final int GAP_X = 3, GAP_Y = 1; //a character is about twice as tall as wide, so the gaps look even
	private static final int EMPTY = Palette.mix(Palette.BG, Palette.LINE, 0.6); //frame of an empty box

	int width() { return cols * BOX_W + (cols - 1) * GAP_X; }
	int height() { return rows * BOX_H + (rows - 1) * GAP_Y; }

	/** draws every box and the items' icons in order, each item clickable under its index;
	 *  the focused box gets a gold frame, a marked item an EQUIPPED tag on its top border */
	void draw(Canvas canvas, UIHandler ui, int x, int y, List<? extends Item> items, int focus, Predicate<Item> marked) {
		for (int i = 0; i < cols * rows; i++) {
			int bx = x + (i % cols) * (BOX_W + GAP_X), by = y + (i / cols) * (BOX_H + GAP_Y);
			if (i >= items.size()) {
				canvas.drawBox(bx, by, BOX_W, BOX_H, EMPTY);
				continue;
			}
			Item item = items.get(i);
			canvas.drawBox(bx, by, BOX_W, BOX_H, i == focus ? Palette.KEY : Palette.LINE);
			if (marked.test(item)) {
				canvas.drawText(bx + 2, by, " EQUIPPED ", Palette.SUCCESS);
			}
			drawIcon(canvas, bx + 1, by + 1, item, 1);
			ui.addButtonBox(bx, by, BOX_W, BOX_H, new Button(i));
		}
	}

	/** focus after an arrow key in a grid holding count items: one box over, staying on filled boxes;
	 *  no focus (-1) starts at the first item */
	int moveFocus(UIHandler.InputKey key, int focus, int count) {
		if (count == 0) {
			return -1;
		}
		if (focus < 0) {
			return 0;
		}
		int next = switch (key) {
			case LEFT -> focus % cols == 0 ? focus : focus - 1;
			case RIGHT -> focus % cols == cols - 1 ? focus : focus + 1;
			case UP -> focus - cols;
			case DOWN -> focus + cols;
			default -> focus;
		};
		return next >= 0 && next < count ? next : focus;
	}

	/** the item's icon, ICON_W x ICON_H times scale; until icons exist the item's name stands in for it,
	 *  wrapped, centered and in its quality color. the caller draws any frame */
	static void drawIcon(Canvas canvas, int x, int y, Item item, int scale) {
		int w = ICON_W * scale, h = ICON_H * scale;
		List<String> lines = Canvas.wrapText(item.getName(), w - 2);
		int top = y + (h - lines.size()) / 2;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			canvas.drawText(x + (w - line.length()) / 2, top + i, line, Palette.colorOf(item.getNameMeaning()));
		}
	}
}
