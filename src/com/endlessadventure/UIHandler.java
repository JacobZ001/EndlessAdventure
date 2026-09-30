package com.endlessadventure;

import java.util.Scanner;

public class UIHandler {
	private Scanner sc;

	public UIHandler() {
		this.sc = new Scanner(System.in);
	}
	
	//clears the console to render new UIHandler
	public void clearScreen() {
	    System.out.print("\033[2J\033[3J\033[H");
	    System.out.flush();
	}
	
	//helper function for print line
	public void print(Object o) {
		System.out.println(o);
	}
	
	//print message for unknown command
	public void printUnknown() {
		System.out.println("Unknown Command, please try again.");
	}
	
	//prints the message and returns the user input on the next line
	public String prompt(String message) {
		print("");
		print(message + " > ");
	    return sc.nextLine().strip().toLowerCase();   
	}
	
	//default prompt overload with no custom message
	public String prompt() {
		return prompt("Input your choice");
	}
	
	//prints the message and returns the user input on the same line
	public String promptInLine(String message) {
		print("");
		System.out.print(message + " > ");
	    return sc.nextLine().strip().toLowerCase();
	}
	
	//default promptInLine overload with no custom message
	public String promptInLine() {
		return promptInLine("Input your choice");
	}
	
	public void renderMainMenuUI() {
		clearScreen();
		print("""
                _____ _   _ ____  _     _____ ____ ____
               | ____| \\ | |  _ \\| |   | ____/ ___/ ___|
               |  _| |  \\| | | | | |   |  _| \\___ \\___ \\
               | |___| |\\  | |_| | |___| |___ ___) |__) |
               |_____|_| \\_|____/|_____|_____|____/____/

            _    ____  __     _______ _   _ _____ _   _ ____  _____
           / \\  |  _ \\ \\ \\   / / ____| \\ | |_   _| | | |  _ \\| ____|
          / _ \\ | | | | \\ \\ / /|  _| |  \\| | | | | | | | |_) |  _|
         / ___ \\| |_| |  \\ V / | |___| |\\  | | | | |_| |  _ <| |___
        /_/   \\_\\____/    \\_/  |_____|_| \\_| |_|  \\___/|_| \\_\\_____|

        +----------------------------------------------------------+
        |                                                          |
        |                    [1] New Game                          |
        |                    [2] Load Game                         |
        |                    [3] How to Play                       |
        |                    [4] Exit                              |
        |                                                          |
        +----------------------------------------------------------+

        Enter 1-4 or: new / load / help / exit
        Press Enter to confirm.""");
	}
	
	public void renderCharacterCreationUI() {
		clearScreen();
		print("""
				CHARACTER CREATION
				+----------------------------------------------------------+""");
	}
	
	public void renderLoadSaveUI() {
		//TODO create load save UI
	}
	
	public void renderAdventureUI() {
		//TODO create adventure UI
		
	}

	public void renderInventoryUI() {
		//TODO create inventory UI
	}

	public void renderCombatUI() {
		//TODO create combat UI
	}

	public void renderHelpUI() {
		print("""
		        HOW TO PLAY
		        Enter a number or command, then press Enter.
		        
		        1 / new   - Start a new adventure
		        2 / load  - Load a saved adventure
		        3 / help  - Show this page
		        4 / quit  - Exit the game
		        
		        Enter b or back to return to the main menu.""");
	}
}
