package com.endlessadventure.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** a full-screen page: owns the frame; overlays like ConfirmDialog do not extend it.
 *  every frame method starts with beginFrame() and ends with endFrame(hints) */
abstract class Screen {
	/** focus style shared by every list: a gold bar at the row's start, then the marker and the text at these offsets */
	protected static final String FOCUS_INDICATOR = "▌";
	protected static final int ROW_MARKER_OFFSET = 2, ROW_TEXT_OFFSET = 6, TAB_SPACING = 5;

	protected final UIHandler ui;
	protected final Canvas canvas;

	protected Screen(UIHandler ui) {
		this.ui = ui;
		canvas = ui.getCanvas();
	}

	/** clears the canvas and the click areas */
	protected void beginFrame() {
		ui.clearButtons();
		canvas.clear(Palette.BG);
	}

	/** draws the key hints at the bottom, then shows the frame */
	protected void endFrame(List<Map.Entry<String, String>> hints) {
		canvas.drawHints(hints);
		ui.present();
	}

	/** tabs at the top left where character creation has its step bar, for screens that act as tabs of one window
	 *  (inventory and character): the current one gold and underlined, the others text buttons carrying their action */
	protected void drawTabs(String[] labels, Enum<?>[] actions, int current) {
		int tlength = Arrays.stream(labels).mapToInt(String::length).sum();
		int x = (Canvas.WIDTH - tlength - (labels.length - 1) * TAB_SPACING) / 2;
		for (int i = 0; i < labels.length; i++) {
			if (i == current) {
				canvas.drawText(x, 2, labels[i], Palette.KEY);
				canvas.drawText(x, 3, "━".repeat(labels[i].length()), Palette.KEY);
			}
			else {
				ui.drawTextButton(x, 2, labels[i], new Button(actions[i]));
			}
			x += labels[i].length() + TAB_SPACING;
		}
	}
	
	/** focus in a vertical list of count rows: UP and DOWN wrap around, HOVER over one of the rows moves the focus there;
	 *  any other input keeps it. for lists of object rows (save cards) pass only UP and DOWN, so hovering does not change the selection.
	 *  one-dimensional lists only; a grid needs its own version */
	protected static int moveFocus(UIHandler.Input in, int current, int bound, boolean persist) {
		if(bound <= 0) {
			return current;
		}
		return switch (in.inputKey()) {
			case UP -> Math.floorMod(current - 1, bound);
			case DOWN -> Math.floorMod(current + 1, bound);
			case HOVER -> in.button() instanceof Button b && b.index() >= 0 && b.index() < bound ? b.index() : persist? current : -1;
			default -> current;
		};
	}
	
	protected static int moveFocus(UIHandler.Input in, int current, int bound) {
		return moveFocus(in, current, bound, true);
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

	protected void drawToolBar(int rightX, int y, String[] labels, Enum<?>[] actions, String divider) {
		for (int i = 0; i < labels.length; i++) {
			rightX -= labels[i].length();
			ui.drawTextButton(rightX, y, labels[i], new Button(actions[i]));
			if (i < labels.length - 1) {
				rightX -= divider.length();
				canvas.drawText(rightX, y, divider, Palette.MUTED);
			}
		}
	}
}
