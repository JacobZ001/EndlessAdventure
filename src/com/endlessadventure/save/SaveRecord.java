package com.endlessadventure.save;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.endlessadventure.GameState;
import com.endlessadventure.WorldMap;
import com.endlessadventure.data.DataValidation;
import com.endlessadventure.data.EnemyCatalog;
import com.endlessadventure.data.EquipmentCatalog;
import com.endlessadventure.data.MissingFieldException;
import com.endlessadventure.entity.Attribute;
import com.endlessadventure.entity.Player;
import com.endlessadventure.entity.Skill;
import com.endlessadventure.item.Equipment;
import com.endlessadventure.item.Item;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.story.PastScene;

/** the layout of a save file. Gson reads and writes these records directly;
 *  turning them back into game objects goes through the game constructors, so loading never skips their checks */
record SaveRecord(int version, PlayerData playerData, CurrentSceneData currentSceneData, List<PastSceneData> historyData,
		WorldData worldData, String currentRoomId, Boolean restProtected) {
	static final int SAVE_CONTRACT_VERSION = 5;

	/** player data holder; int fields set to Integer so GSON returns null instead of 0 for validation purpose */
	record PlayerData(String name, Integer level, Integer exp, Integer hp,
			Map<Attribute, Integer> attributes, Map<Skill, Integer> skills,
			Map<Equipment.Slot, String> equipped, List<String> inventory) {}

	record CurrentSceneData(String description, List<String> options, List<String> enemyIds) {}

	record PastSceneData(String roomId, String description, String action) {}

	/** the world map. WorldMap's own records are not read by Gson directly: Gson wraps their constructor errors in a
	 *  plain RuntimeException and reads a missing int as 0, so these hold the values and the game objects are built
	 *  in toGameState */
	record WorldData(List<LocationData> locations, List<RoomData> rooms, List<ConnectionData> connections) {}

	record LocationData(String id, String name, WorldMap.LocationKind kind, String theme, Integer danger) {}

	record RoomData(String id, String locationId, String name, String backdrop) {}

	record ConnectionData(String a, String b) {}

	/** saving: game objects -> file layout; items are stored by catalog ID
	 *  @throws IllegalArgumentException if the state cannot be saved */
	static SaveRecord from(GameState state) {
		CurrentScene current = state.getCurrentScene();
		List<PastScene> history = state.getHistory();
		if (history.size() != current.getTurn() - 1) {
			throw new IllegalArgumentException("History size " + history.size() + " does not match turn " + current.getTurn());
		}

		Player p = state.getPlayer();
		Map<Equipment.Slot, String> equipped = new EnumMap<>(Equipment.Slot.class);
		p.getEquippedAll().forEach((slot, piece) -> equipped.put(slot, piece.getId()));
		
		PlayerData playerData = new PlayerData(p.getName(), p.getLevel(), p.getExp(), p.getHp(),
				p.getAttributes(), p.getSkills(), equipped,
				p.getInventory().stream().map(Item::getId).toList());

		CurrentSceneData currentSceneData = new CurrentSceneData(current.getDescription(), List.of(current.getOptions()), current.getEnemyIds());

		List<PastSceneData> historyData = new ArrayList<>();
		for (PastScene r : history) {
			historyData.add(new PastSceneData(r.getRoomId(), r.getDescription(), r.getPlayerAction()));
		}
		WorldMap map = state.getWorldMap();
		WorldData worldData = new WorldData(
				map.getLocations().stream().map(l -> new LocationData(l.id(), l.name(), l.kind(), l.theme(), l.danger())).toList(),
				map.getRooms().stream().map(r -> new RoomData(r.id(), r.locationId(), r.name(), r.backdrop())).toList(),
				map.getConnections().stream().map(c -> new ConnectionData(c.a(), c.b())).toList());
		return new SaveRecord(SAVE_CONTRACT_VERSION, playerData, currentSceneData, historyData,
				worldData, state.getCurrentRoom().id(), state.isProtected());
	}

	/** loading: file layout -> game objects
	 *  @throws MissingFieldException if a field the game needs is absent
	 *  @throws IllegalArgumentException if a value is invalid, an item ID is unknown or the equipment conflicts */
	GameState toGameState(EquipmentCatalog equipCat, EnemyCatalog enemyCat) {
		//check null
		DataValidation.checkNotNull(this, ""); 
		
		//load player object
		Player loadedPlayer = new Player(playerData.name(), playerData.level(), playerData.hp(), playerData.exp(),
				playerData.attributes(), playerData.skills());

		//load equipment
		//look each ID up; equip must not have to take anything off, or the file holds a conflict
		for (Map.Entry<Equipment.Slot, String> e : playerData.equipped().entrySet()) {
			Equipment loadedPiece = equipCat.get(e.getValue());
			if (loadedPiece == null) {
				throw new IllegalArgumentException("Unknown equipment: " + e.getValue());
			}
			if (!loadedPlayer.equip(loadedPiece, e.getKey()).isEmpty()) {
				throw new IllegalArgumentException("Conflicting equipment in " + e.getKey().label);
			}
		}

		//load player inventory
		for (String id : playerData.inventory()) {
			Item loadedItem = equipCat.get(id);
			if (loadedItem == null) {
				throw new IllegalArgumentException("Unknown item: " + id);
			}
			loadedPlayer.addItem(loadedItem);
		}
		

		//load history
		//turns are not stored: each record's turn is its position, the current scene comes after the last one
		List<PastScene> loadedHistory = new ArrayList<>();
		for (int i = 0; i < historyData.size(); i++) {
			PastSceneData entry = historyData.get(i);
			loadedHistory.add(new PastScene(entry.roomId(), entry.description(), i + 1,  entry.action()));
		}
		
		//validate enemyIds and load current scene
		for (String id : currentSceneData.enemyIds()) {
			if (enemyCat.get(id) == null) {
				throw new IllegalArgumentException("Unknown enemy: " + id);
			}
		}
		CurrentScene loadedScene = new CurrentScene(
				currentSceneData.description(), 
				loadedHistory.size() + 1,
				currentSceneData.options().toArray(String[]::new), currentSceneData.enemyIds());

		//rebuild the map through the game's constructors and writers, so their checks run
		WorldMap loadedMap = new WorldMap(
				worldData.locations().stream().map(l -> new WorldMap.Location(l.id(), l.name(), l.kind(), l.theme(), l.danger())).toList(),
				worldData.rooms().stream().map(r -> new WorldMap.Room(r.id(), r.locationId(), r.name(), r.backdrop())).toList(),
				worldData.connections().stream().map(c -> new WorldMap.Connection(c.a(), c.b())).toList());

		return new GameState(loadedPlayer, loadedScene, loadedHistory, loadedMap, currentRoomId, restProtected);
	}
}
