package com.endlessadventure;

import com.endlessadventure.SaveManager.SlotStatus;
import com.endlessadventure.SaveManager.SlotSummary;
import com.endlessadventure.entity.Player;
import com.endlessadventure.llm.LlmClient;
import com.endlessadventure.llm.LlmRequestException;

public class GameEngine {
	private enum GameScreen {
	    MAIN_MENU, CHARACTER_CREATION, WRITE_SAVE, LOAD_SAVE, ADVENTURE, INVENTORY, COMBAT, HELP
	}
	private final UIHandler ui;
	private final SaveManager saveManager;
	private final LlmClient llm;
	
    private static boolean running;
    private GameScreen currentScreen;
    private SlotSummary[] slotSummaries;
    private GameState gameState;

	public GameEngine(UIHandler ui, SaveManager saveManager, LlmClient llm) {
		this.ui = ui;
		this.saveManager = saveManager;
		this.llm = llm;
	}
	
	/** core game loop */
	public void run() {
		currentScreen = GameScreen.MAIN_MENU;
	    running = true;
		while(running) {
			currentScreen = switch(currentScreen) {
			    case MAIN_MENU -> handleMainMenu();
			    case CHARACTER_CREATION -> handleCharacterCreation();
			    case WRITE_SAVE -> handleWriteSave();
			    case LOAD_SAVE -> handleLoadSave();
			    case ADVENTURE -> handleAdventure();
			    case INVENTORY -> handleInventory(); 
			    case COMBAT -> handleCombat();
			    case HELP -> handleHelp();
		    };
		}
	}
	
	/** read through all save slots, return true if any slot has an existing readable save */
	private boolean hasSave() {
		slotSummaries = saveManager.readAllSummaries();
		for(SlotSummary s : slotSummaries) {
			if(s.status() == SlotStatus.READABLE) {
				return true;
			}
		}
		return false;
	}
	
	private GameScreen handleMainMenu() {
    	ui.renderMainMenuUI(hasSave());
		String command = ui.prompt();
		
	    return switch(command) {
	        case "1","new" -> GameScreen.CHARACTER_CREATION;
	        case "2","load" -> GameScreen.LOAD_SAVE;
	        case "3","help" -> GameScreen.HELP;
	        case "4", "exit" -> {
			    UIHandler.print("\nGame Exited. Thank you for playing.");
			    running = false;
			    yield null;
	        }
	        default -> unknownCommand();
	    };
    }
	
    private GameScreen handleCharacterCreation() {
    	ui.renderCharacterCreationUI(null);
    	
    	String name = ui.prompt("",true);
    	Player player = new Player(name);
    	gameState = new GameState(player);
    	
    	ui.renderCharacterCreationUI(player);
		String command = ui.prompt();
		
    	return switch(command) {
    		case "c","continue" -> {
    			gameState = new GameState(player);
    			if(hasSave()) {
        			yield GameScreen.WRITE_SAVE;
    			}
    			else {
    				saveManager.writeSave(1, gameState);
    				yield GameScreen.ADVENTURE;
    			}
    		}
			case "b","back" -> GameScreen.MAIN_MENU;
			default -> unknownCommand();
    	};
	}
    
    private GameScreen handleWriteSave() {
    	ui.renderWriteSaveUI(slotSummaries);
    	String command = ui.prompt();
    	
    	return switch(command) {
			case "c","continue" -> GameScreen.ADVENTURE;
			case "b","back" -> GameScreen.MAIN_MENU;
			default -> unknownCommand();
    	};
    }
    
    private GameScreen handleLoadSave() {
    	//TODO create handle load save
    	ui.renderLoadSaveUI(slotSummaries);
		String command = ui.prompt();
		
    	return switch(command) {
    		case "c","continue" -> GameScreen.ADVENTURE;
			case "b","back" -> GameScreen.MAIN_MENU;
			default -> unknownCommand();
    	};
    }
    
	private GameScreen handleAdventure() {
		String systemInstruction = ""; //TODO implement system instruction for llm client
		String userContent = ""; //TODO implement user content for llm client
		try {
			String response = llm.generate(systemInstruction, userContent);//TDO implement llm IO 
		} catch (LlmRequestException e) {
			ui.print(e.getMessage());
			running = false;
		}
		ui.renderAdventureUI();
		//TODO create handle adventure
		String command = ui.prompt();
		
    	return switch(command) {
			case "b","back" -> GameScreen.MAIN_MENU;
		default -> unknownCommand();
	};
	}
	
	private GameScreen handleInventory() {
		ui.renderInventoryUI();
		//TODO create handle inventory
		return null;
	}

	private GameScreen handleCombat() {
		ui.renderCombatUI();
		//TODO create handle combat
		return null;
	}
	
    private GameScreen handleHelp() {
    	//render help screen and handle input
    	ui.renderHelpUI();
		String command = ui.prompt();
    	return switch(command) {
    		case "b","back" -> GameScreen.MAIN_MENU;
    		default -> unknownCommand();
		};
    }
    
    /** print status message for unknown command on next UI render */
    private GameScreen unknownCommand() {
		ui.setStatusMsg("Unknown Command, please try again.",UIHandler.RED);
    	return currentScreen;
    }
}
