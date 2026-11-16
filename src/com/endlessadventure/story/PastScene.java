package com.endlessadventure.story;

public class PastScene extends Scene {
	private final String roomId;	//the room this scene took place in
	private final String playerAction;

	public PastScene(String roomId, String description, int turn, String playerAction) {
		super(description, turn);
		if(roomId == null || roomId.isBlank()) {
			throw new IllegalArgumentException("Invalid past scene: room id cannot be null or blank.");
		}
		this.roomId = roomId;
		this.playerAction = playerAction;
 	}

	public String getRoomId() {
		return roomId;
	}

	public String getPlayerAction() {
		return playerAction;
	}
}
