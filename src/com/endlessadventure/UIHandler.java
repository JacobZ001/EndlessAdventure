package com.endlessadventure;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.endlessadventure.entity.Player;
import com.endlessadventure.inventory.Inventory;
import com.endlessadventure.save.SaveManager;
import com.endlessadventure.save.SaveManager.SlotOverview;
import com.endlessadventure.save.SaveManager.SlotStatus;
import com.endlessadventure.story.CurrentScene;

public class UIHandler implements AutoCloseable {
	public static final int UI_WIDTH = Canvas.WIDTH;
	public static final int UI_HEIGHT = Canvas.HEIGHT;
	public static final int TEXT_PADDING = 2;
	public static final int[] INV_DIMENSIONS = {3,4};
	
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
	
	//Canvas component symbols
	public static final char[] CANVAS_CORNERS = {'┌','┐',
			   							         '└','┘'};
	public static final char CANVAS_ROW_BORDER = '─';
	public static final char CANVAS_COL_BORDER = '│';
	
	//UI ASCII components symbols
	private static final String[] CORNERS = {"┌<o+<",">+o>┐",
	                                         "└<o+<",">+o>┘"};
	private static final String ROW_BORDER = "═";
	private static final String COL_BORDER = "║";
	private static final String[] DIVIDERS = {"╠o>","<o╣"};
	private static final String[] LINE_CAPS = {"<o+<",">+o>"};
	private static final String CENTER = "<-+<\\o/>+->";

	private Terminal terminal;
	private LineReader reader;
	private Canvas canvas = new Canvas();
	private StringBuilder sb = new StringBuilder();

	private String statusMsg = null;
	private String color = null;

	public UIHandler() throws IOException {
		try {
			terminal = TerminalBuilder.builder().system(true).dumb(false).build();
		} catch (IllegalStateException e) {
			throw new IOException("No interactive terminal. Launch the game in Windows Terminal.", e);
		}
		reader = LineReaderBuilder.builder()
		        .terminal(terminal)
		        .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
		        .build();
	}
	
	@Override
	public void close() throws IOException {
		terminal.close();		
	}
	
	public void renderFrame() {
		terminal.writer().print(canvas.toAnsi());
		terminal.writer().flush();
	}
	
	private void present() {
	    if (terminal.getColumns() < Canvas.WIDTH || terminal.getRows() < Canvas.HEIGHT + 2) {
	        System.out.println("Please maximize the window.");
	        return;
	    }
	    System.out.flush();
	    terminal.writer().print(canvas.toAnsi());
	    terminal.writer().flush();
	}
	
	//show messages
	/** show warning message */
	public void showWarning(String msg) {
		setStatusMsg(msg,YELLOW);
	}
	
	/** show error message */
	public void showError(String msg) {
		setStatusMsg(msg,RED);
	}

	/** show success message */
	public void showSuccess(String msg) {
		setStatusMsg(msg,GREEN);
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
		System.out.println();
		System.out.flush();
		String line;
		try {
			line = reader.readLine(message + "> ");
		} catch (EndOfFileException | UserInterruptException e) {
			throw new NoSuchElementException("Input closed");
		}
		if(raw) return line.strip();
	    return line.strip().toLowerCase(Locale.ROOT);
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
			System.out.println(CYAN + /*"▽ " +*/ ins + RESET);
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
		sb.append(buildBoxBottom());
		render(sb.toString(),new String[]{"Enter [1-4] to select an option"});
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
			ins = new String[]{"[C] Continue",
					"[B] Return to Main Menu",
					"[R] Reset"};
		}
		sb.append(buildBoxBottom());
		render(sb.toString(),ins);
	}
	
	public void renderLoadSaveUI(SlotOverview[] slots) {
		buildSaveSlotUI("LOAD GAME", slots);
		String[] ins = {
				"[1-%d] Load Game".formatted(SaveManager.SLOT_COUNT),
				"[B] Return to Main Menu",
				"[D 1-%d] Delete Save".formatted(SaveManager.SLOT_COUNT)};
		render(sb.toString(),ins);
	}
	
	public void renderWriteSaveUI(SlotOverview[] slots) {
		buildSaveSlotUI("SAVE GAME", slots);
		String[] ins = {
				"[1-%d] Save to Slot".formatted(SaveManager.SLOT_COUNT),
				"[B] Return to Adventure",
				"[D 1-%d] Delete Save".formatted(SaveManager.SLOT_COUNT)};
		render(sb.toString(),ins);
	}
	
	public void renderAdventureUI(GameState gameState) {
		CurrentScene currentScene = gameState.getCurrentScene();
		sb.append(buildBoxTop("ADVENTURE"));
		sb.append(buildBoxText("Location: " + CYAN + currentScene.getLocation() + RESET));
		sb.append(buildBoxText("Turn " + gameState.getCurrentScene().getTurn()));
		sb.append(buildBoxEmptyRow());	
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxText(currentScene.getDescription()));
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxDivider());
		sb.append(buildBoxText(gameState.getPlayer().getName() + ", what will you do next?"));
		sb.append(buildBoxEmptyRow());
		
		String[] options = currentScene.getOptions();
		for(int i=0; i<options.length;i++) {
			sb.append(buildBoxText("[%d] %s".formatted(i+1,options[i])));
		}
		sb.append(buildBoxEmptyRow());
		sb.append(buildBoxBottom());
		render(sb.toString(), new String[]{
				"[1-%d] Choose an action, or enter your own intention.".formatted(options.length),
				"[S] Save",
				"[B] Return to Main Menu"});
	}

	public void renderInventoryUI(Player player, int selectedIndex) {
		canvas.clear(0);
		Inventory inventory = player.getInventory();
		
		int page = selectedIndex / 12;
		int localIndex = selectedIndex % 12;
		
		int row = localIndex / 4;
		int column = localIndex % 4;
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
				sb.append(buildBoxText("%s[%d]%s Empty Slot".formatted(CYAN, slot.slot(),RESET)));
				sb.append(buildBoxEmptyRow());
			}
			else if (slot.status() == SlotStatus.CORRUPT) {
				sb.append(buildBoxText("%s[%d]%s Unreadable Save".formatted(CYAN,slot.slot(),RESET)));
				sb.append(buildBoxEmptyRow());
			}
			else {
				sb.append(buildBoxText("%s[%d]%s LV %-5d %s | Turn %d".formatted(CYAN,slot.slot(),RESET,slot.level(),slot.name(),slot.turn())));
				sb.append(buildBoxEmptyRow());
			}
		}
		sb.append(buildBoxBottom());
	}
	
	
	//UI component string building
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
	
	/** build bottom border with default center design */
	private String buildBoxBottom() {
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
		String center = "%s %s %s".formatted(LINE_CAPS[0], YELLOW + title + RESET, LINE_CAPS[1]);
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
