package com.endlessadventure;

import java.io.IOException;
import java.util.NoSuchElementException;

import com.endlessadventure.entity.Player;
import com.endlessadventure.llm.LlmRequestException;
import com.endlessadventure.save.BadSaveException;
import com.endlessadventure.save.SaveManager;
import com.endlessadventure.save.SaveManager.SlotOverview;
import com.endlessadventure.save.SaveManager.SlotStatus;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.story.StoryGenerator;

public class GameEngine {
	private enum GameScreen {
		MAIN_MENU, CHARACTER_CREATION, LOAD_SAVE, WRITE_SAVE, ADVENTURE, INVENTORY, COMBAT, HELP
	}

	private final UIHandler ui;
	private final SaveManager saveManager;
	private final StoryGenerator storyGenerator;

	private boolean running;
	private GameScreen currentScreen; // current screen to be displayed
	private SlotOverview[] slotOverviews;
	private GameState gameState = new GameState();

	public GameEngine(UIHandler ui, SaveManager saveManager, StoryGenerator storyGenerator) {
		this.ui = ui;
		this.saveManager = saveManager;
		this.storyGenerator = storyGenerator;
	}

	/** main game loop */
	public void run() {
		currentScreen = GameScreen.MAIN_MENU;
		running = true;
		try {
			while (running) {
				currentScreen = switch (currentScreen) {
				case MAIN_MENU -> handleMainMenu();
				case CHARACTER_CREATION -> handleCharacterCreation();
				case LOAD_SAVE -> handleLoadSave();
				case WRITE_SAVE -> handleWriteSave();
				case ADVENTURE -> handleAdventure();
				case INVENTORY -> handleInventory();
				case COMBAT -> handleCombat();
				case HELP -> handleHelp();
				};
			}
		} catch (NoSuchElementException e) {
			System.out.println("Input closed. Game exited.");
		} finally {
			running = false;
		}
	}

	/** check if there are any existing saves */
	/**
	 * include unreadable saves so they are visible and never silently overwritten
	 */
	private boolean hasExistingSaves() {
		slotOverviews = saveManager.readAllSummaries();
		for (SlotOverview s : slotOverviews) {
			if (s.status() != SlotStatus.EMPTY) {
				return true;
			}
		}
		return false;
	}

	/** handle main menu screen */
	/** display main menu and handle input */
	private GameScreen handleMainMenu() {
		ui.renderMainMenuUI(hasExistingSaves());

		String command = ui.prompt();

		return switch (command) {
		case "1", "[1]", "new" -> {
			gameState = new GameState();
			yield GameScreen.CHARACTER_CREATION;
		}
		case "2", "[2]", "load" -> GameScreen.LOAD_SAVE;
		case "3", "[3]", "help" -> GameScreen.HELP;
		case "4", "[4]", "exit" -> {
			System.out.println("\nGame Exited. Thank you for playing.");
			running = false;
			yield GameScreen.MAIN_MENU;
		}
		default -> handleGeneralCommand(command);
		};
	}

	/** handle character creation screen */
	private GameScreen handleCharacterCreation() {
		Player player = gameState.getPlayer();
		ui.renderCharacterCreationUI(player);

		if (gameState.getPlayer() == null) {
			String name = ui.prompt("", true);
			try {
				player = new Player(name);
				gameState.setPlayer(player);
			} catch (IllegalArgumentException e) {
				ui.showWarning(e.getMessage());
			}
			return GameScreen.CHARACTER_CREATION;
		}

		String command = ui.prompt();
		return switch (command) {
		case "b", "[b]" -> GameScreen.MAIN_MENU;
		case "c", "[c]" -> {
			System.out.println("Starting your adventure...");
			yield GameScreen.ADVENTURE;
		}
		case "r", "[r]" -> {
			gameState.setPlayer(null);
			yield GameScreen.CHARACTER_CREATION;
		}
		default -> handleGeneralCommand(command);
		};
	}

	/** handle load save screen */
	/**
	 * load save from slot and return GameScreen.ADVENTURE if successful, repeat
	 * same screen if not
	 */
	private GameScreen handleLoadSave() {
		slotOverviews = saveManager.readAllSummaries();
		
		ui.renderLoadSaveUI(slotOverviews);
		
		String input = ui.prompt();
		String[] parts = input.split("\\s+",2);
		String command = parts[0].toLowerCase();
		String argument = parts.length == 2 ? parts[1] : "";

		return switch (command) {
			case "1", "[1]" -> loadSave(1);
			case "2", "[2]" -> loadSave(2);
			case "3", "[3]" -> loadSave(3);
			case "b", "[b]" -> GameScreen.MAIN_MENU;
			case "d", "del", "delete" -> delSave(argument);
			default -> handleGeneralCommand(command);
		};
	}

	/** helper function to load save from slot */
	private GameScreen loadSave(int slot) {
		try {
			gameState = saveManager.loadSave(slot);
			ui.showSuccess("Loaded slot " + slot + ".");
			return GameScreen.ADVENTURE;
		} catch (IOException | BadSaveException e) {
			ui.showError("Cannot load save " + slot + ": " + e.getMessage());
			return GameScreen.LOAD_SAVE;
		}
	}

