package com.endlessadventure;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.random.RandomGenerator;

import com.endlessadventure.audio.Sound;
import com.endlessadventure.combat.Battle;
import com.endlessadventure.data.EnemyCatalog;
import com.endlessadventure.data.EquipmentCatalog;
import com.endlessadventure.entity.Player;
import com.endlessadventure.llm.LlmRequestException;
import com.endlessadventure.save.BadSaveException;
import com.endlessadventure.save.SaveManager;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.story.StoryGenerator;
import com.endlessadventure.ui.AdventureScreen;
import com.endlessadventure.ui.CombatScreen;
import com.endlessadventure.ui.CharacterCreationScreen;
import com.endlessadventure.ui.CharacterScreen;
import com.endlessadventure.ui.InventoryScreen;
import com.endlessadventure.ui.MainMenuScreen;
import com.endlessadventure.ui.SaveScreen;
import com.endlessadventure.ui.UIHandler;

/** core game loop, screen routing, and fundamental back-end logic */
public class GameEngine {
	private enum GameScreen {
		MAIN_MENU, CHARACTER_CREATION, LOAD_SAVE, WRITE_SAVE, ADVENTURE, INVENTORY, CHARACTER, COMBAT
	}

	private final UIHandler ui;
	private final SaveManager saveManager;
	private final StoryGenerator storyGenerator;
	private final EquipmentCatalog equipmentCatalog;
	private final EnemyCatalog enemyCatalog;

	private boolean running = true;
	private int selectedSlot = 1;
	private GameScreen currentScreen = GameScreen.MAIN_MENU;
	private GameState gameState = new GameState();

	public GameEngine(UIHandler ui, SaveManager saveManager, StoryGenerator storyGenerator, EquipmentCatalog equipmentCatalog, EnemyCatalog enemyCatalog) {
		this.ui = ui;
		this.saveManager = saveManager;
		this.storyGenerator = storyGenerator;
		this.equipmentCatalog = equipmentCatalog;
		this.enemyCatalog = enemyCatalog;
	}

	/** main game loop */
	public void run() throws NoSuchElementException{
		while (running) {
			//route music
			switch(currentScreen) {
				case MAIN_MENU, CHARACTER_CREATION, LOAD_SAVE -> Sound.playMusic(Sound.MENU);
				case ADVENTURE -> Sound.playMusic(Sound.ADVENTURE);
				case COMBAT -> Sound.playMusic(Sound.COMBAT);
				case INVENTORY, CHARACTER, WRITE_SAVE -> {}
			}
			//route screen transition states
			currentScreen = switch (currentScreen) {
				case MAIN_MENU -> handleMainMenu();
				case CHARACTER_CREATION -> handleCharacterCreation();
				case LOAD_SAVE -> handleSaveScreen(SaveScreen.Mode.LOAD);
				case WRITE_SAVE -> handleSaveScreen(SaveScreen.Mode.SAVE);
				case ADVENTURE -> handleAdventure();
				case INVENTORY -> handleInventory();
				case CHARACTER -> handleCharacter();
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
			yield null;
		}
		};
	}

	/** run character creation; start the adventure with the new player, or go back to the main menu */
	private GameScreen handleCharacterCreation() {
		if(new CharacterCreationScreen(ui, equipmentCatalog).run() instanceof Player p) {
			gameState.setPlayer(p);
			return GameScreen.ADVENTURE;
		}
		else {
			return GameScreen.MAIN_MENU;
		}
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
				ui.clearStatusMsg();
			} catch (LlmRequestException e) {
				ui.showError("Cannot process LLM request: " + e.getMessage());
				return GameScreen.MAIN_MENU;
			}
		}
		
		AdventureScreen.Choice choice = new AdventureScreen(ui, gameState).run();
		return switch(choice.action()) {
			case ACT -> {
				try {
					CurrentScene previous = gameState.getCurrentScene();
					gameState.setCurrentScene(storyGenerator.generateNext(gameState, choice.text()));
					gameState.addRecord(previous.toRecord(choice.text()));
					ui.clearStatusMsg();
				} catch (LlmRequestException e) {
					ui.showError("Cannot process LLM request: " + e.getMessage());
				}
				yield GameScreen.ADVENTURE;
			}
			case BACK -> GameScreen.MAIN_MENU;
			case SAVE -> GameScreen.WRITE_SAVE;
			case INV -> GameScreen.INVENTORY;
			case CHAR -> GameScreen.CHARACTER;
			case FIGHT -> GameScreen.COMBAT;// TODO remove when encounters arrive (step 4)
		};
	}

	/** handle inventory screen */
	private GameScreen handleInventory() {
		return switch(new InventoryScreen(ui, gameState.getPlayer()).run()) {
			case CLOSE -> GameScreen.ADVENTURE;
			case CHARACTER -> GameScreen.CHARACTER;
		};
	}

	/** handle character screen */
	private GameScreen handleCharacter() {
		return switch(new CharacterScreen(ui, gameState.getPlayer()).run()) {
			case CLOSE -> GameScreen.ADVENTURE;
			case INVENTORY -> GameScreen.INVENTORY;
		};
	}

	/** handle combat screen */
	private GameScreen handleCombat() {
		Battle battle = Battle.onRandomMap(gameState.getPlayer(),
		        List.of(enemyCatalog.createFromId("bandit")), RandomGenerator.getDefault());
		
		return switch(new CombatScreen(ui, battle, gameState.getCurrentScene().getLocation()).run()) {
			case BACK -> GameScreen.ADVENTURE;
			case INVENTORY -> GameScreen.INVENTORY;
			case CHARACTER -> GameScreen.CHARACTER;
		};
	}
}
