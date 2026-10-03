package com.endlessadventure;

import com.endlessadventure.llm.LlmClient;

public class Driver {	
    public static void main(String[] args) {
		UIHandler ui = new UIHandler();
		SaveManager saveManager = new SaveManager();
		LlmClient llm = new LlmClient();
		GameEngine director = new GameEngine(ui, saveManager, llm);
		
		director.run();
    }
}
