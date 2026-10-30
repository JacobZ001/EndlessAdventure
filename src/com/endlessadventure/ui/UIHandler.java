package com.endlessadventure.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Attributes;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.impl.MouseSupport;
import org.jline.utils.InfoCmp.Capability;

public class UIHandler implements AutoCloseable {
	private final Terminal terminal;
	private final PrintWriter writer;
	private final BindingReader bindingReader;
	private final Canvas canvas = new Canvas();
	
	private final KeyMap<InputKey> inputKeys = new KeyMap<>();
	private final List<ButtonBox> buttons = new ArrayList<>();
	private Button lastHoveredButton;
	private int mouseX = -1, mouseY = -1;

	private String statusMsg = null;
	private int color;
	private Attributes savedAttributes;
	
	enum InputKey { UP, DOWN, LEFT, RIGHT, ENTER, ESC, BACKSPACE, DEL, TAB, BACK_TAB, CHAR, CLICK, HOVER, MOUSE, INTERRUPT };

	/** one key or mouse event; button is the clickable area under the mouse for CLICK and HOVER, null otherwise */
	record Input(InputKey inputKey, char ch, int x, int y, Button button) {
		/** 0-based index of a typed digit 1-9 when it is below bound, or -1 for anything else */
		int digit(int bound) {
			int d = ch - '1';
			return Character.isDigit(ch) && d >= 0 && d < bound ? d : -1;
		}
	}
	
	private record ButtonBox(int x, int y, int w, int h, Button button) {
		boolean contains(int px, int py) {
			return px >= x && px < x + w && py >= y && py < y + h;
		}
	}

	public UIHandler() throws IOException {
		try {
			terminal = TerminalBuilder.builder().system(true).dumb(false).build();
		} catch (IllegalStateException e) {
			throw new IOException("No interactive terminal. Launch the game in Windows Terminal.", e);
		}
		writer = terminal.writer();
		
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
		inputKeys.bind(InputKey.BACKSPACE, "\b", "\177");
		inputKeys.bind(InputKey.DEL, KeyMap.key(terminal, Capability.key_dc));
		inputKeys.bind(InputKey.TAB, "\t");
		inputKeys.bind(InputKey.BACK_TAB, "\033[Z");
		for (char c = ' '; c <= '~'; c++) {
			inputKeys.bind(InputKey.CHAR, String.valueOf(c)); //printable ASCII
		}
		inputKeys.setAmbiguousTimeout(100);
				
		//switch to the alternate screen; the original terminal content comes back on close
		writer.print("\033[?1049h");
		writer.flush();
		
	    savedAttributes = terminal.enterRawMode();
	    terminal.puts(Capability.keypad_xmit);
	    terminal.puts(Capability.cursor_invisible);
	    terminal.trackMouse(Terminal.MouseTracking.Any);
	    terminal.flush();
	}
	
	/** the shared canvas that screens draw on */
	Canvas getCanvas() {
		return canvas;
	}

	@Override
	/** override close method for AutoCloseable
	 *  used for try-with-resource in driver */
	public void close() throws IOException {
		terminal.trackMouse(Terminal.MouseTracking.Off);
        terminal.puts(Capability.keypad_local);
        terminal.puts(Capability.cursor_visible);
        terminal.setAttributes(savedAttributes);
        savedAttributes = null;
        terminal.flush();
        
		writer.print("\033[?1049l"); //switch back to original terminal content
		writer.flush();
		terminal.close();
	}
	
	
	
	//show messages on next screen render
	public void showWarning(String msg) {
		setStatusMsg(msg,Palette.WARNING);
	}
	
	public void showError(String msg) {
		setStatusMsg(msg,Palette.DANGER);
	}

	public void showSuccess(String msg) {
		setStatusMsg(msg,Palette.SUCCESS);
	}
	
	public void clearStatusMsg() {
		statusMsg = null;
		color = Palette.BG;
	}

	/** set status message and color to be shown, throws InvalidColorException if the color is invalid */
	private void setStatusMsg(String msg, int color){
		statusMsg = msg;
		this.color = color;
	}
	
	
	