	/** handle write save screen */
	/**
	 * write save to slot and return GameScreen.ADVENTURE if successful, repeat same
	 * screen if not
	 */
	private GameScreen handleWriteSave() {
		slotOverviews = saveManager.readAllSummaries();
		
		ui.renderWriteSaveUI(slotOverviews);

		String input = ui.prompt();
		String[] parts = input.split("\\s+",2);
		String command = parts[0].toLowerCase();
		String argument = parts.length == 2 ? parts[1] : "";
		
		return switch (command) {
			case "1", "[1]" -> writeSave(1);
			case "2", "[2]" -> writeSave(2);
			case "3", "[3]" -> writeSave(3);
			case "b", "[b]" -> GameScreen.ADVENTURE;
			case "d", "del", "delete" -> delSave(argument);
			default -> handleGeneralCommand(command);
		};
	}

	/** helper function to write save to slot */
	private GameScreen writeSave(int slot) {
		SlotStatus status = slotOverviews[slot - 1].status();
		if (status != SlotStatus.EMPTY) {
			String message;
			if (status == SlotStatus.CORRUPT) {
				message = "This slot contains an unreadable save. Overwrite it? [Y/N]";
			} else {
				message = "There is an existing save in this slot. Overwrite it? [Y/N]";
			}
			String confirm = ui.prompt(message);
			if (!confirm.equals("y") && !confirm.equals("yes")) {
				return GameScreen.WRITE_SAVE;
			}
		}
		try {
			saveManager.writeSave(slot, gameState);
			ui.showSuccess("Saved to slot " + slot + ".");
			return GameScreen.WRITE_SAVE;
		} catch (IOException | BadSaveException e) {
			ui.showError("Cannot write save " + slot + ": " + e.getMessage());
			return GameScreen.WRITE_SAVE;
		}
	}
	
	private GameScreen delSave(String argument) {
		try {
			if(argument.isBlank()) {
				throw new IllegalArgumentException("Invalid command: delete, requires slot number selection.");
			}
			int slot = Integer.parseInt(argument);
			if(slot < 1 || slot > SaveManager.SLOT_COUNT) {
				throw new IllegalArgumentException("Invalid command: delete, slot number must be between 1 and " + SaveManager.SLOT_COUNT);
			}

			String confirm = ui.prompt("Delete save in slot " + slot + "? This action cannot be undone. [Y/N]");
			if (!confirm.equals("y") && !confirm.equals("yes")) {
				return currentScreen;
			}
			if(saveManager.deleteSave(slot)) {
				ui.showSuccess("Deleted save in slot " + slot + ".");
			}
			else {
				ui.showError("Slot " + slot + " is already empty.");
			}
		} catch(NumberFormatException e1) {
			ui.showError("Cannot process slot number: " + e1.getMessage());
		} catch(IllegalArgumentException e2) {
			ui.showError(e2.getMessage());
		} catch (IOException e3) {
			ui.showError("Cannot delete save: " + e3.getMessage());
		}
		return currentScreen;
	}

	/** handle adventure screen */
	private GameScreen handleAdventure() {
		//Initialize new game
		if(gameState.getCurrentScene() == null) {
			try {
				gameState.setCurrentScene(storyGenerator.generateOpening(gameState));
			} catch (LlmRequestException e) {
				ui.showError("Cannot process LLM request: " + e.getMessage());
				return GameScreen.MAIN_MENU;
			}
		}
		
		ui.renderAdventureUI(gameState);
		
		String command = ui.prompt("Action ");
    	return switch(command) {
    		//TODO implement inventory and other adventure general commands
    		case "b","[b]" -> {
				String confirm = ui.prompt("Are you sure to return to the main menu? Unsaved progress may be lost. (y/n)");
				yield confirm.equals("y") || confirm.equals("yes") ? GameScreen.MAIN_MENU : GameScreen.ADVENTURE;
    		}
	    	case "s","[s]" -> GameScreen.WRITE_SAVE; 
			default -> {
				String action = getPlayerAction(command, gameState.getCurrentScene().getOptions());
				if(!action.isBlank() && action != null) {
					try {
						CurrentScene temp = gameState.getCurrentScene();
						gameState.setCurrentScene(storyGenerator.generateNext(gameState, action));
						gameState.addRecord(temp.toRecord(action));
						yield GameScreen.ADVENTURE;
					} catch (LlmRequestException e) {
						ui.showError("Cannot process LLM request: " + e.getMessage());
						yield GameScreen.ADVENTURE;
					}
				}
				yield handleGeneralCommand(command);
			}
    	};
	}

	private String getPlayerAction(String command, String[] options) {
		for (int i = 0; i < options.length; i++) {
			if (command.equals(Integer.toString(i + 1)) || command.equals("[%s]".formatted(Integer.toString(i + 1)))) {
				return options[i];
			}
		}
		return command.strip();
	}

	/** handle inventory screen */
	private GameScreen handleInventory() {
		// TODO create handle inventory
		return GameScreen.ADVENTURE;
	}

	/** handle combat screen */
	private GameScreen handleCombat() {
		// TODO create handle combat
		return GameScreen.ADVENTURE;
	}

	/** handle help screen */
	private GameScreen handleHelp() {
		ui.renderHelpUI();
		String command = ui.prompt();
		return switch (command) {
		case "b", "[b]" -> GameScreen.MAIN_MENU;
		default -> handleGeneralCommand(command);
		};
	}

	/**
	 * handle general commands, return current screen and show warning if command is
	 * not recognized
	 */
	private GameScreen handleGeneralCommand(String command) {
		ui.showWarning("Unknown Command, please try again.");
		return currentScreen;
	}
}
