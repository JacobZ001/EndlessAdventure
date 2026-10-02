package com.endlessadventure;

import java.util.List;
import java.util.Scanner;

import com.endlessadventure.entity.Player;

public class UIHandler {
	public static final int UI_WIDTH = 100;
	public static final int TEXT_PADDING = 2;
	public static final int NARRATIVE_LINES = 10;
	
	//ANSI codes	
	public static final String BLACK = "\033[30m";
	public static final String RED = "\033[31m";
	public static final String GREEN = "\033[32m";
	public static final String YELLOW = "\033[33m";
	public static final String BLUE = "\033[34m";
	public static final String PURPLE = "\033[35m";
	public static final String CYAN = "\033[36m";
	public static final String WHITE = "\033[37m";
	
	private static final String CLEAR_SCREEN = "\033[2J\033[3J\033[H";
	private static final String RESET = "\033[0m";
	
	//UI ASCII symbols
	private static final String[] CORNERS = {"┌<o+<",">+o>┐",
	                                         "└<o+<",">+o>┘"};
	private static final String ROW_BORDER = "═";
	private static final String COL_BORDER = "║";
	private static final String[] DIVIDERS = {"╠o>","<o╣"};
	private static final String[] LINE_CAPS = {"<o+<",">+o>"};
	private static final String CENTER = "<-+<\\o/>+->";

	private Scanner sc;
	private String statusMsg;
	private String color;
	private StringBuilder sb;

	public UIHandler() {
		sc = new Scanner(System.in);
		sb = new StringBuilder();
		statusMsg = null;
		color = null;
	}
	
	//helper function for print line
	public void print(Object o) {
		System.out.println(o);
	}
	
	//set status message and color to be shown, throws InvalidColorException if the color is invalid
	public void setStatusMsg(String msg, String color) throws InvalidColorException {
		statusMsg = msg;
		if(!List.of(BLACK,RED,GREEN,YELLOW,BLUE,PURPLE,CYAN,WHITE).contains(color)) {
			throw new InvalidColorException("Invalid color: " + color);
		}
		else {
			this.color = color;
		}
	}
	
	//set status message to be shown
	public void setStatusMsg(String msg) {
		statusMsg = msg;
	}
	
	//prints the message and returns the user input on the same line
	public String prompt(String message, boolean raw) {
		print("");
		System.out.print(message + "> ");
		if(raw) return sc.nextLine().strip();
	    return sc.nextLine().strip().toLowerCase();   
	}
	
	//overload of prompt with default raw = false
	public String prompt(String message) {
		return prompt(message, false);
	}
	
	//overload of prompt with default message and raw = false
	public String prompt() {
		return prompt("",false);
	}
	
	//clears the console to render new UIHandler
	private void clearScreen() {
	    System.out.print(CLEAR_SCREEN);
	    System.out.flush();
	}
	
	//render screen based on content
	private void render(String content, String[] instructions) {
		clearScreen();
		print("");
		print(content);
		for(String ins : instructions) {
			print(YELLOW + "▽ " + ins + RESET);
		}
		sb.setLength(0);

		if (statusMsg != null) {
			if (color == null) {
				print(statusMsg);
			}
			else {
				print(color + statusMsg + RESET);
			}
			statusMsg = null;
		}
		color = null;
	}
	
