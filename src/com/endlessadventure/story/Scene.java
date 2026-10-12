package com.endlessadventure.story;

public abstract class Scene{

	private final String location;
	private final String description;
	private int turn;

	public Scene(String location, String description, int turn) {
		this.location = location;
		this.description = description;
		if(turn < 1) {
			throw new IllegalArgumentException("Invalid turn: " + turn);
		}
		this.turn = turn;
	}

	public String getLocation() {
		return location;
	}

	public String getDescription() {
		return description;
	}

	public int getTurn() {
		return turn;
	}
}