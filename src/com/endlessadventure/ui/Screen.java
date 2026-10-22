package com.endlessadventure.ui;

import java.util.List;
import java.util.Map;

/** a full-screen page: owns the frame; overlays like ConfirmDialog do not extend it.
 *  every frame method starts with beginFrame() and ends with endFrame(hints) */
abstract class Screen {
	/** focus style shared by every list: a gold bar at the row's start, then the marker and the text at these offsets */
	protected static final String FOCUS_INDICATOR = "▌";
	protected static final int ROW_MARKER_OFFSET = 2, ROW_TEXT_OFFSET = 6;

	protected final UIHandler ui;
	protected final Canvas canvas;
	protected final String title;

	protected Screen(UIHandler ui, String title) {
		this.ui = ui;
		canvas = ui.getCanvas();
		this.title = title;
	}

	/** clears the canvas and the click areas, draws the border and the title (none if null) */
	protected void beginFrame() {
		ui.clearButtons();
		canvas.clear(Palette.BG);
		canvas.drawScreenBorder();
		if (title != null) {
			canvas.drawText(2, 0, " " + title + " ", Palette.KEY);
		}
	}

	/** draws the key hints on the bottom border, then shows the frame */
	protected void endFrame(List<Map.Entry<String, String>> hints) {
		canvas.drawHints(hints);
		ui.present();
	}

	/** focus in a vertical list of count rows: UP and DOWN wrap around, HOVER over one of the rows moves the focus there;
	 *  any other input keeps it. for lists of object rows (save cards) pass only UP and DOWN, so hovering does not change the selection.
	 *  one-dimensional lists only; a grid needs its own version */
	protected static int moveFocus(UIHandler.Input in, int current, int bound) {
		if(bound <= 0) {
			return current;
		}
		return switch (in.inputKey()) {
			case UP -> Math.floorMod(current - 1, bound);
			case DOWN -> Math.floorMod(current + 1, bound);
			case HOVER -> in.button() instanceof Button b && b.index() >= 0 && b.index() < bound ? b.index() : current;
			default -> current;
		};
	}

	/** a one-row list entry in the shared focus style, clickable as index: marker (e.g. its number) and text;
	 *  gold bar and gold text when focused, muted when disabled. callers may draw extra text on the row afterwards */
	protected void drawListRow(int index, int x, int y, int w, String marker, String text, boolean focused, boolean enabled) {
		if (focused) {
			canvas.drawText(x, y, FOCUS_INDICATOR, enabled ? Palette.KEY : Palette.MUTED);
		}
		canvas.drawText(x + ROW_MARKER_OFFSET, y, marker, focused && enabled ? Palette.KEY : Palette.MUTED);
		canvas.drawText(x + ROW_TEXT_OFFSET, y, text, !enabled ? Palette.MUTED : focused ? Palette.KEY : Palette.TEXT);
		ui.addButtonBox(x, y, w, 1, new Button(index));
	}
}
