package com.endlessadventure.ui;

import java.util.List;

/** an editable text box: printable characters and Backspace; it holds as much text as fits in its wrapped height */
final class TextField {
	private final UIHandler ui;
	private final int width, height;
	private final StringBuilder text = new StringBuilder();

	TextField(UIHandler ui, int width, int height) {
		this.ui = ui;
		this.width = width;
		this.height = height;
	}

	/** edits the text on CHAR and BACKSPACE and returns true; returns false for any other input */
	boolean handleInput(UIHandler.Input in) {
		switch (in.inputKey()) {
			case CHAR -> {
				if (Canvas.wrapText(text.toString() + in.ch(), width).size() <= height) {
					text.append(in.ch());
				} else {
					ui.showWarning("The input box is full.");
				}
				return true;
			}
			case BACKSPACE -> {
				if (!text.isEmpty()) {
					text.setLength(text.length() - 1);
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	String text() {
		return text.toString();
	}

	boolean isEmpty() {
		return text.isEmpty();
	}

	/** draws the wrapped text from x,y, with a gold caret after the last character if caret is true */
	void draw(Canvas canvas, int x, int y, int color, boolean caret) {
		List<String> rows = Canvas.wrapText(text.toString(), width);
		for (int i = 0; i < rows.size(); i++) {
			canvas.drawText(x, y + i, rows.get(i), color);
		}
		if (caret) {
			//wrapText drops trailing spaces; move the caret one cell so a typed space shows
			String last = rows.getLast();
			boolean space = !last.isEmpty() && text.charAt(text.length() - 1) == ' ' && last.length() < width;
			canvas.drawText(x + last.length() + (space ? 1 : 0), y + rows.size() - 1, "_", Palette.KEY);
		}
	}
}
