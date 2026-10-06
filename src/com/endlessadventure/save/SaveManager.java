package com.endlessadventure.save;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.endlessadventure.entity.Player;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.story.SceneRecord;
import com.endlessadventure.GameState;

public class SaveManager {
	public enum SlotStatus{
		EMPTY, READABLE, CORRUPT
	}
	public static final int SLOT_COUNT = 3;
	private static final String COMMENT = "EndlessAdventure save v2";
	private final Path saveDir = Path.of("saves");
	
	/** get the file path corresponding to the slot number */
	private Path getSlotPath(int slot) {
		if (slot < 1 || slot > SLOT_COUNT) {
			throw new IllegalArgumentException("Slot must be 1–" + SLOT_COUNT + ", got: " + slot);
		}
		return saveDir.resolve("slot" + slot + ".txt");
	}

	/** Summary record for a save slot containing the slot number, status, player name, level, and turn count. */
	public record SlotOverview(int slot, SlotStatus status, String name, int level, int turn) {}

	/** Validate the save before presenting it as readable in the UI. */
	public SlotOverview readSummary(int slot) {
		try {
			GameState state = loadSave(slot);
			Player player = state.getPlayer();
			return new SlotOverview(slot, SlotStatus.READABLE, player.getName(), player.getLevel(), state.getCurrentScene().getTurn());
		} catch (NoSuchFileException e) {
			return new SlotOverview(slot, SlotStatus.EMPTY, null, 0, 0);
		} catch(IOException | BadSaveException e) {
			return new SlotOverview(slot, SlotStatus.CORRUPT, null, 0, 0);
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

	/** Write the current game into a slot (overwrites if present). */
	public void writeSave(int slot, GameState state) throws IOException, BadSaveException{
		Path path = getSlotPath(slot);
		if (state == null) {
			throw new IllegalArgumentException("Cannot write to save: game state is null");
		}
		else if (state.getPlayer() == null) {
			throw new IllegalArgumentException("Cannot write to save: player is null");
		}
		
		//write player stats
		Properties props = new Properties();
		Player player = state.getPlayer();
		props.setProperty("name", player.getName());
		props.setProperty("level", String.valueOf(player.getLevel()));
		props.setProperty("EXP", String.valueOf(player.getExp()));
		props.setProperty("hp", String.valueOf(player.getHp()));
		props.setProperty("maxHp", String.valueOf(player.getMaxHp()));
		props.setProperty("attack", String.valueOf(player.getAttack()));
		props.setProperty("armor", String.valueOf(player.getArmor()));
		props.setProperty("energy", String.valueOf(player.getEnergy()));
		props.setProperty("maxEnergy", String.valueOf(player.getMaxEnergy()));
		
		//write current scene
		CurrentScene currentScene = state.getCurrentScene();
		if(currentScene == null) {
			throw new BadSaveException(slot, "Generate an opening scene before saving");
		}
		props.setProperty("location", currentScene.getLocation());
		props.setProperty("description", currentScene.getDescription());
		
		String[] options = currentScene.getOptions();
		props.setProperty("option_count", String.valueOf(options.length));
		for(int i=0;i<options.length;i++) {
			props.setProperty("options."+i, String.valueOf(options[i]));
		}
		
		//write history
		List<SceneRecord> history = state.getHistory();
		int historyCount = history.size();
		int expectedHC = currentScene.getTurn()-1;
		if(historyCount != expectedHC) {
			throw new BadSaveException(slot, "The history size does not match the current turn." + "Got " + historyCount + ", expected: " + expectedHC);
		}
		props.setProperty("history.count", String.valueOf(historyCount));
		for(int i=0;i<history.size();i++) {
			SceneRecord record = history.get(i);
			if(record.getTurn() != i + 1) {
				throw new BadSaveException(slot, "Invalid Record order at history[%d]. Got %d, expected %d".formatted(i, record.getTurn(), i+1));
			}
			
			String prefix = "history." + i + ".";
			props.setProperty(prefix + "location", record.getLocation());
			props.setProperty(prefix + "description", record.getDescription());
			props.setProperty(prefix + "action", record.getPlayerAction());
		}
		
		//TODO implement write save for inventory
		
		Files.createDirectories(saveDir);
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			props.store(writer, COMMENT);
		}
	}

	/** Load a new state without modifying the running game.
	 * @throws IOException if the file cannot be read
	 * @throws BadSaveException if its contents are invalid */
	public GameState loadSave(int slot) throws IOException, BadSaveException{
		Path path = getSlotPath(slot);
		try {
			Properties props = loadProps(path);

			//load player stats
			String name = getStringProperty(props, "name");
			double maxHp = getDoubleProperty(props, "maxHp");
			double hp = getDoubleProperty(props, "hp");
			double attack = getDoubleProperty(props, "attack");
			double armor = getDoubleProperty(props, "armor");
			int maxEnergy = getIntProperty(props, "maxEnergy");
			int energy = getIntProperty(props, "energy");
			int level = getIntProperty(props, "level");
			int exp = getIntProperty(props, "EXP");

			Player player = new Player(name, maxHp, hp, level, attack, armor, maxEnergy, energy, exp);
			
			//load history
			int historyCount = getIntProperty(props,"history.count");
			
			if (historyCount < 0) {
			    throw new IllegalArgumentException(
			            "Illegal history count: " + historyCount);
			}
			
			List<SceneRecord> history = new ArrayList<>();
			
			for(int i=0; i< historyCount; i++) {
				String prefix = "history." + i + ".";
				
				SceneRecord record = new SceneRecord(
						getStringProperty(props, prefix + "location"),
						getStringProperty(props, prefix + "description"),
						i+1,
						getStringProperty(props, prefix + "action"));
				history.add(record);
			}
			
			//load current scene
			String location = getStringProperty(props, "location");
			String description = getStringProperty(props, "description");
			
			int optionCount = getIntProperty(props, "option_count");
			if(optionCount < CurrentScene.MIN_OPTION_COUNT || optionCount > CurrentScene.MAX_OPTION_COUNT) {
				throw new IllegalArgumentException("Illegal option count: " + optionCount);
			}
			String[] options = new String[optionCount];
			for(int i=0; i<optionCount;i++) {
				options[i] = getStringProperty(props, "options." + i);
			}

			CurrentScene currentScene = new CurrentScene(location, description, historyCount + 1, options);
			
			//TODO implement load save for inventory

			return new GameState(player, currentScene, history);
		} catch (IllegalArgumentException | PropertyNotFoundException e) {
			throw new BadSaveException(slot, e);
		}
	}
	
	/** Deletes the slot file; returns false if it does not exist. */
	public boolean deleteSave(int slot) throws IOException{
		return Files.deleteIfExists(getSlotPath(slot));
	}

	/** load properties from selected slot file */
	private Properties loadProps(Path path) throws IOException {
		Properties props = new Properties();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			props.load(reader);
		}
		return props;
	}
	
	/** helper method for parsing string
	 * @throws PropertyNotFoundException */
	private static String getStringProperty(Properties props, String key){
		String raw = props.getProperty(key);
		if (raw == null || raw.isBlank()) {
			throw new PropertyNotFoundException(key);
		}
		return raw.strip();
	}

	/** helper method for parsing int 
	 * @throws NumberFormatException 
	 * @throws PropertyNotFoundException */
	private static int getIntProperty(Properties props, String key){
		return Integer.parseInt(getStringProperty(props,key));
	}

	/** helper method for parsing double
	 * @throws NumberFormatException 
	 * @throws PropertyNotFoundException */
	private static double getDoubleProperty(Properties props, String key){
		double value = Double.parseDouble(getStringProperty(props,key));
		if (!Double.isFinite(value)) {
			throw new IllegalArgumentException("Invalid number: " + key);
		}
		return value;
	}
}
