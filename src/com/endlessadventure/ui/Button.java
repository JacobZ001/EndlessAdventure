package com.endlessadventure.ui;

/** a clickable area. index: the list row it belongs to, -1 if none (a plain button);
 *  action: the screen's own name for what it does or which part of the row it is, null if the row itself */
record Button(int index, Enum<?> action) {
	/** a pointing area: hovering or clicking only aims, and the screen decides what a click means,
	 *  so UIHandler plays no sound for it */
	enum Area { MAP_CELL }


	/** a whole list row */
	Button(int index) {
		this(index, null);
	}

	/** a button not tied to a row, e.g. new Button(Action.DELETE) */
	Button(Enum<?> action) {
		this(-1, action);
	}
}


