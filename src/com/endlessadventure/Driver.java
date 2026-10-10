package com.endlessadventure;

import java.io.IOException;

import com.endlessadventure.llm.LlmClient;
import com.endlessadventure.save.SaveManager;
import com.endlessadventure.story.StoryGenerator;

public class Driver {	
    public static void main(String[] args) {
		try(UIHandler ui = new UIHandler()) {
			SaveManager saveManager = new SaveManager();
			StoryGenerator storyGenerator = new StoryGenerator(new LlmClient());
			new GameEngine(ui, saveManager, storyGenerator).run();
		} catch (IOException e) {
			System.out.println("Terminal error: " + e.getMessage());
		}
    }
}
