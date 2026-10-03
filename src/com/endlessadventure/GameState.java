package com.endlessadventure;

import com.endlessadventure.entity.Player;

public class GameState {
	private Player player;
	private Scene scene;
	private int turn;
	
	public GameState(Player player, Scene scene, int turn) {
		this.player = player;
		this.scene = scene;
		setTurn(turn);
	}
	
	public GameState(Player player) {
		this(player,new Scene(), 1);
	}
	
	public GameState() {
		this(null);
	}

	public Player getPlayer() {
		return player;
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}

	public Scene getScene() {
		return scene;
	}
	
	public void setScene(Scene scene) {
		this.scene = scene;
	}

	public int getTurn() {
		return turn;
	}
	
	/** @throws IllegalArgumentException */
	public void setTurn(int turn) {
		if(turn < 1) {
			throw new IllegalArgumentException("Invalid turn: " + turn);
		}
		this.turn = turn;
	}
}
