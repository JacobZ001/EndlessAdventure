package com.endlessadventure.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Attributes;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.impl.MouseSupport;
import org.jline.utils.InfoCmp.Capability;

import com.endlessadventure.GameState;
import com.endlessadventure.entity.Player;
import com.endlessadventure.save.SaveManager;
import com.endlessadventure.save.SaveManager.SlotOverview;
import com.endlessadventure.save.SaveManager.SlotStatus;
import com.endlessadventure.story.CurrentScene;

public class UIHandler implements AutoCloseable {
	private static final int UI_WIDTH = Canvas.WIDTH;
	private static final int TEXT_PADDING = 2;
	
	//ANSI codes
	private static final String YELLOW = "\033[33m";
	private static final String CYAN = "\033[36m";
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

	private final Terminal terminal;
	private final PrintWriter writer;
	private final LineReader reader;
	private final BindingReader bindingReader;
	
	public enum InputKey { UP, DOWN, LEFT, RIGHT, ENTER, ESC, CHAR, CLICK, HOVER, MOUSE, INTERRUPT }
	private final KeyMap<InputKey> inputKeys = new KeyMap<>();
	public record Input(InputKey inputKey, char ch, int x, int y) {}


	public enum InputAction { SELECT, MINUS, PLUS }
	public record Button(int index, InputAction inputAction) {}
	private record ButtonBox(int x, int y, int w, int h, Button button) {}
	private final List<ButtonBox> buttons = new ArrayList<>();
	private Button lastHoveredButton;
	
	private final Canvas canvas = new Canvas();
	private final StringBuilder sb = new StringBuilder();

	private String statusMsg = null;
	private int color;
	
	private Attributes savedAttributes;

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
		
		//bind terminal user input to InputMap
		bindingReader = new BindingReader(terminal.reader());
		inputKeys.bind(InputKey.UP, KeyMap.key(terminal, Capability.key_up));
		inputKeys.bind(InputKey.DOWN, KeyMap.key(terminal, Capability.key_down));
		inputKeys.bind(InputKey.LEFT, KeyMap.key(terminal, Capability.key_left));
		inputKeys.bind(InputKey.RIGHT, KeyMap.key(terminal, Capability.key_right));
		inputKeys.bind(InputKey.ENTER, "\r");
		inputKeys.bind(InputKey.ESC, KeyMap.esc());
		inputKeys.bind(InputKey.INTERRUPT, KeyMap.ctrl('C'), KeyMap.ctrl('D'));
		inputKeys.bind(InputKey.MOUSE, MouseSupport.keys(terminal));
		for (char c = 'a'; c <= 'z'; c++) inputKeys.bind(InputKey.CHAR, String.valueOf(c));
		for (char c = '0'; c <= '9'; c++) inputKeys.bind(InputKey.CHAR, String.valueOf(c));
		inputKeys.setAmbiguousTimeout(100);
		
		canvas.clear(Palette.BG);
		
