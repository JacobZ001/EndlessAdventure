package com.endlessadventure;

import com.endlessadventure.entity.Player;

public class GameDirector {
	private final UIHandler ui;
	private final SaveManager saveManager;
	private int slot = 0;
    private static boolean running;

	public GameDirector(UIHandler ui, SaveManager saveManager) {
		this.ui = ui;
		this.saveManager = saveManager;
	}
	
	public void run() {
	    GameScreen currentScreen = GameScreen.MAIN_MENU;
	    
	    running = true;
		while(running) {
			currentScreen = switch(currentScreen) {
			    case MAIN_MENU -> handleMainMenu();
			    case CHARACTER_CREATION -> handleCharacterCreation();
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
		String command = ui.promptInLine();
		
	    //main menu input handling
	    switch(command) {
	        case "1","new" -> {
	        	return GameScreen.CHARACTER_CREATION;
	        }
	        case "2","load" -> {
	        	return GameScreen.LOAD_SAVE;
	    	}
	        case "3","help" -> {
	        	return GameScreen.HELP;
	        }
	        case "4", "exit" -> {
			    ui.print("Thank you for playing.");
			    running = false;
			    return null;
	        }
	        default -> {
	        	ui.printUnknown();
	        	return GameScreen.MAIN_MENU;
	        }
	    }
    }
	
    private GameScreen handleCharacterCreation() {
    	//TODO create handle character creation
    	ui.renderCharacterCreationUI();
    	String name = ui.promptInLine("Adventurer, what is your name?");
    	String
    	return null;
	}
    
    private GameScreen handleLoadSave() {
    	//TODO create handle load save
    	ui.renderLoadSaveUI();
		String command = ui.promptInLine();
    	switch(command) {
			case "b","back" -> {return GameScreen.MAIN_MENU;}
			default -> {
	        	ui.printUnknown();
	        	return GameScreen.HELP;
			}
    	}
    }
    
	private GameScreen handleAdventure() {
		ui.renderAdventureUI();
		//TODO create handle adventure
		return null;
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
		String command = ui.promptInLine();
    	switch(command) {
    		case "b","back" -> {return GameScreen.MAIN_MENU;}
    		default -> {
	        	ui.printUnknown();
	        	return GameScreen.HELP;
    		}
    	}
    }
}
