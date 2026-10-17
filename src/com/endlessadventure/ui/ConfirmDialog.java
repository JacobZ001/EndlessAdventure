package com.endlessadventure.ui;

import java.util.List;


/** a modal yes/no box drawn over the current frame; returns true if the player confirms */
final class ConfirmDialog {
	private static final int WIDTH = 64,
						 	 X = (Canvas.WIDTH - WIDTH) / 2,
						 	 BUTTON_X =	(Canvas.WIDTH - 2 * Canvas.BUTTON_WIDTH - 2) / 2;

	/** message: the first line in TEXT, the rest in MUTED; lines wider than the box are cut */
	static boolean show(UIHandler ui, String title, List<String> message, String warning, String confirmLabel) {
		
		int height = 2 * message.size() + 10;
		int top = (Canvas.HEIGHT - height) / 2;

		Canvas canvas = ui.getCanvas();
		canvas.dim();

		while(true) {
			ui.clearButtons(); //disable screen's buttons until the dialog is done
			canvas.fill(X, top, WIDTH, height, Palette.BG);
			canvas.drawBox(X, top, WIDTH, height, Palette.KEY);
			canvas.drawTextCentered(top, " " + title + " ", Palette.KEY);

			int y = top + 2;
			for (int i = 0; i < message.size(); i++) {
				canvas.drawTextCentered(y, fit(message.get(i)), i == 0 ? Palette.TEXT : Palette.MUTED);
				y += 2;
			}
			canvas.drawTextCentered(y, warning, Palette.WARNING);

			y += 2;
			ui.drawButton(BUTTON_X, y, confirmLabel, true, new Button(0));
			String confirmHint = Character.toUpperCase(confirmLabel.charAt(0)) + confirmLabel.toLowerCase().substring(1);
			ui.drawButton(BUTTON_X + Canvas.BUTTON_WIDTH + 2, y, "CANCEL", true, new Button(1));
			canvas.drawTextCentered(top + height - 2, " [Y] %s   [N] Cancel ".formatted(confirmHint), Palette.MUTED);
			ui.present();

			UIHandler.Input in = ui.readInput();
			switch(in.inputKey()) {
				case CHAR -> {
					if(in.ch() == 'y' || in.ch() == 'Y') {
						return true;
					}
					if(in.ch() == 'n' || in.ch() == 'N') {
						return false;
					}
				}
				case ESC -> {
					return false;
				}
				case CLICK -> {
					Button b = in.button();
					if (b != null) {
						return b.index() == 0;
					}
				}
				default -> {
					continue;
				}
			}
		}
	}

	/** cuts a line that would run into the border; defense for really long names */
	private static String fit(String line) {
		return line.length() <= WIDTH - 4 ? line : line.substring(0, WIDTH - 7) + "...";
	}
}
