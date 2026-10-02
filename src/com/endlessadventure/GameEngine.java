package com.endlessadventure;

import com.endlessadventure.entity.Player;

public class GameEngine {
	private final UIHandler ui;
	private final SaveManager saveManager;
    private static boolean running;
    private GameScreen currentScreen;
    private GameState gameState;

	public GameEngine(UIHandler ui, SaveManager saveManager) {
		this.ui = ui;
		this.saveManager = saveManager;
	}
	
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

	private GameScreen handleMainMenu() {
    	ui.renderMainMenuUI();
		String command = ui.prompt();
		
	    return switch(command) {
	        case "1","new" -> GameScreen.CHARACTER_CREATION;
	        case "2","load" -> GameScreen.LOAD_SAVE;
	        case "3","help" -> GameScreen.HELP;
	        case "4", "exit" -> {
			    ui.print("\nGame Exited. Thank you for playing.");
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
    		case "c","continue" -> GameScreen.WRITE_SAVE;
			case "b","back" -> GameScreen.MAIN_MENU;
			default -> unknownCommand();
    	};
	}
    
    private GameScreen handleWriteSave() {
    	ui.renderWriteSaveUI();
    	String command = ui.prompt();
    	
    	return switch(command) {
			case "c","continue" -> GameScreen.ADVENTURE;
			case "b","back" -> GameScreen.MAIN_MENU;
			default -> unknownCommand();
    	};
    }
    
    private GameScreen handleLoadSave() {
    	//TODO create handle load save
    	ui.renderLoadSaveUI();
		String command = ui.prompt();
		
    	return switch(command) {
    		case "c","continue" -> GameScreen.ADVENTURE;
			case "b","back" -> GameScreen.MAIN_MENU;
			default -> unknownCommand();
    	};
    }
    
	private GameScreen handleAdventure() {
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
    
    private GameScreen unknownCommand() {
    	try {
			ui.setStatusMsg("Unknown Command, please try again.",ui.RED);
		} catch (InvalidColorException e) {
			ui.print(e.getMessage());
			e.printStackTrace();
		}
    	return currentScreen;
    }
}
