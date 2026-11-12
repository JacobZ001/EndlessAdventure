package com.endlessadventure.story;

public class PastScene extends Scene {
	private final String playerAction;

	public PastScene(String location, String description, int turn, String playerAction) {
		super(location, description, turn);
		this.playerAction = playerAction;
 	}

	public String getPlayerAction() {
		return playerAction;
	}
}
