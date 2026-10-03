package com.endlessadventure;

import com.endlessadventure.Story.StoryGenerator;
import com.endlessadventure.llm.LlmClient;

public class Driver {	
    public static void main(String[] args) {
		UIHandler ui = new UIHandler();
		SaveManager sm = new SaveManager();
		StoryGenerator sg = new StoryGenerator(new LlmClient());
		GameEngine ge = new GameEngine(ui, sm, sg);
		
		ge.run();
    }
}