	public void renderMainMenuUI() {
		sb.append(buildBoxTop("ENDLESS ADVENTURE"));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxText("[1] New Game"));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxText("[2] Load Game"));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxText("[3] How to Play"));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxText("[4] Exit"));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxBottomRich());
		render(sb.toString(),new String[]{"Select [1/2/3/4], then press [Enter]"});
	}
	
	public void renderCharacterCreationUI(Player player) {
		sb.append(buildBoxTop("CHARACTER CREATION"));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxText("Your adventure awaits",true));
		sb.append(buildBoxEmptyRow());
		
		String[] ins = {"What is your name, adventurer?"};
		if(player != null) {
	    	sb.append(buildBoxText("%s  |  LV %s".formatted(player.getName(),player.getLevel())));
	    	sb.append(buildBoxText("HP %f / %f".formatted(player.getHP(),player.getMaxHP())));
	    	sb.append(buildBoxText("Attack %-10s | Armor %10s".formatted(player.getAttack(),player.getArmor())));
	    	sb.append(buildBoxEmptyRow());
			ins = new String[]{"Enter [c] or [continue] to continue",
					"Enter [b] or [back] to return to the main menu"};
		}
		sb.append(buildBoxBottomRich());
		render(sb.toString(),ins);
	}
	
	public void renderLoadSaveUI() {
		sb.append(buildBoxTop("LOAD GAME"));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxBottomRich());
	}
	
	public void renderWriteSaveUI() {
		sb.append(buildBoxTop("SELECT AN EMPTY SLOT"));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxBottomRich());
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
		sb.append(buildHLineTitle("HOW TO PLAY"));
		sb.append("\n");
		sb.append(buildText("Type a number or keyword shown on screen, then press Enter."));
		sb.append("\n");
		sb.append(buildText("Start a new game or load a save from the main menu. Explore, interact with other beings, manage your gear,"
				+ " and fight when enemies appear — each screen lists the commands you can use."));
		sb.append("\n");
		sb.append(buildHLineRich());
		render(sb.toString(),new String[]{"Enter [b] or [back] to return to the main menu"});
	}
	
	private void buildSaveSlotInfo() {
		//TODO create build save info
	}
	
	
	
	//UI component building
	//build top border
	private String buildBoxTop(String title, boolean centered) {
		String titleSection = "%s %s %s".formatted(LINE_CAPS[0], title, LINE_CAPS[1]);
		int fill = UI_WIDTH - titleSection.length() - CORNERS[0].length() - CORNERS[1].length();
		if(centered) {
			int leftMargin = Math.floorDiv(fill, 2);
			int rightMargin = Math.ceilDiv(fill, 2);
			return CORNERS[0] + ROW_BORDER.repeat(leftMargin) + titleSection + ROW_BORDER.repeat(rightMargin) + CORNERS[1] + "\n";
		}
		return CORNERS[0] + titleSection + ROW_BORDER.repeat(fill) + CORNERS[1] + "\n";
	}
	
	//overload of buildBoxTop with default centered = true
	private String buildBoxTop(String title) {
		return buildBoxTop(title,true);
	}
	
	//build top border without title
	private String buildBoxTop() {
		return CORNERS[0] + ROW_BORDER.repeat(UI_WIDTH - CORNERS[0].length() - CORNERS[1].length()) + CORNERS[1] + "\n";
	}
	
	//build bottom border
	private String buildBoxBottom() {
		return CORNERS[2] + ROW_BORDER.repeat(UI_WIDTH - CORNERS[2].length() - CORNERS[3].length()) + CORNERS[3] + "\n"; 
	}
	
	//build bottom border with default center design
	private String buildBoxBottomRich() {
		int canvasWidth = UI_WIDTH - CENTER.length() - CORNERS[2].length() - CORNERS[3].length();
		int left = Math.floorDiv(canvasWidth, 2);
		int right = canvasWidth - left;
		return CORNERS[2] + ROW_BORDER.repeat(left) + CENTER + ROW_BORDER.repeat(right) + CORNERS[3] + "\n"; 
	}
	
	//build a empty row with border
	private String buildBoxEmptyRow() {
		return COL_BORDER + " ".repeat(UI_WIDTH - COL_BORDER.length() * 2) + COL_BORDER + "\n";
	}
	
	//build divider
	private String buildBoxDivider() {
	    return DIVIDERS[0] + ROW_BORDER.repeat(UI_WIDTH - DIVIDERS[0].length() - DIVIDERS[1].length()) + DIVIDERS[1] + "\n";
	}
	
	//build horizontal line
	private String buildHLine() {
	    return LINE_CAPS[0] + ROW_BORDER.repeat(UI_WIDTH - LINE_CAPS[0].length() - LINE_CAPS[1].length()) + LINE_CAPS[1] + "\n";
	}
	
	//build horizontal line using center design
	private String buildHLineRich(String center) {
		int canvasWidth = UI_WIDTH - center.length() - LINE_CAPS[0].length() - LINE_CAPS[1].length();
		int left = Math.floorDiv(canvasWidth, 2);
		int right = canvasWidth - left;
		return LINE_CAPS[0] + ROW_BORDER.repeat(left) + center + ROW_BORDER.repeat(right) + LINE_CAPS[1] + "\n";
	}
	
	//build horizontal line using default center design
	private String buildHLineRich() {
		return buildHLineRich(CENTER);
	}

	//build horizontal line with Title
	private String buildHLineTitle(String title) {
		String center = " %s ".formatted(title);
		return buildHLineRich(center);
	}
	
	//build text, can be centered and can be boxed
	private String buildText(String text, boolean centered, boolean boxed) {
		String border = boxed? COL_BORDER : "";
		int canvasWidth = UI_WIDTH - 2 * TEXT_PADDING - border.length() * 2;
		String[] words = text.strip().split(" ");
		StringBuilder result = new StringBuilder();
		StringBuilder line = new StringBuilder();
		
		for(String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if(line.isEmpty()) {
				line.append(word);
			}
			else if (line.length() + 1 + word.length() <= canvasWidth) {
				line.append(' ').append(word);
			}
			else {
				result.append(buildRow(line.toString(), canvasWidth, centered, border));
				line.setLength(0);
				line.append(word);
			}
		}
		if(!line.isEmpty()) {
			result.append(buildRow(line.toString(), canvasWidth, centered, border));
		}
		return result.toString();
	}
	
	//build one row of text, can be centered and can be boxed
	private String buildRow(String content, int canvasWidth, boolean centered, String border) {
		if(centered) {
			int left = Math.floorDiv(canvasWidth - content.length(), 2);
			int right = canvasWidth - content.length() - left;
			return border + " ".repeat(TEXT_PADDING + left) + content + " ".repeat(right + TEXT_PADDING) + border + "\n";
		}
		return border + " ".repeat(TEXT_PADDING) + content + " ".repeat(canvasWidth - content.length() + TEXT_PADDING) + border + "\n";
	}
	
	//overload of buildText with default boxed = false
	private String buildText(String text, boolean centered) {
		return buildText(text, centered, false);
	}
	
	//overload of buildText with default centered = false, boxed = false
	private String buildText(String text) {
		return buildText(text, false, false);
	}
	
	//variant of buildText with default boxed = true
	private String buildBoxText(String text, boolean centered) {
		return buildText(text,centered,true);
	}
	
	//overload of buildBoxText with default centered = false
	private String buildBoxText(String text) {
		return buildText(text, false, true);
	}
}
