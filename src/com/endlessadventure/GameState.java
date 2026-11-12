package com.endlessadventure;

import java.util.ArrayList;
import java.util.List;

import com.endlessadventure.entity.Player;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.story.PastScene;

public class GameState {
	private Player player;
	private CurrentScene currentScene;
	private List<PastScene> history;
	
	public GameState(Player player, CurrentScene currentScene, List<PastScene> history) {
		setPlayer(player);
		setCurrentScene(currentScene);
		setHistory(history);
	}
	
	public GameState() {
		this(null, null, new ArrayList<PastScene>());
	}

	public Player getPlayer() {
		return player;
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}

	public CurrentScene getCurrentScene() {
		return currentScene;
	}
	
	public void setCurrentScene(CurrentScene currentScene) {
		this.currentScene = currentScene;
	}

	public List<PastScene> getHistory() {
		return history;
	}

	public void setHistory(List<PastScene> history) {
		this.history = history;
	}
	
	public void addToHistory(PastScene scene) {
		history.add(scene);
	}
}
