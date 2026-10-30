package com.endlessadventure.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.endlessadventure.combat.Battle;
import com.endlessadventure.combat.Battle.CombatEvent;
import com.endlessadventure.combat.Battle.MovePreview;
import com.endlessadventure.combat.BattleMap;
import com.endlessadventure.combat.BattleMap.Terrain;
import com.endlessadventure.combat.Combatant;
import com.endlessadventure.combat.Combatant.AttackType;
import com.endlessadventure.combat.Combatant.DefenseType;
import com.endlessadventure.combat.Position;
import com.endlessadventure.entity.Attribute;
import com.endlessadventure.entity.Enemy;
import com.endlessadventure.entity.GameCharacter;
import com.endlessadventure.entity.Player;
import com.endlessadventure.item.Equipment;
import com.endlessadventure.item.Equipment.Slot;

public class CombatScreen extends Screen {
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
							 HP_BAR_Y_OFFSET = 5,
							 HP_BAR_X_OFFSET = 3, //x offset from cell x
							 HP_BAR_X_SPAN = 7,
							 CELL_HIGHLIGHT_STROKE_LENGTH = 3;
	
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
	
	//combat option constants
	private static final int MOVE_OPTION = 0;
	
	//bitmap
	private static final String[] MOVE_PREVIEW_CIRCLE = {
			"···#####···",
			"·##·····##·",
			"#·········#",
			"·##·····##·",
			"···#####···"};
	
	private final Battle battle;
	private final BattleMap map;
	private final String location;
	
	private List<ActionOption> options = new ArrayList<>();
	private List<CombatEvent> events = new ArrayList<>(); //TODO implement connection with start() and endPlayerTurn()
	private int focusedOption = -1;
	private int hoveredOption = -1;
	
	private Position cursor = null;
	private Combatant focusedTarget = null;
	private Mode mode = Mode.IDLE;
	
	public enum ScreenAction { BACK, INVENTORY, CHARACTER }
	public enum CombatAction { ACT, END }
	private enum MapArea { CELL }
	private enum Mode { IDLE, TARGETING }
	private enum ObjectKind { PROP, UNIT, GHOST, BLOCKED_GHOST }
	
	private record SceneObject(Position cell, PixelImage image, ObjectKind kind, Combatant unit) {
		boolean isUnit() {
			return kind == ObjectKind.UNIT;
		}
		
		static SceneObject ofProp(Position cell, String image) {
			PixelImage i = PixelImage.figure(image).get(); //TODO change when decided to let game run without sprites
			return new SceneObject(cell, i, ObjectKind.PROP, null);
		}
		
		static SceneObject ofUnit(Position cell, String image, Combatant unit) {
			PixelImage i = PixelImage.figure(image).get(); //TODO change when decided to let game run without sprites
			return new SceneObject(cell, i, ObjectKind.UNIT, unit);
		}
		
		static SceneObject ofGhost(Position cell, String image, boolean blocked) {
			PixelImage i = PixelImage.figure(image).get(); //TODO change when decided to let game run without sprites
			return new SceneObject(cell, i, blocked? ObjectKind.BLOCKED_GHOST : ObjectKind.GHOST, null);
		}
	}
	
