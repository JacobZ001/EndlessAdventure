package com.endlessadventure;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

import com.endlessadventure.audio.Sound;
import com.endlessadventure.combat.Battle;
import com.endlessadventure.data.EnemyCatalog;
import com.endlessadventure.data.EquipmentCatalog;
import com.endlessadventure.entity.Enemy;
import com.endlessadventure.entity.Player;
import com.endlessadventure.llm.LlmRequestException;
import com.endlessadventure.save.BadSaveException;
import com.endlessadventure.save.SaveManager;
import com.endlessadventure.story.CurrentScene;
import com.endlessadventure.story.Move;
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
	
	/** defined a custom supplier interface as jdk vanilla supplier cannot throw a checked exception */
	@FunctionalInterface
	private interface StoryRequester {
		StoryGenerator.StoryResult request() throws LlmRequestException; 
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
	public void run() {
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
		if(gameState.getCurrentScene() == null && !advanceStory(() -> storyGenerator.generateOpening(gameState), "")) {
			return GameScreen.MAIN_MENU;
		}
		
		List<String> enemyIds = gameState.getCurrentScene().getEnemyIds();
		String enemyText = enemyIds.isEmpty()? null : storyGenerator.describeEnemies(enemyIds);
		AdventureScreen.Choice choice = new AdventureScreen(ui, gameState, enemyText).run();
		return switch(choice.action()) {
			case ACT -> {
				advanceStory(() -> storyGenerator.generateNext(gameState, choice.text()), choice.text());
				yield GameScreen.ADVENTURE;
			}
			case SAVE -> GameScreen.WRITE_SAVE;
			case INV -> GameScreen.INVENTORY;
			case CHAR -> GameScreen.CHARACTER;
			case FIGHT -> GameScreen.COMBAT;
			case REST -> {
				Player p = gameState.getPlayer();
				int before = p.getHp();
				int amount =  (int) Math.round(p.getMaxHp() * gameState.getCurrentLocation().kind().restRecoveryRatio);
				boolean interrupted = gameState.isProtected()? false : 
					RandomGenerator.getDefault().nextDouble() < gameState.getCurrentLocation().kind().restInterruptChance;
				
				p.restoreHP(interrupted? amount / 2 : amount); //heals half of original amount if interrupted
				//if LLM request failed, then the rest and healing is reverted
				if(interrupted) {
					if(!advanceStory(() -> storyGenerator.generateAfterInterruptedRest(gameState, before),
							storyGenerator.interruptedRestSummary(before, p))) {
						p.setHp(before);
					}
				}
				else if(!advanceStory(() -> storyGenerator.generateAfterRest(gameState, before), 
						storyGenerator.restSummary(before, p))){
					p.setHp(before); 
				}
				else {
					gameState.setProtected(false);
				}
				yield GameScreen.ADVENTURE;
			}
			case BACK -> GameScreen.MAIN_MENU;
		};
	}
	
	/** asks for the next scene; on success it replaces the current one, which goes into history with action.
	 *  on failure the state stays as it was, the error is shown, and it returns false */
	private boolean advanceStory(StoryRequester requester, String action) {
		CurrentScene previous = gameState.getCurrentScene();
		try {
			StoryGenerator.StoryResult result = requester.request();
			if(previous != null) {
				gameState.addToHistory(previous.toPastScene(action, gameState.getCurrentRoom().id()));
			}
			gameState.setCurrentScene(result.scene());
			switch(result.move()) {
				case Move.Stay s -> {}
				case Move.ToRoom(String id) -> gameState.moveTo(id);
				case Move.NewRoom(String name, String backdrop) -> {
					WorldMap map = gameState.getWorldMap();
					WorldMap.Room newRoom = map.addRoom(gameState.getCurrentLocation().id(), name, backdrop);
					map.connect(gameState.getCurrentRoom().id(), newRoom.id());
					gameState.moveTo(newRoom.id());
				}
				case Move.NewLocation(String name, WorldMap.LocationKind kind, String theme, int danger, String room, String backdrop) -> {
					WorldMap map = gameState.getWorldMap();
					WorldMap.Location newLocation = map.addLocation(name, kind, theme, danger);
					WorldMap.Room newRoom = map.addRoom(newLocation.id(), room, backdrop);
					if(gameState.getCurrentRoom() != null) {
						map.connect(gameState.getCurrentRoom().id(), newRoom.id());
					}
					gameState.moveTo(newRoom.id());
				}
			}
			ui.clearStatusMsg();
			return true;
		} catch (LlmRequestException e) {
			ui.showError("Cannot process LLM request: " + e.getMessage());
			return false;
		}
	}


	/** handle inventory screen */
	private GameScreen handleInventory() {
		return switch(new InventoryScreen(ui, gameState.getPlayer(), false).run()) {
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
		List<String> eids = gameState.getCurrentScene().getEnemyIds();
		List<Enemy> enemies = eids.stream().map(enemyCatalog::createFromId).toList();
		
		//number enemies that share a template, e.g. Bandit 1, Bandit 2
		for(int i = 0; i < enemies.size(); i++) {
			String id = eids.get(i);
			if(Collections.frequency(eids, id) > 1) {
				Enemy e = enemies.get(i);
				e.setName(e.getName() + " " + Collections.frequency(eids.subList(0, i + 1), id));
			}
		}
		Battle battle = Battle.onRandomMap(gameState.getPlayer(), enemies, RandomGenerator.getDefault());
		
		Battle.Outcome outcome = new CombatScreen(ui, battle, gameState.getCurrentLocation().name()).run(); 
		if(outcome == Battle.Outcome.DEFEATED) {
			//a downed player comes to with 1 HP for story progression purpose, the story then tells where they wake
			gameState.getPlayer().setHp(1);
			gameState.setProtected(true);
		}
		
		
		//shows the previous scene under the waiting card while the model writes the aftermath
		new AdventureScreen(ui, gameState, null).showWaiting(); 
		advanceStory(() -> storyGenerator.generateAfterCombat(gameState, outcome),
				storyGenerator.combatSummary(gameState.getCurrentScene().getEnemyIds(), outcome, gameState.getPlayer()));
		return GameScreen.ADVENTURE;
	}
}
