package com.endlessadventure.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.endlessadventure.combat.Battle;
import com.endlessadventure.combat.Battle.CombatEvent;
import com.endlessadventure.combat.BattleMap;
import com.endlessadventure.combat.BattleMap.Terrain;
import com.endlessadventure.combat.Combatant;
import com.endlessadventure.combat.Combatant.AttackType;
import com.endlessadventure.combat.Combatant.DefenseType;
import com.endlessadventure.combat.Combatant.Side;
import com.endlessadventure.combat.Position;
import com.endlessadventure.entity.Attribute;
import com.endlessadventure.entity.Enemy;
import com.endlessadventure.entity.GameCharacter;
import com.endlessadventure.entity.Player;
import com.endlessadventure.item.Equipment;
import com.endlessadventure.item.Equipment.Slot;

public class CombatScreen extends Screen {
	public enum ScreenAction { BACK, INVENTORY, CHARACTER }
	public enum CombatAction { ACT, END }
	
	//figure image ids
	private static final String TILE_IMG = "forest_floor",
								TREE_IMG = "forest_tree",
								PLAYER_IMG = "player";
	
	//colors
	private static final int PLAYER_COLOR = Palette.colorOf(Attribute.DEX),
							 ENEMY_COLOR = Palette.DANGER;
	
	//map and middle section positioning
	private static final int TITLE_Y = 2;
	private static final int CELL = 12,
							 MAP_X = 44, MAP_Y = 5 * 2, //pixel coordinates
							 MAP_W = 120;
	//Map rendering
	private static final int FOOT_Y_OFFSET = -3,
							 HP_BAR_Y_OFFSET = 3,
							 HP_BAR_X_OFFSET = 1, //x offset from cell x
							 HP_BAR_X_SPAN = 10;
	
	//side panel positioning
	private static final int LEFT_X = 3, RIGHT_X = MAP_X + MAP_W + 3, //167
							 SIDE_Y = 2,
							 SIDE_W = 38, IMG_W = 15, INFO_TEXT_W = 16, 
							 LEFT_TOP_H = 15, LEFT_BOT_H = 32, LEFT_PANEL_MARGIN = 1,   //side panels, rows 1-49
							 RIGHT_TOP_H = 15, RIGHT_BOT_H = 32, RIGHT_PANEL_MARGIN = 1,
							 PAD = 3;
	
	//initiative bar positioning
	private static final int CARD_Y = 48,
							 CARD_W_BUFFER = 8,
							 CARD_MARGIN = 3;
	
	private record SceneObject(Position cell, PixelImage image, boolean isUnit, Combatant unit) {
		static SceneObject ofTile(Position cell, String image) {
			PixelImage i = PixelImage.figure(image).get(); //TODO change when decided to let game run without sprites
			return new SceneObject(cell, i, false, null);
		}
		
		static SceneObject ofUnit(Position cell, String image, Combatant unit) {
			PixelImage i = PixelImage.figure(image).get(); //TODO change when decided to let game run without sprites
			return new SceneObject(cell, i, true, unit);
		}
	}
	
	private record ActionOption(String label, int cost, String detail, boolean canUse, Supplier<List<CombatEvent>> action) {}
	
	private final Battle battle;
	private final BattleMap map;
	private final String location;
	
	private List<ActionOption> options = new ArrayList<>();
	private List<CombatEvent> events = new ArrayList<>(); //TODO implement connection with start() and endPlayerTurn()
	private int focusedOption = -1;
	private int hoveredOption = -1;
	private Position moveTarget = null;
	private Combatant actionTarget = null;

	public CombatScreen(UIHandler ui, Battle battle, String location) {
		super(ui);
		this.battle = battle;
		this.map = battle.getMap();
		this.location = location;
	}
	
	public ScreenAction run() {
		battle.start();
		while(true) {
			drawFrame();
			UIHandler.Input in = ui.readInput();
			CombatAction cAction = null;
			switch (in.inputKey()) {
				case ESC -> {return ScreenAction.BACK;}
				case UP, DOWN -> focusedOption = focusedOption == -1? 0 : moveFocus(in, focusedOption, options.size());
				case HOVER -> hoveredOption = moveFocus(in, hoveredOption, options.size(),false);
				case ENTER -> {
					if(focusedOption != -1) {
						
					}
				}
				case CLICK -> {
					if (in.button() instanceof Button b) {
						if(b.action() instanceof ScreenAction a) {return a;}
						if(b.action() instanceof CombatAction a) {cAction = a;}
						else if(b.index() >= 0) {
							focusedOption = b.index();
						}
					}
				}
				default -> {} //TODO expand input handling
				
			}
			if(cAction != null) {
				performCombatAction(cAction);
			}
			if(battle.getOutcome() != null) { //TODO temporary stub exit
				return ScreenAction.BACK;
			}
		}
	}
	
