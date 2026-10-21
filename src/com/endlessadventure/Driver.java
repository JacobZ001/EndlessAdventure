package com.endlessadventure;

import java.io.IOException;
import java.util.NoSuchElementException;

import com.endlessadventure.data.EquipmentCatalog;
import com.endlessadventure.llm.LlmClient;
import com.endlessadventure.save.SaveManager;
import com.endlessadventure.story.StoryGenerator;
import com.endlessadventure.ui.UIHandler;

public class Driver {	
    public static void main(String[] args) {
    	EquipmentCatalog catalog;
    	try {
    		// read predefined data from files
			 catalog = new EquipmentCatalog();
    	} catch(IOException e) {
            System.out.println("Cannot load game data: " + e.getMessage());
            return;
    	}
    	
    	//load game save, LLM client, and start game
		try(UIHandler ui = new UIHandler()) {
			SaveManager saveManager = new SaveManager(catalog);
			StoryGenerator storyGenerator = new StoryGenerator(new LlmClient());
			new GameEngine(ui, saveManager, storyGenerator, catalog).run();
		} catch (IOException e) {
			System.out.println("Terminal error: " + e.getMessage());
		} catch (NoSuchElementException e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("Game exited.");
		}
    }
}
