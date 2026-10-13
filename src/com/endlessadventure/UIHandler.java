package com.endlessadventure;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
	private static final int UI_WIDTH = Canvas.WIDTH;
	private static final int UI_HEIGHT = Canvas.HEIGHT;
	private static final int TEXT_PADDING = 2;
	private static final int[] INV_DIMENSIONS = {3,4};
	
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
	
	//UI ASCII components symbols
	private static final String[] CORNERS = {"┌<o+<",">+o>┐",
	                                         "└<o+<",">+o>┘"};
	private static final String ROW_BORDER = "═";
	private static final String COL_BORDER = "║";
	private static final String[] DIVIDERS = {"╠o>","<o╣"};
	private static final String[] LINE_CAPS = {"<o+<",">+o>"};
	private static final String CENTER = "<-+<\\o/>+->";

	//screen-specific color roles, see Palette for the shared colors
	private static final int OPTION_KEY = Palette.GOLD;
	private static final int TITLE_TOP = Palette.GOLD;
	private static final int TITLE_BOT = Palette.EMBER;

	//Main menu title bitmaps, each '#' is drawn as 2x2 pixels
	private static final String[] TITLE_ENDLESS = {
			"#####.#...#.####..#.....#####..####..####",
			"#.....##..#.#...#.#.....#.....#.....#....",
			"#.....##..#.#...#.#.....#.....#.....#....",
			"####..#.#.#.#...#.#.....####...###...###.",
			"#.....#..##.#...#.#.....#.........#.....#",
			"#.....#..##.#...#.#.....#.........#.....#",
			"#####.#...#.####..#####.#####.####..####."};
	private static final String[] TITLE_ADVENTURE = {
			".###..####..#...#.#####.#...#.#####.#...#.####..#####",
			"#...#.#...#.#...#.#.....##..#...#...#...#.#...#.#....",
			"#...#.#...#.#...#.#.....##..#...#...#...#.#...#.#....",
			"#####.#...#.#...#.####..#.#.#...#...#...#.####..####.",
			"#...#.#...#..#.#..#.....#..##...#...#...#.#.#...#....",
			"#...#.#...#..#.#..#.....#..##...#...#...#.#..#..#....",
			"#...#.####....#...#####.#...#...#....###..#...#.#####"};

	private final Terminal terminal;
	private final PrintWriter writer;
	private final LineReader reader;
	private final Canvas canvas = new Canvas();
	private final StringBuilder sb = new StringBuilder();

	private String statusMsg = null;
	private String color = null;

	public UIHandler() throws IOException {
		try {
			terminal = TerminalBuilder.builder().system(true).dumb(false).build();
		} catch (IllegalStateException e) {
			throw new IOException("No interactive terminal. Launch the game in Windows Terminal.", e);
		}
		writer = terminal.writer();
		reader = LineReaderBuilder.builder()
		        .terminal(terminal)
		        .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
		        .build();
		
		canvas.clear(Palette.BG);
		
		//switch to the alternate screen; the original terminal content comes back on close
		writer.print("\033[?1049h");
		writer.flush();
	}
	
	

	@Override
	/** override close method for AutoCloseable
	 *  used for try-with-resource in driver */
	public void close() throws IOException {
		writer.print("\033[?1049l"); //switch back to original terminal content
		writer.flush();
		terminal.close();
	}
	
	
	
	//show messages on next screen render
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
		System.out.print("\033[" + (Canvas.HEIGHT + 2) + ";1H\033[K"); //input row: second row below the canvas
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
	/** print the canvas, then the status message on the row below it */
	private void renderCanvas() {
	    if (terminal.getColumns() < Canvas.WIDTH || terminal.getRows() < Canvas.HEIGHT + 2) {
	        System.out.println("Please maximize the window.");
	        return;
	    }
	    System.out.flush();
	    writer.print(canvas);
	    writer.print("\033[" + (Canvas.HEIGHT + 1) + ";1H\033[J"); //clear status and input rows below the canvas
	    if (statusMsg != null) {
	    	writer.print("  " + color + statusMsg + RESET);
	    	statusMsg = null;
	    	color = null;
	    }
	    writer.flush();
	}
	
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
	
	/** draw a title word centered horizontally at pixel row py0, with a shadow one pixel down-right */
	private void drawTitle(String[] bitmap, int py0) {
		int px0 = (Canvas.WIDTH - bitmap[0].length() * 2) / 2;
		int shadow = Palette.mix(TITLE_BOT, Palette.BG, 0.75);
		drawBitmap(bitmap, px0 + 1, py0 + 1, shadow, shadow); //render the title drop shadow
		drawBitmap(bitmap, px0, py0, TITLE_TOP, TITLE_BOT); //render the title text
	}

	/** draw each '#' of the bitmap as 2x2 pixels, colored from top to bottom as a vertical gradient */
	private void drawBitmap(String[] bitmap, int px0, int py0, int topColor, int bottomColor) {
		for (int row = 0; row < bitmap.length; row++) {
			for (int col = 0; col < bitmap[row].length(); col++) {
				if (bitmap[row].charAt(col) != '#') {
					continue;
				}
				for (int dy = 0; dy < 2; dy++) {
					for (int dx = 0; dx < 2; dx++) {
						int py = row * 2 + dy;
						double ratio = (double) py / (bitmap.length * 2 - 1); //ratio of current pixel y to last row
						canvas.drawPixel(px0 + col * 2 + dx, py0 + py, Palette.mix(topColor, bottomColor, ratio));
					}
				}
			}
		}
	}
	
	private void drawInstruction(List<Map.Entry<String, String>> ins) {
		int x = 2;
		for(Map.Entry<String, String> entry : ins) {
			String key = " [" + entry.getKey() + "] ";
			String value = entry.getValue() + "   ";
			canvas.drawText(x, Canvas.HEIGHT - 1, key, OPTION_KEY, Palette.BG);
			canvas.drawText(x+key.length(), Canvas.HEIGHT - 1, value, Palette.MUTED, Palette.BG);
			x += key.length() + value.length();
		}
	}

	private void drawFrame() {
		canvas.drawBox(0, 0, Canvas.WIDTH, Canvas.HEIGHT, Palette.MUTED, Palette.BG);
	}

	
	
	
	//public methods for rendering specific screens
	public void renderMainMenuUI(boolean hasSave) {
		canvas.clear(Palette.BG);
		
		int row = 17;
		drawTitle(TITLE_ENDLESS, row);
		drawTitle(TITLE_ADVENTURE, row + 16);
		
		row += TITLE_ENDLESS.length + TITLE_ADVENTURE.length - 3;
		canvas.drawTextCentered(row, "────────────────────  +  ────────────────────", Palette.MUTED, Palette.BG);

		String[] labels = {
				"New Game",
				"Load Game",
				"How to Play",
				"Exit"};
		
		int x = (Canvas.WIDTH / 2) - 7;
		row += 5;
		for (int i = 0; i < labels.length; i++) {
			boolean enabled = i != 1 || hasSave; //Load Game is dimmed without saves
			int y = row + i * 2;
			canvas.drawText(x, y, "[" + (i + 1) + "]", enabled ? OPTION_KEY : Palette.MUTED, Palette.BG);
			canvas.drawText(x + 5, y, labels[i], enabled ? Palette.TEXT : Palette.MUTED, Palette.BG);
		}

		drawFrame();
		drawInstruction(List.of(
				Map.entry("1-4","selection"),
				Map.entry("ENTER","confirm")
		));
		renderCanvas();
	}
	
	public void renderCharacterCreationUI(Player player) {
		sb.append(buildBoxTop("CHARACTER CREATION"));
		sb.append(buildBoxEmptyRow());
		
		String[] ins = {"What is your name, adventurer?"};
		if(player != null) {
			
	    	sb.append(buildBoxText("%s  |  LV %s".formatted(player.getName(),player.getLevel())));
			sb.append(buildBoxText("HP %d / %d".formatted(player.getHp(),player.getMaxHp())));
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
		canvas.clear(Palette.BG);
		drawFrame();
		canvas.drawText(2, 0, "INVENTORY", Palette.GOLD, Palette.BG);
		Inventory inventory = player.getInventory();
		
		int page = selectedIndex / 12;
		int localIndex = selectedIndex % 12;
		
		int row = localIndex / 4;
		int column = localIndex % 4;
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
	
	/** overload of buildText with default centered = false, boxed = false */
	private String buildText(String text) {
		return buildText(text, false, false);
	}
	
	/** overload of buildBoxText with default centered = false */
	private String buildBoxText(String text) {
		return buildText(text, false, true);
	}
}
