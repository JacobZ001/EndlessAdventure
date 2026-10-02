package com.endlessadventure;

public class Driver {	
    public static void main(String[] args) {
		UIHandler ui = new UIHandler();
		SaveManager saveManager = new SaveManager();
		GameEngine director = new GameEngine(ui, saveManager);
		
		director.run();
    }
}
