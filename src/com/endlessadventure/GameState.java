package com.endlessadventure;

import java.util.ArrayList;
import java.util.List;

import com.endlessadventure.WorldMap.Location;
import com.endlessadventure.WorldMap.Room;
import com.endlessadventure.entity.Player;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.story.PastScene;

public class GameState {
	private Player player;
	private CurrentScene currentScene;
	private List<PastScene> history;
	private WorldMap worldMap;
	private String currentRoomId;	//where the player is; null before the opening
	private boolean interruptProtected;
	
	public GameState(Player player, CurrentScene currentScene, List<PastScene> history) {
		this(player, currentScene, history, new WorldMap(), null, false);
	}

	/** the whole state, as a save loads it; the current room and every past scene's room must be on the map */
	public GameState(Player player, CurrentScene currentScene, List<PastScene> history,
			WorldMap worldMap, String currentRoomId, boolean interruptProtected) {
		setPlayer(player);
		setCurrentScene(currentScene);
		setHistory(history);
		this.worldMap = worldMap;
		if (currentRoomId != null) {
			moveTo(currentRoomId);
		}
		for (PastScene past : history) {
			if (worldMap.getRoom(past.getRoomId()) == null) {
				throw new IllegalArgumentException("A past scene names a room that is not on the map: " + past.getRoomId());
			}
		}
		this.interruptProtected = interruptProtected;
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
	
	public WorldMap getWorldMap() {
		return worldMap;
	}
	
	/** the room the player is in, or null before the opening */
	public Room getCurrentRoom() {
		return worldMap.getRoom(currentRoomId);
	}
	
	/** the location of the current room, or null before the opening */
	public Location getCurrentLocation() {
		Room room = getCurrentRoom();
		return room == null ? null : worldMap.getLocation(room.locationId());
	}
	
	void moveTo(String roomId) {
		if(worldMap.getRoom(roomId) == null) {
			throw new IllegalArgumentException("Invalid move: room " + roomId + " does not exist.");
		}
		currentRoomId = roomId;
	}
	
	public boolean isProtected() {
		return interruptProtected;
	}
	
	public void setProtected(boolean x) {
		interruptProtected = x;
	}
}