	private void performCombatAction(CombatAction action) {
		switch(action) {
			case ACT -> {
				ActionOption option = options.get(focusedOption);
				if(option.canUse) {
					events.addAll(option.action().get());
				}
			}
			case END -> battle.endPlayerTurn();
		}
		focusedOption = -1;
	}
	
	private void generateActionOptions(Player playerChar) {
		//decide available actions
		// attack
		options.clear();
		boolean canMain = false, canOff = false;
		boolean hasOff = playerChar.getOffHandAttackPower() != 0;

		String mname = playerChar.getEquipped(Slot.MAIN_HAND) instanceof Equipment e? e.getName() : "Unarmed";
		String mdetail = "", odetail = "", filler = " · Damage: ";
		if(actionTarget != null) {
			Battle.AttackPreview mp = battle.previewAttack(actionTarget,AttackType.MAIN);
			canMain = mp.legal();
			mdetail = canMain? mname + filler + mp.damage() : mp.reason();
			if(hasOff) {
				Battle.AttackPreview op = battle.previewAttack(actionTarget,AttackType.OFF_HAND);
				String oname = playerChar.getEquipped(Slot.OFF_HAND).getName();
				canOff = op.legal();
				odetail  = canOff? oname + filler + op.damage() : op.reason();
			}
		}
		else {
			mdetail = "No Target";
			odetail = "No Target";
		}

		options.add(new ActionOption("Attack",Combatant.MAIN_ATTACK_COST, mdetail,canMain,
				() -> battle.attack(actionTarget, AttackType.MAIN)));

		if(hasOff) {
			options.add(new ActionOption("Off Hand Attack",Combatant.OFF_HAND_ATTACK_COST, odetail,canOff,
					() -> battle.attack(actionTarget, AttackType.OFF_HAND)));
		}
			
		//defense
		Battle.DefensePreview dp;
		String label = "Guard";
		String detail;
		Supplier<List<CombatEvent>> guardAction;
		if(playerChar.getShieldGuard() != 0) {
			dp = battle.previewDefend(DefenseType.SHIELD);
			guardAction = () -> battle.defend(DefenseType.SHIELD);
			label = "Shield Guard";
		}
		else {
			dp = battle.previewDefend(DefenseType.SIMPLE);
			guardAction = () -> battle.defend(DefenseType.SIMPLE);
		}
		if(dp.legal()) {
			detail = "Armor +%d until your next turn".formatted(dp.guard());
		}
		else {
			detail = dp.reason();
		}
		options.add(new ActionOption(label, Combatant.DEFEND_COST, 
				detail ,dp.legal(), guardAction));
			
		//dodge
		if(playerChar.canDodge()) {
			Battle.DefensePreview dgp = battle.previewDefend(DefenseType.DODGE);
			if(dgp.legal()) {
				var chances = dgp.dodgeChances().values().stream().sorted().toList();
				int[] minMax = {100 - chances.getLast(), 100 - chances.getFirst()};
				options.add(new ActionOption("Dodge", Combatant.DEFEND_COST, 
						"Gain %d - %d%% chance to dodge attacks until next round"
						.formatted(minMax[0], minMax[1]),true, 
						() -> battle.defend(DefenseType.DODGE)));
			}
		}
		
		//move
		//TODO implement move action
		
		//evacuation
		Battle.EvacuatePreview ep = battle.previewEvacuate();
		options.add(new ActionOption("Escape", Combatant.EVACUATE_COST, 
				ep.legal()? "Escape Battle" : ep.reason() ,ep.legal(), () -> battle.evacuate()));
	}