		//switch to the alternate screen; the original terminal content comes back on close
		writer.print("\033[?1049h");
		writer.flush();
	}
	
	/** the shared canvas that screens draw on */
	Canvas getCanvas() {
		return canvas;
	}
	
	/** enter raw mode to capture key input and mouse click */
	private void enterRawMode() {
		if (savedAttributes != null) {
			return;
		}
	    savedAttributes = terminal.enterRawMode();
	    terminal.puts(Capability.keypad_xmit);
	    terminal.puts(Capability.cursor_invisible);
	    terminal.trackMouse(Terminal.MouseTracking.Any);
	    terminal.flush();
	}
	
	private void leaveRawMode() {
		if (savedAttributes == null) {
			return;
		}
		terminal.trackMouse(Terminal.MouseTracking.Off);
        terminal.puts(Capability.keypad_local);
        terminal.puts(Capability.cursor_visible);
        terminal.setAttributes(savedAttributes);
        savedAttributes = null;
        terminal.flush();
	}

	@Override
	/** override close method for AutoCloseable
	 *  used for try-with-resource in driver */
	public void close() throws IOException {
		leaveRawMode();
		writer.print("\033[?1049l"); //switch back to original terminal content
		writer.flush();
		terminal.close();
	}
	
	
	
	//show messages on next screen render
	/** show warning message */
	public void showWarning(String msg) {
		setStatusMsg(msg,Palette.WARNING);
	}
	
	/** show error message */
	public void showError(String msg) {
		setStatusMsg(msg,Palette.ERROR);
	}

	/** show success message */
	public void showSuccess(String msg) {
		setStatusMsg(msg,Palette.SUCCESS);
	}

	/** set status message and color to be shown, throws InvalidColorException if the color is invalid */
	private void setStatusMsg(String msg, int color){
		statusMsg = msg;
		this.color = color;
	}
	
	

	//user input reading
	/** prints the message on the bottom row and returns the user input on the same line */
	public String prompt(String message, boolean raw) {
		return promptAt(0, Canvas.HEIGHT - 1, message + " > ", raw);
	}

	/** reads one line typed at canvas position x,y, after the prompt text; screens draw their own input box around it */
	String promptAt(int x, int y, String promptText, boolean raw) {
		leaveRawMode();
		writer.print(Canvas.ESC + (y + 1) + ";" + (x + 1) + "H"); //canvas is 0-based, ANSI cursor positions are 1-based
		writer.flush();
		String line;
		try {
			line = reader.readLine(promptText);
		} catch (EndOfFileException | UserInterruptException e) {
			throw new NoSuchElementException("Input closed");
		}
		statusMsg = null;
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
	
	/** enters raw mode and reads raw user input */
	public Input readInput() {
		enterRawMode();
        while (true) {            
            InputKey key = bindingReader.readBinding(inputKeys);
            if (key == null || key == InputKey.INTERRUPT) {
            	throw new NoSuchElementException("Input closed");
            }
            if (key == InputKey.MOUSE) {
                MouseEvent e = terminal.readMouseEvent(bindingReader::readCharacter, bindingReader.getLastBinding());
                if(e.getType() == MouseEvent.Type.Moved) {
                	Button hoveredButton = getSelectedButton(e.getX(), e.getY());
                	boolean changed = !Objects.equals(hoveredButton, lastHoveredButton);
                	lastHoveredButton = hoveredButton; //null over empty space, so re-entering a button counts as a change
                    if (changed && hoveredButton != null) {
                    	return new Input(InputKey.HOVER, '\0', e.getX(), e.getY()); //hover keeps the status message
                    }
                }
                else if (e.getType() == MouseEvent.Type.Pressed && e.getButton() == MouseEvent.Button.Button1) {
                    statusMsg = null;
                    return new Input(InputKey.CLICK, '\0', e.getX(), e.getY());
                }
                continue; //other mouse events (moves within a button, releases, drags, wheel): keep reading
            }
            statusMsg = null;
            char ch = (key == InputKey.CHAR)? bindingReader.getLastBinding().charAt(0) : '\0';
        	return new Input(key, ch, -1, -1);
        }
	}
	
	/** check for the button at position */
	Button getSelectedButton(int x, int y) {
		for (ButtonBox b : buttons) {
	        if (x >= b.x() && x < b.x() + b.w() && y >= b.y() && y < b.y() + b.h()) return b.button();
		}
		return null;
	}
	
	/** remove the buttons registered for the previous frame; call at the start of every canvas screen */
	void clearButtons() {
		buttons.clear();
	}

	/** register a clickable area for the current frame */
	void addButton(int x, int y, int w, int h, Button b) {
		buttons.add(new ButtonBox(x,y,w,h,b));
	}


	
	//render UI
	/** print the canvas; a status message is drawn on the bottom border, right-aligned, until the next key or click */
	void present() {
	    if (terminal.getColumns() < Canvas.WIDTH || terminal.getRows() < Canvas.HEIGHT) {
	        writer.println("Please maximize the window.");
	        writer.flush();
	        return;
	    }
	    if (statusMsg != null) {
	    	String text = " " + statusMsg + " ";
	    	canvas.drawText(Canvas.WIDTH - 3 - text.length(), Canvas.HEIGHT - 1, text, color, Palette.BG);
	    }
	    writer.print(canvas);
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
			System.out.println(Palette.rgbToAnsi(Canvas.FOREGROUND, color) + statusMsg + RESET);
			statusMsg = null;
		}
	}
	
	
	
	//public methods for rendering specific screens
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
	}

	public void renderCombatUI() {
		//TODO create combat UI
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
	
	/** overload of buildBoxText with default centered = false */
	private String buildBoxText(String text) {
		return buildText(text, false, true);
	}
}
