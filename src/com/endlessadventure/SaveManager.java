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
		EMPTY, READABLE, UNREADABLE
	}
	public static final int SLOT_COUNT = 3;
	private static final String COMMENT = "EndlessAdventure save v1";
	private final Path saveDir = Path.of("saves");
	
	/** get the file corresponding to the slot number */
	private Path getSlotFile(int slot) {
		if (slot < 1 || slot > SLOT_COUNT) {
			throw new IllegalArgumentException("Slot must be 1–" + SLOT_COUNT + ", got: " + slot);
		}
		return saveDir.resolve("slot" + slot + ".txt");
	}

	/** Summary record for load/save UI. */
	public record SlotSummary(int slot, SlotStatus status, String name, int level, int turn) {}

	/** Lightweight read for UI */
	public SlotSummary readSummary(int slot) {
		Path path = getSlotFile(slot);
		
		try {
			Properties props = loadProps(path);
			String name = getStringProperty(props,"name");
			int level = getIntProperty(props, "level");
			int turn = getIntProperty(props, "turn");
			
			if (level < 1 || level > Player.MAX_LEVEL || turn < 1) {
			    throw new IllegalArgumentException("Invalid level or turn");
			}
			
			return new SlotSummary(slot, SlotStatus.READABLE, name, level, turn);
		} catch (NoSuchFileException e) {
			return new SlotSummary(slot, SlotStatus.EMPTY, null, 0, 0);
		} catch(IOException | IllegalArgumentException | PropertyNotFoundException e) {
			return new SlotSummary(slot,SlotStatus.UNREADABLE, null, 0, 0);
		}
	}
	
	/** All three slots, for drawing the load/save screen. */
	public SlotSummary[] readAllSummaries() {
		SlotSummary[] summaries = new SlotSummary[SLOT_COUNT];
		for (int slot = 1; slot <= SLOT_COUNT; slot++) {
			summaries[slot - 1] = readSummary(slot);
		}
		return summaries;
	}

	/** Writes the current game into a slot (overwrites if present). */
	public void writeSave(int slot, GameState state){
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
		
		try {
			Files.createDirectories(saveDir);
		} catch (IOException e) {
			UIHandler.print("Cannot create directory: " + saveDir);
			e.printStackTrace();
		}
		Path path = getSlotFile(slot);
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			props.store(writer, COMMENT);
		} catch (IOException e) {
			UIHandler.print("Cannot write to save: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/** Loads a slot into a new GameState. 
	 * @throws NoSuchFileException
	 * @throws PropertyNotFoundException */
	public GameState loadSave(int slot){
		try {
			Properties props = loadProps(getSlotFile(slot));

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
		} catch (IOException e1) {
			UIHandler.print("Cannot load file at slot " + slot);
			e1.printStackTrace();
		} catch (PropertyNotFoundException e2) {
			throw new PropertyNotFoundException("Property not found at slot: " + slot ,e2);
		}
		return null;
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
			throw new PropertyNotFoundException("String Property value not found: " + key);
		}
		return raw.strip();
	}

	/** helper method for parsing int 
	 * @throws PropertyNotFoundException */
	private static int getIntProperty(Properties props, String key){
		return Integer.parseInt(getStringProperty(props,key));
	}

	/** helper method for parsing double
	 * @throws PropertyNotFoundException */
	private static double getDoubleProperty(Properties props, String key){
		double value = Double.parseDouble(getStringProperty(props,key));
		if (!Double.isFinite(value)) {
			throw new IllegalArgumentException("Invalid number: " + key);
		}
		return value;
	}
}
