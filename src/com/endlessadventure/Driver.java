package com.endlessadventure;

import com.endlessadventure.llm.LlmClient;
import com.endlessadventure.save.SaveManager;
import com.endlessadventure.story.StoryGenerator;

public class Driver {	
    public static void main(String[] args) {
		UIHandler ui = new UIHandler();
		SaveManager saveManager = new SaveManager();
		StoryGenerator storyGenerator = new StoryGenerator(new LlmClient());
		GameEngine ge = new GameEngine(ui, saveManager, storyGenerator);
		
		ge.run();
    }
}