	private void drawFrame() {
		beginFrame();
		
		
		//identify scene objects and units
		List<SceneObject> objects = new ArrayList<>();
		List<Combatant> units = battle.getParticipantsAlive();

		for(int i = 0; i < map.height(); i++) {
			for(int j = 0; j < map.width(); j++) {//TODO collect more terrains when implemented
				if(map.terrainAt(new Position(j, i)) == Terrain.TREE) {
					objects.add(SceneObject.ofTile(new Position(j, i), TREE_IMG));
				}
			}
		}
		for(Combatant c : units) {
			String image = c.getCharacter() instanceof Enemy e? e.getId() : PLAYER_IMG;
			objects.add(SceneObject.ofUnit(c.getPosition(), image, c));
		}
		Combatant player = units.stream().filter(c -> c.getCharacter() instanceof Player).findFirst().orElseThrow(
				() -> new IllegalStateException("No alive player combatant unit"));
		
		generateActionOptions((Player) player.getCharacter());
		
		//draw top-left side panel: player info
		{
			int x = LEFT_X + PAD + IMG_W;
			int y = SIDE_Y + 2;
			int iconX = LEFT_X + PAD;
			int iconY = SIDE_Y + 4;

			canvas.fill(LEFT_X, SIDE_Y, SIDE_W, LEFT_TOP_H, Palette.PANEL);
			drawCharInfo(x, y, iconX, iconY, INFO_TEXT_W, player, PLAYER_IMG, PLAYER_COLOR);
		}
		
		//draw bottom-left side panel: action panel
		{
						
			//draw action panel
			{
				int x = LEFT_X + PAD;
				int y = SIDE_Y + LEFT_TOP_H + LEFT_PANEL_MARGIN;
				canvas.fill(LEFT_X, y, SIDE_W, LEFT_BOT_H, Palette.PANEL);
				canvas.drawText(x, ++y, "ACTIONS", Palette.FAINT);

				y += 2;
				int labelX = x + 4;
				for(int i=0; i < options.size(); i++) {
					ActionOption o = options.get(i);

					int labelColor = Palette.MUTED, detailColor = Palette.FAINT, pipColor = Palette.FAINT;
					if(o.canUse()) {
						labelColor = Palette.TAG;
						detailColor = Palette.MUTED;
						pipColor = Palette.KEY;
					}
					if(hoveredOption == i || focusedOption == i) {
						int indicatorColor = Palette.MUTED;
						if(focusedOption == i) {
							labelColor = Palette.KEY;
							indicatorColor = Palette.KEY;
							canvas.fill(LEFT_X, y, SIDE_W, 4, Palette.PANEL_LIGHT);
						}
						for(int j=0; j<4; j++) {
							canvas.drawText(LEFT_X, y+j, FOCUS_INDICATOR, indicatorColor);
						}
					}
					canvas.drawText(x, y + 1, String.valueOf(i+1), labelColor);
					canvas.drawText(labelX, y + 1, o.label(), labelColor);
					drawPips(LEFT_X + SIDE_W - PAD, y + 1, o.cost, o.cost(), pipColor);
					canvas.drawText(labelX, y + 2, o.detail(), detailColor);

					ui.addButtonBox(LEFT_X, y, SIDE_W, 4, new Button(i));

					y += 4;
				}
				int buttonY = SIDE_Y + LEFT_TOP_H + LEFT_BOT_H + LEFT_PANEL_MARGIN - Canvas.BUTTON_HEIGHT;
				ui.drawButton(LEFT_X + 1, buttonY, "END TURN",
						true, new Button(CombatAction.END));
				boolean canConfirm = false;
				Button confirmButton = null;
				if(focusedOption != -1 && options.get(focusedOption).canUse()) {
					canConfirm = true;
					confirmButton = new Button(CombatAction.ACT);
				}
				ui.drawButton(LEFT_X + SIDE_W - Canvas.BUTTON_WIDTH - 1, buttonY, "Confirm", canConfirm, confirmButton);
			}
		}
		
		//draw top-right side info panel
		{
			canvas.fill(RIGHT_X, SIDE_Y, SIDE_W, RIGHT_TOP_H, Palette.PANEL);
			if(units.size() >= 2) {
				Combatant enemy = units.stream().filter(c -> c.getSide() == Side.ENEMY).findFirst().get();
				Enemy enemyChar = (Enemy) enemy.getCharacter();
				int x = RIGHT_X + PAD;
				int y = SIDE_Y + 2;
				int iconX = RIGHT_X + SIDE_W - IMG_W - PAD;
				int iconY = SIDE_Y + 4;
				
				drawCharInfo(x, y, iconX, iconY, INFO_TEXT_W, enemy, enemyChar.getId(), ENEMY_COLOR);
			}
		}
		
		//draw bottom-right log 
		{
			int y = SIDE_Y + RIGHT_TOP_H + RIGHT_PANEL_MARGIN;
			int x= RIGHT_X + PAD;
			canvas.fill(RIGHT_X, y, SIDE_W, RIGHT_BOT_H, Palette.PANEL);
			canvas.drawText(x, ++y, "LOG", Palette.FAINT);
		}
		
		//draw map
		{
			//tile
			{
				PixelImage.ground(TILE_IMG).ifPresentOrElse(
						img -> canvas.drawImage(MAP_X, MAP_Y, img) ,
						() -> canvas.fill(MAP_X, MAP_Y / 2, map.width() * CELL, map.height() * CELL / 2, Palette.PANEL));

				//draw grid shadings
				for(int y = 0; y < map.height() * CELL; y++) {
					for(int x = 0; x < map.width() * CELL; x++) {
						if(x % CELL == 0 || y % CELL == 0) {
							canvas.shadePixel(MAP_X + x, MAP_Y + y, 0.08);
						}
					}
				}
			}

			//draw shadows for all objects, then images, then health bar for units
			{
				for(SceneObject o : objects) {
					canvas.drawContactShadow(imgX(o), footY(o), o.image(), o.isUnit()? 0.2 : 0.15);
				}
				//sort using y then x cords, the lower the later for z-layering
				objects.sort(Comparator.comparingInt((SceneObject o) -> o.cell().y())
						.thenComparing(o -> o.cell().x()));
				for(SceneObject o : objects) {
					canvas.drawImage(imgX(o), footY(o) - o.image().getHeight() + 1, o.image());
				}

				for(SceneObject o : objects) {
					if(o.isUnit()) {
						int color = o.unit().getSide() == Side.PLAYER? PLAYER_COLOR : ENEMY_COLOR;
						int fill = (int) Math.round((double) o.unit().getCharacter().getHp() * HP_BAR_X_SPAN / o.unit().getCharacter().getMaxHp());
						for(int i=0; i < HP_BAR_X_SPAN; i++) {
							int left = MAP_X + o.cell().x() * CELL;
							canvas.drawPixel( left + HP_BAR_X_OFFSET + i, footY(o) + HP_BAR_Y_OFFSET, i < fill? color : Palette.LINE);
						}
					}
				}
			}
		}
		
		//draw title bar last to overlay on top of the map images
		String roundLabel = "/  ROUND %02d".formatted(battle.getRound());
		canvas.dim(MAP_X, TITLE_Y -1 , location.length() + roundLabel.length() + 1, 3);

		canvas.drawText(MAP_X, TITLE_Y, location, Palette.TAG);
		canvas.drawText(MAP_X + location.length() + 2, TITLE_Y, roundLabel, Palette.MUTED);
		
//		//draw tool bar //TODO implement correct tool bar logic
//		{
//			
//			String[] labels = {"Main Menu", "Inventory", "Character"};
//			ScreenAction[] actions = {ScreenAction.BACK, ScreenAction.INVENTORY, ScreenAction.CHARACTER};
//			String divider = "  |  ";
//			int totalW = Arrays.stream(labels).mapToInt(String::length).sum() + divider.length() * (labels.length - 1); 
//			canvas.dim(MAP_X + MAP_W - totalW, TITLE_Y -1 , Arrays.stream(labels).mapToInt(String::length).sum(), 3);
//			drawToolBar(MAP_X + MAP_W, TITLE_Y, labels, actions, divider); 
//		}
		
		//draw initiative bar
		{
			//one card per living unit
			int[] widths = units.stream()
					.mapToInt(unit -> {
						GameCharacter ch = unit.getCharacter();
						return "%s%d / %d".formatted(ch.getName(), ch.getHp(), ch.getMaxHp()).length() + CARD_W_BUFFER;
					})
					.toArray();
			int totalW = Arrays.stream(widths).sum() + CARD_MARGIN * (units.size() - 1);
			int x0 = MAP_X + (MAP_W - totalW) / 2;

			
			canvas.fill(x0, CARD_Y, totalW, 2, Palette.PANEL);
			
			int x = x0;
			for(int i = 0; i < units.size(); i++) {
				Combatant unit = units.get(i);
				GameCharacter ch = unit.getCharacter();
				
				int w = widths[i];
				int sideColor = unit.getSide() == Side.PLAYER? PLAYER_COLOR : ENEMY_COLOR;
				boolean isCurrent = battle.getCurrentActor().equals(unit);
				
				if(isCurrent) {
					canvas.fill(x, CARD_Y, w, 2, Palette.PANEL_LIGHT);
					canvas.drawText(x, CARD_Y, FOCUS_INDICATOR, Palette.KEY);
					canvas.drawText(x, CARD_Y + 1, FOCUS_INDICATOR, Palette.KEY);
				}
				canvas.drawText(x + 2, CARD_Y, "▸", isCurrent? Palette.KEY : Palette.MUTED);
				canvas.drawText(x + 4, CARD_Y, ch.getName(), isCurrent? Palette.KEY : sideColor);
				drawRight(x + w - 2, CARD_Y, "%d / %d".formatted(ch.getHp(), ch.getMaxHp()), isCurrent? Palette.TEXT : Palette.FAINT);
				drawBar(x + 4, CARD_Y + 1, w - 6, ch.getHp(), ch.getMaxHp(), sideColor);
				
				x += w + CARD_MARGIN;
			}
		}
		
		endFrame(List.of());
	}	
	/** returns the pixel-x position of the left origin of the object */
	private int imgX(SceneObject o) {
		return MAP_X + o.cell().x() * CELL + (CELL - o.image().getWidth()) / 2;
	}
	
