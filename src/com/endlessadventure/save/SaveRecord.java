package com.endlessadventure.save;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.endlessadventure.GameState;
import com.endlessadventure.data.DataValidation;
import com.endlessadventure.data.EquipmentCatalog;
import com.endlessadventure.data.MissingFieldException;
import com.endlessadventure.entity.Attribute;
import com.endlessadventure.entity.Player;
import com.endlessadventure.entity.Skill;
import com.endlessadventure.item.Equipment;
import com.endlessadventure.item.Item;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.story.SceneRecord;

/** the layout of a save file. Gson reads and writes these records directly;
 *  turning them back into game objects goes through the game constructors, so loading never skips their checks */
record SaveRecord(int version, PlayerData player, SceneData scene, List<HistoryData> history) {
	static final int SAVE_CONTRACT_VERSION = 3;

	/** player data holder; int fields set to Integer so GSON returns null instead of 0 for validation purpose */
	record PlayerData(String name, Integer level, Integer exp, Integer hp,
			Map<Attribute, Integer> attributes, Map<Skill, Integer> skills,
			Map<Equipment.Slot, String> equipped, List<String> inventory) {}

	record SceneData(String location, String description, List<String> options) {}

	record HistoryData(String location, String description, String action) {}

	/** saving: game objects -> file layout; items are stored by catalog ID
	 *  @throws IllegalArgumentException if the state cannot be saved */
	static SaveRecord from(GameState state) {
		CurrentScene current = state.getCurrentScene();
		List<SceneRecord> records = state.getHistory();
		if (records.size() != current.getTurn() - 1) {
			throw new IllegalArgumentException("History size " + records.size() + " does not match turn " + current.getTurn());
		}

		Player p = state.getPlayer();
		Map<Equipment.Slot, String> equipped = new EnumMap<>(Equipment.Slot.class);
		p.getEquippedAll().forEach((slot, piece) -> equipped.put(slot, piece.getId()));
		
		PlayerData player = new PlayerData(p.getName(), p.getLevel(), p.getExp(), p.getHp(),
				p.getAttributes(), p.getSkills(), equipped,
				p.getInventory().stream().map(Item::getId).toList());

		SceneData scene = new SceneData(current.getLocation(), current.getDescription(), List.of(current.getOptions()));

		List<HistoryData> history = new ArrayList<>();
		for (SceneRecord r : records) {
			history.add(new HistoryData(r.getLocation(), r.getDescription(), r.getPlayerAction()));
		}
		return new SaveRecord(SAVE_CONTRACT_VERSION, player, scene, history);
	}

	/** loading: file layout -> game objects
	 *  @throws MissingFieldException if a field the game needs is absent
	 *  @throws IllegalArgumentException if a value is invalid, an item ID is unknown or the equipment conflicts */
	GameState toGameState(EquipmentCatalog catalog) {
		//check null and create player object
		PlayerData p = DataValidation.require(player, "player");
		Player loaded = new Player(
				DataValidation.require(p.name(), "player.name"),
				DataValidation.require(p.level(), "player.level"),
				DataValidation.require(p.hp(),"player.hp"),
				DataValidation.require(p.exp(),"player.exp"),
				DataValidation.requireEntries(p.attributes(), "player.attributes"),
				DataValidation.requireEntries(p.skills(), "player.skills"));

		//equip equipment
		//look each ID up; equip must not have to take anything off, or the file holds a conflict
		for (Map.Entry<Equipment.Slot, String> e : DataValidation.requireEntries(p.equipped(), "player.equipped").entrySet()) {
			Equipment piece = catalog.get(e.getValue());
			if (piece == null) {
				throw new IllegalArgumentException("Unknown equipment: " + e.getValue());
			}
			if (!loaded.equip(piece, e.getKey()).isEmpty()) {
				throw new IllegalArgumentException("Conflicting equipment in " + e.getKey().label);
			}
		}

		//populate player inventory
		for (String id : DataValidation.require(p.inventory(), "player.inventory")) {
			Item item = catalog.get(DataValidation.require(id, "player.inventory entry"));
			if (item == null) {
				throw new IllegalArgumentException("Unknown item: " + id);
			}
			loaded.addItem(item);
		}

		//restore history
		//turns are not stored: each record's turn is its position, the current scene comes after the last one
		List<SceneRecord> records = new ArrayList<>();
		List<HistoryData> saved = DataValidation.require(history, "history");
		for (int i = 0; i < saved.size(); i++) {
			HistoryData h = DataValidation.require(saved.get(i), "history entry");
			records.add(new SceneRecord(DataValidation.require(h.location(), "history.location"), DataValidation.require(h.description(), "history.description"),
					i + 1, DataValidation.require(h.action(), "history.action")));
		}

		SceneData s = DataValidation.require(scene, "scene");
		CurrentScene current = new CurrentScene(DataValidation.require(s.location(), "scene.location"), DataValidation.require(s.description(), "scene.description"),
				records.size() + 1, DataValidation.require(s.options(), "scene.options").toArray(String[]::new));

		return new GameState(loaded, current, records);
	}
}