	private record ActionOption(String label, int cost, String detail, boolean ready, Predicate<Position> isValidTarget,
			Function<Position, List<CombatEvent>> action){}

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
				case ESC -> {
					if(focusedOption == -1) {
						return ScreenAction.BACK;
					}
					if(mode == Mode.TARGETING) {
						mode = Mode.IDLE;
					}
					focusedOption = -1;

				}
				case UP, DOWN -> focusedOption = focusedOption == -1? 0 : moveFocus(in, focusedOption, options.size());
				case HOVER -> {
					if(in.button() instanceof Button b && b.action() == MapArea.CELL) {
						cursor = new Position(b.index() % map.width(), b.index() / map.width());
						hoveredOption = -1;
					}
					else{
						cursor = null;
						hoveredOption = moveFocus(in, hoveredOption, options.size(),false);
					}
				}
				case ENTER -> {
					if(focusedOption != -1) {
						cAction = CombatAction.ACT;
					}
				}
				case CHAR -> {
					int digit = in.digit(options.size());
					if(digit >= 0) {
						selectOption(digit);
					}
				}
				case CLICK -> {
					if (in.button() instanceof Button b) {
						if(b.action() instanceof ScreenAction a) {return a;}
						if(b.action() instanceof CombatAction a) {cAction = a;}
						else if(b.action() == MapArea.CELL) {
							cursor = new Position(b.index() % map.width(), b.index() / map.width());
							switch(mode) {
								case IDLE -> focusedTarget = battle.getUnitAt(cursor);
								case TARGETING -> execute(options.get(focusedOption));
							}
						}
						else if(b.index() >= 0) {
							selectOption(b.index());
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
	
	/** a click or number key on row i: options without a target run on the second press, the others start or stop aiming */
	private void selectOption(int i) {
		ActionOption ao = options.get(i);
		if(i == focusedOption) {
			if(ao.isValidTarget() == null) {
				execute(ao);
			}
			else {
				focusedOption = -1;
				mode = Mode.IDLE;
			}
		}
		else {
			focusedOption = i;
			mode = ao.isValidTarget() == null? Mode.IDLE : Mode.TARGETING;
		}
	}
	
	/** whether running o now would succeed: its target is always the cursor; options without a target only need to be ready */
	private boolean canExecute(ActionOption o) {
		return o.isValidTarget() == null ? o.ready() : cursor != null && o.isValidTarget().test(cursor);
	}

	/** runs o on the cursor if canExecute, then returns to IDLE; does nothing otherwise */
	private void execute(ActionOption o) {
		if(canExecute(o)) {
			events.addAll(o.action().apply(o.isValidTarget() == null? null : cursor));
			mode = Mode.IDLE;
			focusedOption = -1;
		}
	}

	private void performCombatAction(CombatAction action) {
		switch(action) {
			case ACT -> {
				if(focusedOption > -1) {
					ActionOption ao = options.get(focusedOption);
					if(canExecute(ao)) {
						execute(ao);
					}
					else {
						ui.showWarning(ao.isValidTarget() == null? ao.detail() : "Point at a target on the map");
					}
				}
			}
			case END -> {
				battle.endPlayerTurn();
				mode = Mode.IDLE;
				focusedOption = -1;
			}
		}
	}

	private void generateActionOptions(Combatant player, List<Combatant> units) {
		Player playerChar = (Player) player.getCharacter();
		options.clear();
		//decide available actions
		// move
		String moveDetail = "Point at a cell";
		int moveCost = Combatant.MOVE_COST;
		if(cursor != null && battle.getUnitAt(cursor) == null) {
			MovePreview mv = battle.previewMove(cursor);
			moveDetail = mv.legal()? "Move %d steps".formatted(mv.path().size()) : mv.reason();
			moveCost = mv.legal()? mv.apCost() : Combatant.MOVE_COST;
		}

		options.add(new ActionOption("Move", moveCost, moveDetail,
				player.canMove(),
				p -> battle.getUnitAt(p) == null && battle.previewMove(p).legal(),
				battle::move));

		// attack: the row shows the damage on the enemy under the cursor, otherwise the weapon's attack power
		Combatant aimed = cursor != null && battle.getUnitAt(cursor) instanceof Combatant c && c.isEnemy()? c : null;
		String filler = " · Damage: ";

		Predicate<Position> mainTarget = p -> battle.getUnitAt(p) instanceof Combatant c && c.isEnemy() &&
				battle.previewAttack(c, AttackType.MAIN).legal();
		String mname = playerChar.getEquipped(Slot.MAIN_HAND) instanceof Equipment e? e.getName() : "Unarmed";
		String mainDetail = mname + " · Attack " + playerChar.getMainAttackPower();
		if(aimed != null) {
			Battle.AttackPreview mp = battle.previewAttack(aimed, AttackType.MAIN);
			mainDetail = mp.legal()? mname + filler + mp.damage() : mp.reason();
		}
		options.add(new ActionOption("Attack",Combatant.MAIN_ATTACK_COST, mainDetail,
				units.stream().anyMatch(u -> mainTarget.test(u.getPosition())),
				mainTarget,
				p -> battle.attack(battle.getUnitAt(p), AttackType.MAIN)));

		if(playerChar.getOffHandAttackPower() != 0) {
			Predicate<Position> offTarget = p -> battle.getUnitAt(p) instanceof Combatant c && c.isEnemy() &&
					battle.previewAttack(c, AttackType.OFF_HAND).legal();
			String oname = playerChar.getEquipped(Slot.OFF_HAND).getName();
			String offDetail = oname + " · Attack " + playerChar.getOffHandAttackPower();
			if(aimed != null) {
				Battle.AttackPreview op = battle.previewAttack(aimed, AttackType.OFF_HAND);
				offDetail = op.legal()? oname + filler + op.damage() : op.reason();
			}
			options.add(new ActionOption("Off Hand Attack",Combatant.OFF_HAND_ATTACK_COST, offDetail,
					units.stream().anyMatch(u -> offTarget.test(u.getPosition())),
					offTarget,
					p -> battle.attack(battle.getUnitAt(p), AttackType.OFF_HAND)));
		}
			
		//defense
		Battle.DefensePreview dp;
		String label = "Guard";
		String defDetail;
		DefenseType defType;
		if(playerChar.getShieldGuard() != 0) {
			dp = battle.previewDefend(DefenseType.SHIELD);
			defType = DefenseType.SHIELD;
			label = "Shield Guard";
		}
		else {
			dp = battle.previewDefend(DefenseType.SIMPLE);
			defType = DefenseType.SIMPLE;
		}
		if(dp.legal()) {
			defDetail = "Armor +%d for 1 turn".formatted(dp.guard());
		}
		else {
			defDetail = dp.reason();
		}
		options.add(new ActionOption(label, Combatant.DEFEND_COST, 
				defDetail ,dp.legal(), null, p -> battle.defend(defType)));
			
		//dodge
		if(playerChar.canDodge()) {
			Battle.DefensePreview dgp = battle.previewDefend(DefenseType.DODGE);
			if(dgp.legal()) {
				var chances = dgp.dodgeChances().values().stream().sorted().toList();
				int[] minMax = {100 - chances.getLast(), 100 - chances.getFirst()};
				options.add(new ActionOption("Dodge", Combatant.DEFEND_COST, 
						"Gain %s chance to dodge attacks for 1 turn".formatted( minMax[0] == minMax[1]? 
								minMax[0] + "%" : "%d - %d%%".formatted(minMax[0], minMax[1])),
						true,
						null,
						p -> battle.defend(DefenseType.DODGE)));
			}
		}
		
		//evacuation
		Battle.EvacuatePreview ep = battle.previewEvacuate();
		options.add(new ActionOption("Escape", Combatant.EVACUATE_COST, 
				ep.legal()? "Escape battle" : ep.reason() ,ep.legal(),
						null,p -> battle.evacuate()));
	}
	
	

	private void drawFrame() {
		beginFrame();
		
		//identify scene objects and units
		List<SceneObject> objects = new ArrayList<>();
		List<Combatant> units = battle.getParticipantsAlive();

		for(int i = 0; i < map.height(); i++) {
			for(int j = 0; j < map.width(); j++) {//TODO collect more terrains when implemented
				if(map.terrainAt(new Position(j, i)) == Terrain.TREE) {
					objects.add(SceneObject.ofProp(new Position(j, i), TREE_IMG));
				}
			}
		}
		for(Combatant c : units) {
			String image = c.getCharacter() instanceof Enemy e? e.getId() : PLAYER_IMG;
			objects.add(SceneObject.ofUnit(c.getPosition(), image, c));
		}
		Combatant player = units.stream().filter(c -> c.getCharacter() instanceof Player).findFirst().orElseThrow(
				() -> new IllegalStateException("No alive player combatant unit"));
		
		if(focusedTarget != null && !focusedTarget.getCharacter().isAlive()) {
			focusedTarget = null;
		}
		
		//generate action options for action panel
		generateActionOptions(player, units);
		
		
		
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
		drawActionPanel();
		
		//draw top-right side info panel
		{
			canvas.fill(RIGHT_X, SIDE_Y, SIDE_W, RIGHT_TOP_H, Palette.PANEL);
			Combatant unit = cursor != null && battle.getUnitAt(cursor) instanceof Combatant c? c : focusedTarget != null? focusedTarget : null;
			String img = PLAYER_IMG;
			int color = PLAYER_COLOR;

			if(unit != null) {
				if(unit.getCharacter() instanceof Enemy e) {
					img = e.getId();
					color = ENEMY_COLOR;
				}
				
				int x = RIGHT_X + PAD;
				int y = SIDE_Y + 2;
				int iconX = RIGHT_X + SIDE_W - IMG_W - PAD;
				int iconY = SIDE_Y + 4;
				drawCharInfo(x, y, iconX, iconY, INFO_TEXT_W, unit, img, color);
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
		drawMap(objects, player);
		
		//draw title bar last to overlay on top of the map images
		String roundLabel = "/  ROUND %02d".formatted(battle.getRound());
		canvas.dim(MAP_X, TITLE_Y -1 , location.length() + roundLabel.length() + 1, 3);

		canvas.drawText(MAP_X, TITLE_Y, location, Palette.TAG);
		canvas.drawText(MAP_X + location.length() + 2, TITLE_Y, roundLabel, Palette.MUTED);
		
//		//draw tool bar 
		//TODO implement correct tool bar logic
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
				int sideColor = unit.isEnemy()? ENEMY_COLOR : PLAYER_COLOR;
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

	private void drawActionPanel() {
		int x = LEFT_X + PAD;
		int y = SIDE_Y + LEFT_TOP_H + LEFT_PANEL_MARGIN;
		canvas.fill(LEFT_X, y, SIDE_W, LEFT_BOT_H, Palette.PANEL);
		canvas.drawText(x, ++y, "ACTIONS", Palette.FAINT);

		y += 2;
		int labelX = x + 4;
		for(int i=0; i < options.size(); i++) {
			ActionOption o = options.get(i);

			int labelColor = Palette.MUTED; 
			int detailColor = Palette.FAINT;
			int pipColor = o.ready()? Palette.KEY : Palette.FAINT;
			if(hoveredOption == i || focusedOption == i) {
				int indicatorColor = Palette.MUTED;
				if(focusedOption == i) {
					labelColor = o.ready()? Palette.KEY : Palette.MUTED;
					indicatorColor = Palette.KEY;
					canvas.fill(LEFT_X, y, SIDE_W, 4, Palette.PANEL_LIGHT);
				}
				for(int j=0; j<4; j++) {
					canvas.drawText(LEFT_X, y+j, FOCUS_INDICATOR, indicatorColor);
				}
			}
			if(o.ready()) {
				detailColor = Palette.MUTED;
				labelColor = Palette.TAG;
			}
			canvas.drawText(x, y + 1, String.valueOf(i+1), labelColor);
			canvas.drawText(labelX, y + 1, o.label(), labelColor);
			drawPips(LEFT_X + SIDE_W - PAD, y + 1, o.cost, o.cost(), pipColor);
			canvas.drawParagraph(labelX, y + 2, LEFT_X + SIDE_W - PAD - labelX, o.detail(), detailColor);

			ui.addButtonBox(LEFT_X, y, SIDE_W, 4, new Button(i));

			y += 4;
		}
		int buttonY = SIDE_Y + LEFT_TOP_H + LEFT_BOT_H + LEFT_PANEL_MARGIN - Canvas.BUTTON_HEIGHT;
		ui.drawButton(LEFT_X + 1, buttonY, "END TURN", true, new Button(CombatAction.END));
		//a dim Confirm still takes the click, so performCombatAction can say why it cannot run
		ui.drawButton(LEFT_X + SIDE_W - Canvas.BUTTON_WIDTH - 1, buttonY, "Confirm",
				focusedOption != -1 && canExecute(options.get(focusedOption)), new Button(CombatAction.ACT));
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
		
		y++;
		if(isCurrent) {
			canvas.drawText(x, ++y, "AP", Palette.MUTED);
			drawPips(x + textW, y, c.getAp(), Combatant.MAX_AP, Palette.KEY);
		}
		
		String defenseLabel = String.valueOf(ch.getArmor()) + (c.getGuardValue() == 0? "" : " + " + c.getGuardValue());
		canvas.drawText(x, ++y, "Armor", Palette.MUTED);
		drawRight(x + textW, y, defenseLabel , Palette.TEXT);
		
		canvas.drawText(x, ++y, "Speed",Palette.MUTED);
		drawRight(x + textW, y, String.valueOf(ch.getSpeed()), Palette.TEXT);	
		if(c.getStepsLeft() != 0 && isCurrent) {
			canvas.drawText(x, ++y, "Move",Palette.MUTED);
			drawRight(x + textW, y, c.getStepsLeft() + " steps", Palette.MUTED);
		}
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
	
	

	//map drawing
	/** draws the center battle map area and interactable elements within */
	private void drawMap(List<SceneObject> objects, Combatant player) {
		//floor tile
		PixelImage.ground(TILE_IMG).ifPresentOrElse(
				img -> canvas.drawImage(MAP_X, MAP_Y, img) ,
				() -> canvas.fill(MAP_X, MAP_Y / 2, map.width() * CELL, map.height() * CELL / 2, Palette.PANEL));
		
		//register clickable map elements and draw map indications 
		for (int y = 0; y < map.height(); y++) {
			for (int x = 0; x < map.width(); x++) {
				ui.addButtonBox(MAP_X + x * CELL, MAP_Y / 2 + y * CELL / 2, CELL, CELL / 2,
						new Button(y * map.width() + x, MapArea.CELL));
				
				Position current = new Position(x, y);

				//draw move range indicator
				if(mode == Mode.TARGETING && focusedOption == MOVE_OPTION) {
					if(!map.terrainAt(current).blocks && battle.getUnitAt(current) == null && 
							battle.previewMove(current) instanceof MovePreview mv && mv.legal()) {
						drawCellMovePreview(x, y, 1, Palette.TAG,
								player.getAp() - mv.apCost() >= Combatant.MAIN_ATTACK_COST? 0.24 : 0.12);
					}
					else {
						drawCellMovePreview(x, y, 0, Palette.BG, 0.2);
					}
				}
				else if(mode == Mode.TARGETING) { //other targets: red under each valid one, everything else dimmed like out-of-reach cells
					if(options.get(focusedOption).isValidTarget().test(current)) {
						drawCellMovePreview(x, y, 1, Palette.DANGER, 0.24);
					}
					else {
						drawCellMovePreview(x, y, 0, Palette.BG, 0.2);
					}
				}
			}
		}
		
		//draw grid lines
		for(int y = 0; y < map.height() * CELL; y++) {
			for(int x = 0; x < map.width() * CELL; x++) {
				if(x % CELL == 0 || y % CELL == 0) {
					canvas.shadePixel(MAP_X + x, MAP_Y + y, 0.08);
				}
			}
		}
		
		//draw path lines
		if(canTargetMove()){
			 MovePreview mv = battle.previewMove(cursor);
			 if(mv.legal()) {
					List<Position> paths = new ArrayList<>(mv.path());
					paths.add(0, player.getPosition());
					for(int c = 1; c < paths.size(); c++) {
						int x0 = MAP_X + paths.get(c-1).x() * CELL + 6;
						int x1 = MAP_X + paths.get(c).x() * CELL + 6;
						int y0 = footY(paths.get(c-1).y());
						int y1 = footY(paths.get(c).y());
						for(int i=0; i<CELL + 1; i++) {
							int px = x0 + Integer.signum(x1 - x0) * i;
							int py = y0 + Integer.signum(y1 - y0) * i;
							canvas.drawPixel(px, py, Palette.mix(canvas.getPixel(px, py), Palette.TEXT, 0.6));
							canvas.drawPixel(px, py + 1, Palette.mix(canvas.getPixel(px, py + 1), Palette.TEXT, 0.6));
						}
						//draw circle for destination
						if(c == paths.size() - 1) {
							int color = Palette.mix(canvas.getPixel(x1, y1), Palette.TEXT, 0.6);
							canvas.drawBitmap(MOVE_PREVIEW_CIRCLE, x1 - 5, y1 - 2, color, color, 1);
						}
					}
			 }
			objects.add(SceneObject.ofGhost(cursor, PLAYER_IMG, !mv.legal()));
		}

		//reorder objects for rendering sequence using y then x cords, the lower the later for z-layering
		objects.sort(Comparator.comparingInt((SceneObject o) -> o.cell().y())
				.thenComparing(o -> o.cell().x()));
		
		//draw shadows
		for(SceneObject o : objects) {
			Position cell = o.cell();
			canvas.drawContactShadow(imgX(cell.x(), o.image()), footY(cell.y()), o.image(), o.isUnit()? 0.2 : 0.15);
		}
		
		//draw objects
		for(SceneObject o : objects) {
			Position cell = o.cell();
			int px = imgX(cell.x(), o.image());
			int py = footY(cell.y()) - o.image().getHeight() + 1;
			if(cursor != null && cell.x() == cursor.x() && cell.y() - cursor.y() == 1) {
				canvas.drawImage(px, py, o.image(), Palette.TEXT, 0.1, 0.25);
			}
			else if(o.kind() == ObjectKind.GHOST) {
				canvas.drawImage(px, py, o.image(), Palette.TEXT, 0.2, 0.8);
			}
			else if(o.kind() == ObjectKind.BLOCKED_GHOST){
				canvas.drawImage(px, py, o.image(), Palette.DANGER, 0.7, 0.6);
			}
			else{
				canvas.drawImage(px, py, o.image());
			}
		}
		
		//draw health bars
		for(SceneObject o : objects) {
			if(o.isUnit()) {
				int color = o.unit().isEnemy()? ENEMY_COLOR : PLAYER_COLOR;
				int fill = (int) Math.round((double) o.unit().getCharacter().getHp() * HP_BAR_X_SPAN / o.unit().getCharacter().getMaxHp());
				for(int i=0; i < HP_BAR_X_SPAN; i++) {
					int left = MAP_X + o.cell().x() * CELL;
					canvas.drawPixel( left + HP_BAR_X_OFFSET + i, footY(o.cell().y()) + HP_BAR_Y_OFFSET, i < fill? color : Palette.LINE);
				}
			}
		}
		
		//draw corners: the pinned unit, then the cursor over it
		if(focusedTarget != null) {
			drawFocusedCell(focusedTarget.getPosition(), focusedTarget.isEnemy()? Palette.DANGER : Palette.SUCCESS);
		}
		if(canTargetMove()) {
			MovePreview mv = battle.previewMove(cursor);
			drawFocusedCell(cursor, mv.legal()? Palette.KEY : Palette.LINE);
		}
		else if(cursor != null && battle.getUnitAt(cursor) instanceof Combatant c){
			drawFocusedCell(cursor, c.isEnemy()? Palette.DANGER : Palette.SUCCESS);
		}
		else if(cursor != null) { //an empty cell or a tree
			drawFocusedCell(cursor, Palette.KEY);
		}
	}

	/** draws an overlay indicator at selected cell with color {@code color} mixed with the original at ratio {@code ratio} */
	private void drawCellMovePreview(int cellX, int cellY, int inset, int color, double ratio) {
		for(int i = 0 + inset; i < CELL - inset + 1; i++) {
			for(int j = 0 + inset; j < CELL - inset + 1; j++) {
				int px = MAP_X + cellX * CELL + j;
				int py = MAP_Y + cellY * CELL + i;
				canvas.drawPixel(px, py, Palette.mix(canvas.getPixel(px, py), color, ratio));
			}
		}
	}

	/** draws selection corners for selected cell*/
	private void drawFocusedCell(Position cell, int color) {
		int len = CELL_HIGHLIGHT_STROKE_LENGTH;
		for(int x = 0; x < CELL + 1; x++) { //highlights the whole grid instead of just the cell area
			for(int y = 0; y < CELL + 1; y++) {
				if((x == 0 || x == CELL || y == 0 || y == CELL) &&
						((x < len || x > CELL - len) && (y < len || y > CELL - len))) {
					int px = MAP_X + cell.x() * CELL + x;
					int py = MAP_Y + cell.y() * CELL + y;
					canvas.drawPixel(px, py, Palette.mix(canvas.getPixel(px, py), color, 0.65));
				}
			}
		}
	}
	
	/** returns the pixel-x position of the left origin of the object */
	private int imgX(int cellX, PixelImage img) {
		return MAP_X + cellX * CELL + (CELL - img.getWidth()) / 2;
	}
	
	/** returns the pixel-y position of the foot of a object/unit */
	private int footY(int cellY) {
		return MAP_Y + cellY * CELL + CELL + FOOT_Y_OFFSET;
	}
	
	/** whether the map shows a move preview (path, ghost) at the cursor: aiming Move at a cell nobody stands on */
	private boolean canTargetMove() {
		return cursor != null && mode == Mode.TARGETING && focusedOption == MOVE_OPTION && battle.getUnitAt(cursor) == null;
	}
}
