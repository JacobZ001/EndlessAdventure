package com.endlessadventure.save;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.endlessadventure.GameState;
import com.endlessadventure.data.EquipmentCatalog;
import com.endlessadventure.entity.Player;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/** reads, writes and deletes the save slots; each slot is one JSON file laid out as a SaveRecord */
public class SaveManager {
	public static final int SLOT_COUNT = 3;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	private final Path saveDir = Path.of("saves");
	private final EquipmentCatalog catalog;

	public enum SlotStatus { EMPTY, READABLE, CORRUPT }
	
	/** Summary record for a save slot containing the slot number, status, player name, level, and turn count. */
	public record SlotOverview(int slot, SlotStatus status, String name, int level, int turn, String location) {}

	public SaveManager(EquipmentCatalog catalog) {
		this.catalog = catalog;
	}

	/** get the file path corresponding to the slot number */
	private Path getSlotPath(int slot) {
		if (slot < 1 || slot > SLOT_COUNT) {
			throw new IllegalArgumentException("Slot must be 1–" + SLOT_COUNT + ", got: " + slot);
		}
		return saveDir.resolve("slot" + slot + ".json");
	}

	/** Validate the save before presenting it as readable in the UI. */
	public SlotOverview readSummary(int slot) {
		try {
			GameState state = loadSave(slot);
			Player player = state.getPlayer();
			return new SlotOverview(slot, SlotStatus.READABLE, player.getName(), player.getLevel(), state.getCurrentScene().getTurn(), state.getCurrentScene().getLocation());
		} catch (NoSuchFileException e) {
			return new SlotOverview(slot, SlotStatus.EMPTY, null, 0, 0, "");
		} catch(IOException | BadSaveException e) {
			return new SlotOverview(slot, SlotStatus.CORRUPT, null, 0, 0, "");
		}
	}
	
	/** Read the summary of all three slots, for drawing the load/save screen. */
	public SlotOverview[] readAllSummaries() {
		SlotOverview[] summaries = new SlotOverview[SLOT_COUNT];
		for (int slot = 1; slot <= SLOT_COUNT; slot++) {
			summaries[slot - 1] = readSummary(slot);
		}
		return summaries;
	}
	
	/** Write the current game into a slot, replacing what is there.
	 *  The file is written to a temporary file first and then moved over the old one,
	 *  so a crash while writing never leaves a half-written save behind */
	public void writeSave(int slot, GameState state) throws IOException, BadSaveException {
		Path path = getSlotPath(slot);
		if (state == null || state.getPlayer() == null) {
			throw new IllegalArgumentException("Cannot write to save: no game or player");
		}
		if (state.getCurrentScene() == null) {
			throw new BadSaveException(slot, "Generate an opening scene before saving");
		}
		String json;
		try {
			json = GSON.toJson(SaveRecord.from(state));
		} catch (IllegalArgumentException e) {
			throw new BadSaveException(slot, e);
		}
		Files.createDirectories(saveDir);
		Path temp = path.resolveSibling(path.getFileName() + ".tmp");
		Files.writeString(temp, json, StandardCharsets.UTF_8);
		Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	/** Load a new state without modifying the running game.
	 *  @throws NoSuchFileException if the slot is empty
	 *  @throws IOException if the file cannot be read
	 *  @throws BadSaveException if its contents are invalid or from another save version */
	public GameState loadSave(int slot) throws IOException, BadSaveException {
		Path path = getSlotPath(slot);
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			SaveRecord saveRecord = GSON.fromJson(reader, SaveRecord.class);
			if (saveRecord == null || saveRecord.version() != SaveRecord.SAVE_CONTRACT_VERSION) {
				throw new BadSaveException(slot, "Unsupported save version.");
			}
			return saveRecord.toGameState(catalog);
		} catch (JsonParseException | IllegalArgumentException | MissingFieldException e) {
			throw new BadSaveException(slot, e);
		}
	}

	/** Deletes the slot file; returns false if it does not exist. */
	public boolean deleteSave(int slot) throws IOException{
		return Files.deleteIfExists(getSlotPath(slot));
	}
}
