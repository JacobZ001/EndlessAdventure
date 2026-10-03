package com.endlessadventure;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.endlessadventure.SaveManager.SlotOverview;
import com.endlessadventure.SaveManager.SlotStatus;
import com.endlessadventure.Story.Scene;
import com.endlessadventure.entity.Player;

public class UIHandler {
	public static final int UI_WIDTH = 100;
	public static final int TEXT_PADDING = 2;
	public static final int NARRATIVE_LINES = 25;
	
	//ANSI codes	
	private static final String BLACK = "\033[30m";
	private static final String RED = "\033[31m";
	private static final String GREEN = "\033[32m";
	private static final String YELLOW = "\033[33m";
	private static final String BLUE = "\033[34m";
	private static final String PURPLE = "\033[35m";
	private static final String CYAN = "\033[36m";
	private static final String WHITE = "\033[37m";
	
	private static final String CLEAR_SCREEN = "\033[2J\033[3J\033[H";
	private static final String RESET = "\033[0m";
	private static final Pattern ANSI_COLOR_PATTERN = Pattern.compile("\033\\[[0-9;]*m");
	
	//UI ASCII UI components symbols
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
	
	//show messages
	/** show warning message */
	public void showWarning(String msg) {
		setStatusMsg("? " +msg,YELLOW);
	}
	
	/** show error message */
	public void showError(String msg) {
		setStatusMsg(msg,RED);
	}

	/** show success message */
	public void showSuccess(String msg) {
		setStatusMsg(msg,GREEN);
	}

	/** show info message */
	public void showInfo(String msg) {
		setStatusMsg(msg,CYAN);
	}

	/** show message */
	public void showMessage(String msg) {
		setStatusMsg(msg,WHITE);
	}

	/** set status message and color to be shown, throws InvalidColorException if the color is invalid */
	private void setStatusMsg(String msg, String color){
		statusMsg = msg;
		if(!List.of(BLACK,RED,GREEN,YELLOW,BLUE,PURPLE,CYAN,WHITE).contains(color)) {
			throw new IllegalArgumentException("Invalid color: " + color);
		}
		else {
			this.color = color;
		}
	}
	
	
	//prompt user for input
	/** prints the message and returns the user input on the same line */
	public String prompt(String message, boolean raw) {
		System.out.println((Object) "");
		System.out.print(message + "> ");
		if(raw) return sc.nextLine().strip();
	    return sc.nextLine().strip().toLowerCase(Locale.ROOT);
	}
	
	/** overload of prompt with default raw = false */
	public String prompt(String message) {
		return prompt(message, false);
	}
	
	/** overload of prompt with default message and raw = false */
	public String prompt() {
		return prompt("",false);
	}


	//render UI
	/** clears the console to render new UIHandler */
	private void clearScreen() {
	    System.out.print(CLEAR_SCREEN);
	    System.out.flush();
	}
	
	/** render screen based on content */
	private void render(String content, String[] instructions) {
		clearScreen();
		System.out.println();
		System.out.println(content);
		for(String ins : instructions) {
			System.out.println(CYAN + "▽ " + ins + RESET);
		}
		sb.setLength(0);

		if (statusMsg != null) {
			System.out.println();
			System.out.println(color + statusMsg + RESET);
			statusMsg = null;
			color = null;
		}
	}
	
