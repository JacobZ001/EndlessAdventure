package com.endlessadventure;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Properties;

import com.endlessadventure.entity.Player;

public class SaveManager {
	public enum SlotStatus{
		EMPTY, READABLE
	}
	public static final int SLOT_COUNT = 3;
	private static final String COMMENT = "EndlessAdventure save v1";
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

	/** Lightweight read for UI to display the summary of a save slot. */
	public SlotOverview readSummary(int slot) {
		Path path = getSlotPath(slot);
		
		try {
			Properties props = loadProps(path);
			String name = getStringProperty(props,"name");
			int level = getIntProperty(props, "level");
			int turn = getIntProperty(props, "turn");
			
			if (level < 1 || level > Player.MAX_LEVEL || turn < 1) {
			    throw new IllegalArgumentException("Invalid level or turn");
			}
			
			return new SlotOverview(slot, SlotStatus.READABLE, name, level, turn);
		} catch (NoSuchFileException e) {
			return new SlotOverview(slot, SlotStatus.EMPTY, null, 0, 0);
		} catch(IOException | IllegalArgumentException | PropertyNotFoundException e) {
			throw new BadSaveException(slot, e);
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
	public void writeSave(int slot, GameState state) throws IOException{
		if (state == null) {
			throw new IllegalArgumentException("Cannot write to save: game state is null");
		}
		else if (state.getPlayer() == null) {
			throw new IllegalArgumentException("Cannot write to save: player is null");
		}
		
		Properties props = new Properties();
		Player player = state.getPlayer();
		props.setProperty("name", player.getName());
		props.setProperty("level", String.valueOf(player.getLevel()));
		props.setProperty("EXP", String.valueOf((int) player.getExp()));
		props.setProperty("hp", String.valueOf(player.getHp()));
		props.setProperty("maxHp", String.valueOf(player.getMaxHp()));
		props.setProperty("attack", String.valueOf(player.getAttack()));
		props.setProperty("armor", String.valueOf(player.getArmor()));
		props.setProperty("energy", String.valueOf(player.getEnergy()));
		props.setProperty("maxEnergy", String.valueOf(player.getMaxEnergy()));
		props.setProperty("turn", String.valueOf(state.getTurn()));
		//TODO Reserved for later: scene id, items, journal (item.0.template=..., etc.)
		
		Files.createDirectories(saveDir);
		Path path = getSlotPath(slot);
		
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			props.store(writer, COMMENT);
		}
	}

	/** Loads a slot into a new GameState. 
	 * @throws NoSuchFileException
	 * @throws PropertyNotFoundException */
	public GameState loadSave(int slot) throws IOException{
		Properties props = loadProps(getSlotPath(slot));

		String name = getStringProperty(props, "name");
		double maxHp = getDoubleProperty(props, "maxHp");
		double hp = getDoubleProperty(props, "hp");
		double attack = getDoubleProperty(props, "attack");
		double armor = getDoubleProperty(props, "armor");
		int maxEnergy = getIntProperty(props, "maxEnergy");
		int energy = getIntProperty(props, "energy");
		int level = getIntProperty(props, "level");
		int exp = getIntProperty(props, "EXP");
		int turn = getIntProperty(props, "turn");
		
		Player player = new Player(name, maxHp, hp, level, attack, armor, maxEnergy, energy, exp);
		//TODO Scene / inventory / journal: load when those systems exist.

		return new GameState(player, new Scene(), turn);
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