	/** returns the pixel-y position of the foot of a object/unit */
	private int footY(SceneObject o) {
		return MAP_Y + o.cell.y() * CELL + CELL + FOOT_Y_OFFSET;
	}
	
	/** text that ends at column endX (exclusive), for numbers lined up on the right */
	private void drawRight(int endX, int y, String text, int color) {
		canvas.drawText(endX - text.length(), y, text, color);
	}

	/** a bar of w cells: the filled part in color, the rest in Palette.LINE */
	private void drawBar(int x, int y, int w, int value, int max, int color) {
		int fill = (int) Math.round((double) value * w / max);
		for(int i=0; i<w; i++) {
			canvas.drawText(x + i, y, "━", i < fill? color : Palette.LINE);
		}
	}

	/** AP as dots: have filled in Palette.KEY, the rest hollow in Palette.MUTED, two columns apart */
	private void drawPips(int endX, int y, int have, int total, int color) {
		for(int i=0; i<total; i++) {
			canvas.drawText(endX - 2 * total + 1 + 2 * i , y, i < have? "●" : "○", i < have? color : Palette.FAINT);
		}
	}
	
	/** draws the character information with basic combat stats at given x,y */
	private void drawCharInfo(int x, int y, int iconX, int iconY, int textW, Combatant c, String img, int color) {
		GameCharacter ch = c.getCharacter();
		boolean isCurrent = battle.getCurrentActor().equals(c);
		
		PixelImage.largeFigure(img).ifPresent(i -> canvas.drawImage(iconX, iconY, i));
		canvas.drawText(x, y, ch.getName(), color);
		canvas.drawText(x, ++y, "Level %d".formatted(ch.getLevel()), Palette.MUTED);
		
		canvas.drawText(x, y+=2, "HP", Palette.MUTED);
		drawRight(x + textW, y, "%d / %d".formatted(ch.getHp(), ch.getMaxHp()), Palette.TEXT);
		drawBar(x, ++y, textW, ch.getHp(), ch.getMaxHp(), color);
		
		if(isCurrent) {
			canvas.drawText(x, y+=2, "AP", Palette.MUTED);
			drawPips(x + textW, y, c.getAp(), Combatant.MAX_AP, Palette.KEY);
		}
		
		String defenseLabel = String.valueOf(ch.getArmor()) + (c.getGuardValue() == 0? "" : " + " + c.getGuardValue());
		canvas.drawText(x, ++y, "Armor", Palette.MUTED);
		drawRight(x + textW, y, defenseLabel , Palette.TEXT);
		
		canvas.drawText(x, ++y, "Speed",Palette.MUTED);
		drawRight(x + textW, y, String.valueOf(ch.getSpeed()), Palette.TEXT);		
		if(c.getStepsLeft() != 0 && isCurrent) {
			canvas.drawText(x, ++y, "Move",Palette.MUTED);
			drawRight(x + textW, y, c.getStepsLeft() + " steps", Palette.FAINT);
		}
	}
}
