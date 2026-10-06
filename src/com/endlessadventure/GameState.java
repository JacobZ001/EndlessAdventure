package com.endlessadventure;

import java.util.ArrayList;
import java.util.List;

import com.endlessadventure.entity.Player;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.story.SceneRecord;

public class GameState {
	private Player player;
	private CurrentScene currentScene;
	private List<SceneRecord> history;
	
	public GameState(Player player, CurrentScene currentScene, List<SceneRecord> history) {
		setPlayer(player);
		setCurrentScene(currentScene);
		setHistory(history);
	}
	
	public GameState() {
		this(null, null, new ArrayList<SceneRecord>());
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

	public List<SceneRecord> getHistory() {
		return history;
	}

	public void setHistory(List<SceneRecord> history) {
		this.history = history;
	}
	
	public void addRecord(SceneRecord record) {
		history.add(record);
	}
}