	/** print the canvas; a status message is drawn on the bottom border, right-aligned, until the next key or click */
	void present() {
	    int columns = terminal.getColumns(), rows = terminal.getRows();
	    if (columns < Canvas.WIDTH || rows < Canvas.HEIGHT) {
	    	String text = "Please maximize the window (" + Canvas.WIDTH + " x " + Canvas.HEIGHT + " needed, now " + columns + " x " + rows + ").";
	        writer.print("\033[0m" + "\033[2J" + Canvas.ESC + (rows / 2 + 1) + ";" + Math.max(1, (columns - text.length()) / 2 + 1) + "H" + text);
	        writer.flush();
	        return;
	    }
	    if (statusMsg != null) {
	    	String text = " " + statusMsg + " ";
	    	canvas.drawText(Canvas.WIDTH - 3 - text.length(), Canvas.HEIGHT - 1, text, color);
	    }
	    writer.print(canvas);
	    writer.flush();
	}
	

	
	/** reads raw user inputKey and return the input  */
	public Input readInput() {
        while (true) {
            InputKey key = bindingReader.readBinding(inputKeys);
            if (key == null || key == InputKey.INTERRUPT) {
            	throw new NoSuchElementException("Input closed: Key: " + key.toString());
            }
            if (key == InputKey.MOUSE) {
                MouseEvent e = terminal.readMouseEvent(bindingReader::readCharacter, bindingReader.getLastBinding());
                if(e.getType() == MouseEvent.Type.Moved) {
                	mouseX = e.getX();
                	mouseY = e.getY();
                	Button hoveredButton = getSelectedButton(mouseX, mouseY);
                	boolean changed = !Objects.equals(hoveredButton, lastHoveredButton);
                	lastHoveredButton = hoveredButton;
                    if (changed) {
                    	return new Input(InputKey.HOVER, '\0', mouseX, mouseY, hoveredButton);
                    }
                }
                else if (e.getType() == MouseEvent.Type.Pressed && e.getButton() == MouseEvent.Button.Button1) {
                    statusMsg = null;
                	mouseX = e.getX();
                	mouseY = e.getY();
                    return new Input(InputKey.CLICK, '\0', mouseX, mouseY, getSelectedButton(mouseX, mouseY));
                }
                continue; //other mouse events (moves within a button, releases, drags, wheel): keep reading
            }
            if (key != InputKey.UP && key != InputKey.DOWN) {
            	statusMsg = null; //moving the focus keeps the status message; any other key acts on it
            }
            char ch = (key == InputKey.CHAR)? bindingReader.getLastBinding().charAt(0) : '\0';
        	return new Input(key, ch, -1, -1, null);
        }
	}
	
	/** check for the button at position */
	private Button getSelectedButton(int x, int y) {
		for (ButtonBox b : buttons) {
	        if (b.contains(x, y)) {
	        	return b.button();
	        }
		}
		return null;
	}
	
	/** remove the buttons registered for the previous frame; call at the start of every canvas screen */
	void clearButtons() {
		buttons.clear();
	}

	/** register a click-able area for the current frame */
	void addButtonBox(int x, int y, int w, int h, Button b) {
		buttons.add(new ButtonBox(x, y, w, h, b));
	}
	
	/** draw a default sized button on canvas and register a click-able area for the current frame
	 *  while the mouse is over it the label and the frame turns gold */
	void drawButton(int x, int y, String label, boolean enabled, Button b) {
		ButtonBox buttonBox = new ButtonBox(x, y, Canvas.BUTTON_WIDTH, Canvas.BUTTON_HEIGHT, b);
	    canvas.drawButton(x, y, label, enabled, buttonBox.contains(mouseX, mouseY));
	    buttons.add(buttonBox);
	}
	
	/** draw a borderless one-row text button as wide as its label and register it, with one cell of click margin on each side;
	 *  while the mouse is over it the label turns gold and is underlined on the next row */
	void drawTextButton(int x, int y, String label, Button b) {
		ButtonBox buttonBox = new ButtonBox(x - 1, y, label.length() + 2, 1, b);
		boolean hovered = buttonBox.contains(mouseX, mouseY);
		canvas.drawText(x, y, label, hovered ? Palette.KEY : Palette.TEXT);
		if (hovered) {
			canvas.drawText(x, y + 1, "─".repeat(label.length()), Palette.KEY);
		}
		buttons.add(buttonBox);
	}
}
