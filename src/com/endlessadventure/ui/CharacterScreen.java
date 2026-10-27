package com.endlessadventure.ui;

import java.util.List;
import java.util.Map;

import com.endlessadventure.entity.Player;

/** the whole character: the same character column as the inventory, and every other stat where the inventory has its backpack */
public final class CharacterScreen extends Screen {
	public enum Action { CLOSE, INVENTORY }

	//same frame as the inventory; the details column leaves room for the CLOSE button under it
	private static final int X = 3, Y = 4, H = 44, DETAIL_H = 40, SPACING = 2;
	private static final int DETAIL_X = X + CharacterPanel.WIDTH + SPACING, DETAIL_W = Canvas.WIDTH - 3 - DETAIL_X;

	private final Player player;

	public CharacterScreen(UIHandler ui, Player player) {
		super(ui);
		this.player = player;
	}

	/** shows the character until the player closes it */
	public Action run() {
		while (true) {
			drawFrame();
			UIHandler.Input in = ui.readInput();
			switch (in.inputKey()) {
				case ESC -> {return Action.CLOSE;}
				case CHAR -> {
					switch(Character.toLowerCase(in.ch())) {
						case 'c' -> {return Action.CLOSE;}
						case 'i' -> {return Action.INVENTORY;}
						default -> {}
					}
				}
				case CLICK -> {
					if (in.button() instanceof Button b && b.action() instanceof Action a) { //CLOSE or the INVENTORY tab
						return a;
					}
				}
				default -> {}
			};
		}
	}

	private void drawFrame() {
		beginFrame();
		drawTabs(new String[] {"CHARACTER", "INVENTORY"}, new Enum<?>[] {null, Action.INVENTORY}, 0);
		CharacterPanel.draw(canvas, ui, X, Y, CharacterPanel.WIDTH, H, player, s -> Palette.LINE);
		CharacterDetails.draw(canvas, DETAIL_X, Y, DETAIL_W, DETAIL_H, player);
		ui.drawButton(DETAIL_X + DETAIL_W - Canvas.BUTTON_WIDTH, Y + DETAIL_H + 1, "CLOSE", true, new Button(Action.CLOSE));
		endFrame(List.of(
				Map.entry("C / ESC", "Close"),
				Map.entry("I", "Inventory")));
	}
}