	public void renderMainMenuUI(boolean hasSave) {
		sb.append(buildBoxTop("ENDLESS ADVENTURE"));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxText(CYAN + "[1]" + RESET + " New Game"));
		sb.append(buildBoxEmptyRow());
		if(hasSave) {
			sb.append(buildBoxText(CYAN + "[2]" + RESET + " Load Game"));
			sb.append(buildBoxEmptyRow());
		}
		sb.append(buildBoxText(CYAN + "[3]" + RESET + " How to Play"));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxText(CYAN + "[4]" + RESET + " Exit"));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxBottomRich());
		render(sb.toString(),new String[]{"Enter [1][2][3][4] to select an option"});
	}
	
	public void renderCharacterCreationUI(Player player) {
		sb.append(buildBoxTop("CHARACTER CREATION"));
		sb.append(buildBoxEmptyRow());
		
		String[] ins = {"What is your name, adventurer?"};
		if(player != null) {
	    	sb.append(buildBoxText("%s  |  LV %s".formatted(player.getName(),player.getLevel())));
			sb.append(buildBoxText("HP %.0f / %.0f".formatted(player.getHp(),player.getMaxHp())));
			sb.append(buildBoxText("Attack %-10.0f | Armor %10.0f".formatted(player.getAttack(),player.getArmor())));
	    	sb.append(buildBoxEmptyRow());
			ins = new String[]{"Enter [c] or [continue] to continue",
					"Enter [b] or [back] to return to the main menu"};
		}
		sb.append(buildBoxBottomRich());
		render(sb.toString(),ins);
	}
	
	public void renderLoadSaveUI(SlotOverview[] slots) {
		buildSaveSlotUI("LOAD GAME", slots);
		String[] ins = {"Enter [1][2][3] to load a game","Enter [b] or [back] to return to the main menu"};
		render(sb.toString(),ins);
	}
	
	public void renderWriteSaveUI(SlotOverview[] slots) {
		buildSaveSlotUI("SAVE GAME", slots);
		String[] ins = {"Enter [1][2][3] to select a slot","Enter [b] or [back] to return to the previous screen"};
		render(sb.toString(),ins);
	}
	
	public void renderAdventureUI(GameState gameState) {
		//TODO render the actual scene when the adventure loop is implemented.
		Scene scene = gameState.getScene();
		sb.append(buildBoxTop("ADVENTURE"));
		sb.append(buildBoxText("Location: " + scene.getLocation(),true));
		sb.append(buildBoxDivider());
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxText(scene.getDescription()));
		sb.append(buildBoxEmptyRow());

		sb.append(buildBoxBottomRich());
		render(sb.toString(), new String[]{"Enter [s] or [save] to save your game", "Enter [b] or [back] to return to the main menu"});
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
	
	private void buildSaveSlotUI(String title, SlotOverview[] slots) {
		sb.append(buildBoxTop(title));
		sb.append(buildBoxEmptyRow());
		for(SlotOverview slot : slots) {
			if(slot.status() == SlotStatus.EMPTY) {
				sb.append(buildBoxText("[%d] Empty Slot".formatted(slot.slot())));
				sb.append(buildBoxEmptyRow());
			}
			else if (slot.status() == SlotStatus.CORRUPT) {
				sb.append(buildBoxText("[%d] Unreadable Save".formatted(slot.slot())));
				sb.append(buildBoxEmptyRow());
			}
			else {
				sb.append(buildBoxText("[%d] Lv %-5d %s".formatted(slot.slot(),slot.level(),slot.name())));
				sb.append(buildBoxText("     Last Turn %d".formatted(slot.turn())));
				sb.append(buildBoxEmptyRow());
			}
		}
		sb.append(buildBoxBottomRich());
	}
	
	
	//UI component building
	/** calculate the visible length of a text, ignoring ANSI escape codes */
	private int visibleLength(String text) {
		return ANSI_COLOR_PATTERN.matcher(text).replaceAll("").length();
	}

	/** find a cut position by visible width, keeping ANSI color codes intact */
	private int visibleEndIndex(String text, int width) {
		Matcher colorCodes = ANSI_COLOR_PATTERN.matcher(text);
		int index = 0;
		int visible = 0;
		while (index < text.length()) {
			if (text.charAt(index) == '\033') {
				colorCodes.region(index, text.length());
				if (colorCodes.lookingAt()) {
					index = colorCodes.end();
					continue;
				}
			}
			if (visible == width) {
				break;
			}
			index++;
			visible++;
		}
		return index;
	}

	/** build top border with title, can be centered */
	private String buildBoxTop(String title, boolean centered) {
		String titleSection = "%s %s %s".formatted(LINE_CAPS[0], YELLOW + title + RESET, LINE_CAPS[1]);
		int fill = UI_WIDTH - visibleLength(titleSection) - CORNERS[0].length() - CORNERS[1].length();
		if(centered) {
			int leftMargin = Math.floorDiv(fill, 2);
			int rightMargin = Math.ceilDiv(fill, 2);
			return CORNERS[0] + ROW_BORDER.repeat(leftMargin) + titleSection + ROW_BORDER.repeat(rightMargin) + CORNERS[1] + "\n";
		}
		return CORNERS[0] + titleSection + ROW_BORDER.repeat(fill) + CORNERS[1] + "\n";
	}
	
	/** overload of buildBoxTop with default centered = true */
	private String buildBoxTop(String title) {
		return buildBoxTop(title,true);
	}
	
	/** overload of buildBoxTop with no title */
	private String buildBoxTop() {
		return CORNERS[0] + ROW_BORDER.repeat(UI_WIDTH - CORNERS[0].length() - CORNERS[1].length()) + CORNERS[1] + "\n";
	}
	
	/** build bottom border */
	private String buildBoxBottom() {
		return CORNERS[2] + ROW_BORDER.repeat(UI_WIDTH - CORNERS[2].length() - CORNERS[3].length()) + CORNERS[3] + "\n"; 
	}
	
	/** build bottom border with default center design */
	private String buildBoxBottomRich() {
		int canvasWidth = UI_WIDTH - CENTER.length() - CORNERS[2].length() - CORNERS[3].length();
		int left = Math.floorDiv(canvasWidth, 2);
		int right = canvasWidth - left;
		return CORNERS[2] + ROW_BORDER.repeat(left) + CENTER + ROW_BORDER.repeat(right) + CORNERS[3] + "\n"; 
	}
	
	/** build a empty row with border */
	private String buildBoxEmptyRow() {
		return COL_BORDER + " ".repeat(UI_WIDTH - COL_BORDER.length() * 2) + COL_BORDER + "\n";
	}
	
	/** build divider */
	private String buildBoxDivider() {
	    return DIVIDERS[0] + ROW_BORDER.repeat(UI_WIDTH - DIVIDERS[0].length() - DIVIDERS[1].length()) + DIVIDERS[1] + "\n";
	}
	
	/** build horizontal line */
	private String buildHLine() {
	    return LINE_CAPS[0] + ROW_BORDER.repeat(UI_WIDTH - LINE_CAPS[0].length() - LINE_CAPS[1].length()) + LINE_CAPS[1] + "\n";
	}
	
	/** build horizontal line using center design */
	private String buildHLineRich(String center) {
		int canvasWidth = UI_WIDTH - visibleLength(center) - LINE_CAPS[0].length() - LINE_CAPS[1].length();
		int left = Math.floorDiv(canvasWidth, 2);
		int right = canvasWidth - left;
		return LINE_CAPS[0] + ROW_BORDER.repeat(left) + center + ROW_BORDER.repeat(right) + LINE_CAPS[1] + "\n";
	}
	
	/** build horizontal line using default center design */
	private String buildHLineRich() {
		return buildHLineRich(CENTER);
	}

	/** build horizontal line with Title */
	private String buildHLineTitle(String title) {
		String center = " %s ".formatted(YELLOW + title + RESET);
		return buildHLineRich(center);
	}
	
	/** build text, can be centered and can be boxed */
	private String buildText(String text, boolean centered, boolean boxed) {
		String border = boxed? COL_BORDER : "";
		int canvasWidth = UI_WIDTH - 2 * TEXT_PADDING - border.length() * 2;
		String[] words = text.strip().split("\\s+");
		StringBuilder result = new StringBuilder();
		StringBuilder line = new StringBuilder();
		
		for(String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (visibleLength(word) > canvasWidth) {
				if (!line.isEmpty()) {
					result.append(buildRow(line.toString(), canvasWidth, centered, border));
					line.setLength(0);
				}
				while (visibleLength(word) > canvasWidth) {
					int endIndex = visibleEndIndex(word, canvasWidth);
					result.append(buildRow(word.substring(0, endIndex), canvasWidth, centered, border));
					word = word.substring(endIndex);
				}
			}
			if(line.isEmpty()) {
				line.append(word);
			}
			else if (visibleLength(line.toString()) + 1 + visibleLength(word) <= canvasWidth) {
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
	
	/** build one row of text, can be centered and can be boxed */
	private String buildRow(String content, int canvasWidth, boolean centered, String border) {
		int contentWidth = visibleLength(content);
		if(centered) {
			int left = Math.floorDiv(canvasWidth - contentWidth, 2);
			int right = canvasWidth - contentWidth - left;
			return border + " ".repeat(TEXT_PADDING + left) + content + " ".repeat(right + TEXT_PADDING) + border + "\n";
		}
		return border + " ".repeat(TEXT_PADDING) + content + " ".repeat(canvasWidth - contentWidth + TEXT_PADDING) + border + "\n";
	}
	
	/** overload of buildText with default boxed = false */
	private String buildText(String text, boolean centered) {
		return buildText(text, centered, false);
	}
	
	/** overload of buildText with default centered = false, boxed = false */
	private String buildText(String text) {
		return buildText(text, false, false);
	}
	
	/** variant of buildText with default boxed = true */
	private String buildBoxText(String text, boolean centered) {
		return buildText(text,centered,true);
	}
	
	/** overload of buildBoxText with default centered = false */
	private String buildBoxText(String text) {
		return buildText(text, false, true);
	}
}
