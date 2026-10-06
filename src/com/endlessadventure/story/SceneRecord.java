package com.endlessadventure.story;

public class SceneRecord extends Scene {
	private final String playerAction;

	public SceneRecord(String location, String description, int turn, String playerAction) {
		super(location, description, turn);
		this.playerAction = playerAction;
 	}

	public String getPlayerAction() {
		return playerAction;
	}
}
