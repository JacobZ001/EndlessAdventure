package com.endlessadventure.ui;

/** a full-screen page: owns the frame; overlays like ConfirmDialog do not extend it */
abstract class Screen {

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
		if(title != null) {
			canvas.drawText(2, 0, " " + title +" ", Palette.KEY);
		}
    }
}