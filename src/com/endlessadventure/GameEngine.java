package com.endlessadventure;

import java.io.IOException;
import java.util.NoSuchElementException;

import com.endlessadventure.entity.Player;
import com.endlessadventure.llm.LlmRequestException;
import com.endlessadventure.save.BadSaveException;
import com.endlessadventure.save.SaveManager;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.story.StoryGenerator;
import com.endlessadventure.ui.CharacterCreationScreen;
import com.endlessadventure.ui.MainMenuScreen;
import com.endlessadventure.ui.SaveScreen;
import com.endlessadventure.ui.UIHandler;

public class GameEngine {
	private enum GameScreen {
		MAIN_MENU, CHARACTER_CREATION, LOAD_SAVE, WRITE_SAVE, ADVENTURE, INVENTORY, COMBAT
	}

	private final UIHandler ui;
	private final SaveManager saveManager;
	private final StoryGenerator storyGenerator;

	private boolean running = true;
	private int selectedSlot = 1;
	private GameScreen currentScreen = GameScreen.MAIN_MENU;
	private GameState gameState = new GameState();

	public GameEngine(UIHandler ui, SaveManager saveManager, StoryGenerator storyGenerator) {
		this.ui = ui;
		this.saveManager = saveManager;
		this.storyGenerator = storyGenerator;
	}

	/** main game loop */
	public void run() throws NoSuchElementException{
		while (running) {
			currentScreen = switch (currentScreen) {
				case MAIN_MENU -> handleMainMenu();
				case CHARACTER_CREATION -> handleCharacterCreation();
				case LOAD_SAVE -> handleSaveScreen(SaveScreen.Mode.LOAD);
				case WRITE_SAVE -> handleSaveScreen(SaveScreen.Mode.SAVE);
				case ADVENTURE -> handleAdventure();
				case INVENTORY -> handleInventory();
				case COMBAT -> handleCombat();
			};
		}
	}

	/** check if there are any existing saves include unreadable saves so they are visible and never silently overwritten */
	private boolean hasExistingSaves() {
		for (SaveManager.SlotOverview s : saveManager.readAllSummaries()) {
			if (s.status() != SaveManager.SlotStatus.EMPTY) {
				return true;
			}
		}
		return false;
	}

	/** show the main menu and act on the player's choice */
	private GameScreen handleMainMenu() {
		return switch (new MainMenuScreen(ui, hasExistingSaves()).run()) {
		case NEW_GAME -> {
			gameState = new GameState();
			yield GameScreen.CHARACTER_CREATION;
		}
		case LOAD_GAME -> GameScreen.LOAD_SAVE;
		case EXIT -> {
			running = false;
			yield GameScreen.MAIN_MENU;
		}
		};
	}

	/** run character creation; start the adventure with the new player, or go back to the main menu */
	private GameScreen handleCharacterCreation() {
		Player player = new CharacterCreationScreen(ui).run();
		if (player == null) {
			return GameScreen.MAIN_MENU;
		}
		gameState.setPlayer(player);
		return GameScreen.ADVENTURE;
	}

	/** handle load and save screen */
	private GameScreen handleSaveScreen(SaveScreen.Mode mode) {
		boolean isLoadMode = mode == SaveScreen.Mode.LOAD;
		SaveScreen.Choice choice = new SaveScreen(ui, mode , saveManager.readAllSummaries(), selectedSlot).run();
		selectedSlot = choice.slot();
		return switch(choice.action()) {
			case CONFIRM -> {
				try {
					if(isLoadMode) {
						gameState = saveManager.loadSave(selectedSlot);
						ui.showSuccess("Loaded slot " + selectedSlot + ".");
						yield GameScreen.ADVENTURE;
					}
					else {
						saveManager.writeSave(selectedSlot, gameState);
						ui.showSuccess("Saved to slot " + selectedSlot + ".");
						yield GameScreen.WRITE_SAVE;
					}
				} catch (IOException | BadSaveException e) {
					ui.showError("Cannot " + mode.toString().toLowerCase() + " slot " + selectedSlot + ": " + e.getMessage());
					yield currentScreen;
				}
			}
			case DELETE -> {
				try {
					saveManager.deleteSave(selectedSlot);
					ui.showSuccess("Deleted save in slot " + selectedSlot + ".");
				} catch (IOException e3) {
					ui.showError("Cannot delete save: " + e3.getMessage());
				}
				yield currentScreen;
			}
			case BACK -> isLoadMode? GameScreen.MAIN_MENU : GameScreen.ADVENTURE;
		};
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

	/**
	 * handle general commands, return current screen and show warning if command is
	 * not recognized
	 */
	private GameScreen handleGeneralCommand(String command) {
		ui.showWarning("Unknown Command, please try again.");
		return currentScreen;
	}
}
