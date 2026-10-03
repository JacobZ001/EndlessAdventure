package com.endlessadventure;

import java.io.IOException;
import java.util.NoSuchElementException;

import com.endlessadventure.SaveManager;
import com.endlessadventure.SaveManager.SlotOverview;
import com.endlessadventure.SaveManager.SlotStatus;
import com.endlessadventure.Story.StoryGenerator;
import com.endlessadventure.entity.Player;
import com.endlessadventure.llm.LlmRequestException;

public class GameEngine {
	private enum GameScreen {
	    MAIN_MENU, CHARACTER_CREATION, LOAD_SAVE, WRITE_SAVE, ADVENTURE, INVENTORY, COMBAT, HELP
	}
	private final UIHandler ui;
	private final SaveManager saveManager;
	private final StoryGenerator storyGenerator;
	
    private boolean running;
    private GameScreen currentScreen; //current screen to be displayed
    private SlotOverview[] slotOverviews;
    private GameState gameState;

	public GameEngine(UIHandler ui, SaveManager saveManager, StoryGenerator storyGenerator) {
		this.ui = ui;
		this.saveManager = saveManager;
		this.gameState = new GameState();
		this.storyGenerator = storyGenerator;
	}
	
	/** main game loop */
	public void run() {
		currentScreen = GameScreen.MAIN_MENU;
	    running = true;
		try {
			while(running) {
				currentScreen = switch(currentScreen) {
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
	/** include unreadable saves so they are visible and never silently overwritten */
	private boolean hasExistingSaves() {
		slotOverviews = saveManager.readAllSummaries();
		for(SlotOverview s : slotOverviews) {
			if(s.status() != SlotStatus.EMPTY) {
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
		
	    return switch(command) {
	        case "1","[1]","new" -> {
				gameState = new GameState();
				yield GameScreen.CHARACTER_CREATION;
	        }
	        case "2","[2]","load" -> GameScreen.LOAD_SAVE;
	        case "3","[3]","h","help" -> GameScreen.HELP;
	        case "4","[4]", "exit" -> {
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

		if(gameState.getPlayer()== null) {
			String name = ui.prompt("",true);
			try {
				player = new Player(name);
				gameState.setPlayer(player);
			} catch (IllegalArgumentException e) {
				ui.showWarning(e.getMessage());
			}
			return GameScreen.CHARACTER_CREATION;
		}

		String command = ui.prompt();
    	return switch(command) {
    		case "b","back" -> GameScreen.MAIN_MENU;
    		case "c","continue" -> GameScreen.ADVENTURE;
			default -> handleGeneralCommand(command);
    	};
	}

	/** handle load save screen */
	/** load save from slot and return GameScreen.ADVENTURE if successful, repeat same screen if not */
    private GameScreen handleLoadSave() {
		slotOverviews = saveManager.readAllSummaries();
    	ui.renderLoadSaveUI(slotOverviews);
		String command = ui.prompt();
		
    	return switch(command) {
			case "1","[1]" -> loadSave(1);
			case "2","[2]" -> loadSave(2);
			case "3","[3]" -> loadSave(3);
			case "b","back" -> GameScreen.MAIN_MENU;
			default -> handleGeneralCommand(command);
    	};
    }
    
    /** helper function to load save from slot */
    private GameScreen loadSave(int slot) {
		try {
			gameState = saveManager.loadSave(slot);
			ui.showSuccess("Loaded slot " + slot + ".");
			return GameScreen.ADVENTURE;
		} catch (IOException e) {
			ui.showError("cannot load save " + slot + ": " + e.getMessage());
			return GameScreen.LOAD_SAVE;
		} catch (BadSaveException e) {
			ui.showError("Save " + e.getSlot() + " is invalid: " + e.getCause().getMessage());
			return GameScreen.LOAD_SAVE;
		}
    }
    
	/** handle write save screen */
	/** write save to slot and return GameScreen.ADVENTURE if successful, repeat same screen if not */
    private GameScreen handleWriteSave() {
		slotOverviews = saveManager.readAllSummaries();
    	ui.renderWriteSaveUI(slotOverviews);
    	
    	String command = ui.prompt();
    	return switch(command) {
			case "1","[1]" -> writeSave(1);
			case "2","[2]" -> writeSave(2);
			case "3","[3]" -> writeSave(3);
			case "b","back" -> GameScreen.ADVENTURE;
			default -> handleGeneralCommand(command);
    	};
    }
    
	/** helper function to write save to slot */
    private GameScreen writeSave(int slot) {
		SlotStatus status = slotOverviews[slot-1].status();
		if(status != SlotStatus.EMPTY) {
			String message;
			if (status == SlotStatus.CORRUPT) {
				message = "This slot contains an unreadable save. Overwrite it? (y/n)";
			}
			else {
				message = "There is an existing save in this slot. Overwrite it? (y/n)";
			}
			String confirm = ui.prompt(message);
			if(!confirm.equals("y") && !confirm.equals("yes")) {
				return GameScreen.WRITE_SAVE;
			}
		}
		try {
			saveManager.writeSave(slot, gameState);
			ui.showSuccess("Saved to slot " + slot + ".");
			return GameScreen.WRITE_SAVE;
		} catch (IOException e) {
			ui.showError("cannot write save " + slot + ": " + e.getMessage());
			return GameScreen.WRITE_SAVE;
		}
	}

	/** handle adventure screen */
	private GameScreen handleAdventure() {
		if(gameState.getTurn() == 1) {
			try {
				gameState.setScene(storyGenerator.generateOpening(gameState));
			} catch (LlmRequestException e) {
				ui.showError("cannot process LLM request: " + e.getMessage());
				return GameScreen.MAIN_MENU;
			}
		}
		
		ui.renderAdventureUI(gameState);
		
		String command = ui.prompt("Action ");
		String action = processPlayerAction(command, gameState.getScene().getOptions());
		if(action != null) {
			try {
				gameState.setScene(storyGenerator.generateNext(gameState, action));
				gameState.setTurn(gameState.getTurn() + 1);
				return GameScreen.ADVENTURE;
			} catch (LlmRequestException e) {
				ui.showError("cannot process LLM request: " + e.getMessage());
				return GameScreen.MAIN_MENU;
			}
		}
    	return switch(command) {
    		//TODO implement inventory and other adventure general commands
    		case "b","back" -> {
				String confirm = ui.prompt("Return to the main menu? Unsaved progress may be lost. (y/n)");
				yield confirm.equals("y") || confirm.equals("yes") ? GameScreen.MAIN_MENU : currentScreen;
    		}
	    	case "s","save" -> GameScreen.WRITE_SAVE; 
			default -> handleGeneralCommand(command);
		};
	}
	
	private String processPlayerAction(String command, String[] options) {
		for(int i=0; i<options.length; i++) {
			if(command.equals(Integer.toString(i+1)) || command.equals("[%s]".formatted(Integer.toString(i+1)))){
				return options[i];
			}
		}
			return null;
	}
	
	/** handle inventory screen */
	private GameScreen handleInventory() {
		//TODO create handle inventory
		ui.showInfo("Inventory is not available yet.");
		return GameScreen.ADVENTURE;
	}

	/** handle combat screen */
	private GameScreen handleCombat() {
		//TODO create handle combat
		ui.showInfo("Combat is not available yet.");
		return GameScreen.ADVENTURE;
	}
	
	/** handle help screen */
    private GameScreen handleHelp() {
    	ui.renderHelpUI();
		String command = ui.prompt();
    	return switch(command) {
			case "b","back" -> GameScreen.MAIN_MENU;
			default -> handleGeneralCommand(command);
		};
    }
    
    /** handle general commands, return current screen and show warning if command is not recognized */
    private GameScreen handleGeneralCommand(String command) {
		return switch(command) {
			default -> {
				ui.showWarning("Unknown Command, please try again.");
				yield currentScreen;
			}
		};
    }
}
