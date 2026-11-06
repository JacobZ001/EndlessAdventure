package com.endlessadventure.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import com.endlessadventure.Displayable.Detail;
import com.endlessadventure.Displayable.Detail.Line;
import com.endlessadventure.Displayable.Detail.Stat;
import com.endlessadventure.Displayable.Span;
import com.endlessadventure.Displayable.Tone;
import com.endlessadventure.audio.Sound;
import com.endlessadventure.combat.Battle;
import com.endlessadventure.combat.Battle.CombatEvent;
import com.endlessadventure.combat.Battle.MovePreview;
import com.endlessadventure.combat.Battle.Outcome;
import com.endlessadventure.combat.BattleMap;
import com.endlessadventure.combat.BattleMap.Terrain;
import com.endlessadventure.combat.Combatant;
import com.endlessadventure.combat.Combatant.AttackType;
import com.endlessadventure.combat.Combatant.DefenseType;
import com.endlessadventure.combat.Position;
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
	
	//outcome card
	private static final int OUTCOME_W = 44,
							 OUTCOME_H = 16;

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
	private final Combatant self; //the player's unit
	private final List<Position> cells = new ArrayList<>(); //every cell of the map, row by row
	private final List<ActionOption> options; //built once: which rows exist depends only on equipment

	private List<CombatEvent> events = new ArrayList<>();
	private int focusedOption = -1;
	private int hoveredOption = -1;

	private Position cursor = null; //the cell under the mouse; every action is aimed at it
	private Combatant focusedTarget = null; //the unit pinned to the details panel
	private Mode mode = Mode.IDLE;

	public enum ScreenAction { INVENTORY, CHARACTER }
	public enum CombatAction { ACT, END }
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

		static SceneObject ofUnit(Combatant unit) {
			PixelImage i = PixelImage.figure(figureId(unit)).get(); //TODO change when decided to let game run without sprites
			return new SceneObject(unit.getPosition(), i, ObjectKind.UNIT, unit);
		}

		static SceneObject ofGhost(Position cell, String image, boolean blocked) {
			PixelImage i = PixelImage.figure(image).get(); //TODO change when decided to let game run without sprites
			return new SceneObject(cell, i, blocked? ObjectKind.BLOCKED_GHOST : ObjectKind.GHOST, null);
		}
	}

	/** what an option would do on a cell right now. reason: why it cannot run, null when it can;
	 *  detail: the row's second line; cost: the AP shown as pips; preview: the lines of the preview box */
	private record Check(String reason, String detail, int cost, List<Detail> preview) {
		boolean legal() {
			return reason == null;
		}

		static Check of(String detail, int cost, List<Detail> preview) {
			return new Check(null, detail, cost, preview);
		}

		/** it cannot run here: the reason doubles as the detail line, and shows in red in the preview */
		static Check blocked(String reason, int cost) {
			return new Check(reason, reason, cost, List.of(Line.of(reason, Tone.BAD)));
		}

		/** it needs a target that is not pointed at yet: a hint, not an error */
		static Check prompt(String hint, String detail, int cost) {
			return new Check(hint, detail, cost, List.of(Line.of(hint, Tone.NOTE)));
		}
	}

	/** one row of the action panel. check answers everything about running it on a cell (the cursor, or any cell when
	 *  the map shows the valid targets); options without a target ignore the cell. action runs it on that cell */
	private record ActionOption(String label, boolean needsTarget, Function<Position, Check> check,
			Function<Position, List<CombatEvent>> action){}

	public CombatScreen(UIHandler ui, Battle battle, String location) {
		super(ui);
		this.battle = battle;
		this.map = battle.getMap();
		this.location = location;
		self = battle.getParticipants().stream().filter(c -> !c.isEnemy()).findFirst().orElseThrow(
				() -> new IllegalStateException("No player combatant unit"));
		for(int y = 0; y < map.height(); y++) {
			for(int x = 0; x < map.width(); x++) {
				cells.add(new Position(x, y));
			}
		}
		options = buildOptions();
	}
	
	/** the rows of the action panel, in the order of their number keys; Move stays first (MOVE_OPTION) */
	private List<ActionOption> buildOptions() {
		Player playerChar = (Player) self.getCharacter();
		List<ActionOption> list = new ArrayList<>();

		//move
		list.add(new ActionOption("Move", true, p -> {
			if(p == null) {
				return Check.prompt("Point at a cell", "Point at a cell", Combatant.MOVE_COST);
			}
			if(battle.getUnitAt(p) != null) {
				return Check.blocked("Occupied", Combatant.MOVE_COST);
			}
			MovePreview mv = battle.previewMove(p);
			if(!mv.legal()) {
				return Check.blocked(mv.reason(), Combatant.MOVE_COST);
			}
			int steps = mv.path().size();
			int stepsLeft = self.getStepsLeft() + mv.apCost() / Combatant.MOVE_COST * playerChar.getSpeed() - steps; //as in Combatant.moveTo
			return Check.of("Move %d steps".formatted(steps), mv.apCost(), List.of(
					Stat.of("Steps", steps),
					Stat.of("Your AP", self.getAp() + " → " + (self.getAp() - mv.apCost())),
					Stat.of("Steps left", stepsLeft)));
		}, battle::move));

		//attacks
		list.add(new ActionOption("Attack", true, p -> buildAttackCheck(p, AttackType.MAIN),
				p -> battle.attack(battle.getUnitAt(p), AttackType.MAIN)));
		if(playerChar.getOffHandAttackPower() != 0) {
			list.add(new ActionOption("Off Hand Attack", true, p -> buildAttackCheck(p, AttackType.OFF_HAND),
					p -> battle.attack(battle.getUnitAt(p), AttackType.OFF_HAND)));
		}

		//defense
		DefenseType guardType = playerChar.getShieldGuard() != 0? DefenseType.SHIELD : DefenseType.SIMPLE;
		list.add(new ActionOption(guardType == DefenseType.SHIELD? "Shield Guard" : "Guard", false, p -> {
			Battle.DefensePreview dp = battle.previewDefend(guardType);
			if(!dp.legal()) {
				return Check.blocked(dp.reason(), Combatant.DEFEND_COST);
			}
			int armor = playerChar.getArmor();
			return Check.of("Armor +%d for 1 turn".formatted(dp.guard()), Combatant.DEFEND_COST, List.of(
					Stat.of("Armor", armor + " → " + (armor + dp.guard())),
					Line.of("Until your next turn", Tone.NOTE)));
		}, p -> battle.defend(guardType)));

		//dodge
		if(playerChar.canDodge()) {
			list.add(new ActionOption("Dodge", false, p -> {
				Battle.DefensePreview dp = battle.previewDefend(DefenseType.DODGE);
				if(!dp.legal()) {
					return Check.blocked(dp.reason(), Combatant.DEFEND_COST);
				}
				//dodgeChances holds each opponent's chance to hit; the chance to dodge it is the rest
				List<Detail> preview = new ArrayList<>();
				dp.dodgeChances().forEach((enemy, hit) ->
						preview.add(Stat.of(enemy.getCharacter().getName(), (100 - hit) + "% dodge")));
				preview.add(Line.of("Until your next turn", Tone.NOTE));
				List<Integer> dodges = dp.dodgeChances().values().stream().map(hit -> 100 - hit).sorted().toList();
				String range = dodges.getFirst().equals(dodges.getLast())? dodges.getFirst() + "%"
						: "%d - %d%%".formatted(dodges.getFirst(), dodges.getLast());
				return Check.of("Gain %s chance to dodge attacks for 1 turn".formatted(range), Combatant.DEFEND_COST, preview);
			}, p -> battle.defend(DefenseType.DODGE)));
		}

		//evacuation
		list.add(new ActionOption("Escape", false, p -> {
			Battle.EvacuatePreview ep = battle.previewEvacuate();
			if(!ep.legal()) {
				return Check.blocked(ep.reason(), Combatant.EVACUATE_COST);
			}
			return Check.of("Escape battle", Combatant.EVACUATE_COST,
					List.of(Line.of("Leave the fight. The story goes on from here.", Tone.NOTE)));
		}, p -> battle.evacuate()));

		return list;
	}

	/** a main or off-hand attack on whatever stands at p; without an enemy there the row shows the weapon's attack power */
	private Check buildAttackCheck(Position p, AttackType type) {
		Player playerChar = (Player) self.getCharacter();
		boolean main = type == AttackType.MAIN;
		int cost = main? Combatant.MAIN_ATTACK_COST : Combatant.OFF_HAND_ATTACK_COST;
		String weapon = playerChar.getEquipped(main? Slot.MAIN_HAND : Slot.OFF_HAND) instanceof Equipment e? e.getName() : "Unarmed";
		int power = main? playerChar.getMainAttackPower() : playerChar.getOffHandAttackPower();

		Combatant target = p == null? null : battle.getUnitAt(p);
		if(target == null || !target.isEnemy()) {
			return Check.prompt("Point at an enemy", weapon + " · Attack " + power, cost);
		}
		Battle.AttackPreview ap = battle.previewAttack(target, type);
		if(!ap.legal()) {
			return Check.blocked(ap.reason(), cost);
		}

		//armor as a sum: base, plus the target's guard unless broken, minus pierce
		GameCharacter t = target.getCharacter();
		String armor = String.valueOf(t.getArmor());
		if(target.getGuardValue() > 0) {
			armor += ap.guardIgnored() > 0? " (guard broken)" : " + " + target.getGuardValue() + " guard";
		}
		if(ap.penetration() > 0) {
			armor += " - " + ap.penetration() + " pierce";
		}
		armor += " = " + ap.effectiveArmor();

		return Check.of(weapon + " · Damage: " + ap.damage(), cost, List.of(
				Line.of(t.getName(), Tone.BAD),
				Stat.of("Raw", ap.raw() + (ap.crit()? " (critical)" : ""), ap.crit()? Tone.GOOD : Tone.PLAIN),
				Stat.of("Armor", armor),
				Stat.of("Damage", ap.damage(), Tone.KEYWORD),
				Stat.of("Hit", ap.hitChance() + "%", ap.hitChance() < 100? Tone.BAD : Tone.PLAIN),
				Stat.of("Target HP", t.getHp() + " → " + Math.max(0, t.getHp() - ap.damage())),
				Stat.of("Your AP", self.getAp() + " → " + (self.getAp() - cost))));
	}

	
	
	//main logic handling loop
	public Battle.Outcome run() {
		List<CombatEvent> startEvents = battle.start();
		playEventSounds(startEvents);
		events.addAll(startEvents);
		while(true) {
			drawFrame();
			UIHandler.Input in = ui.readInput();
			CombatAction cAction = null;
			switch (in.inputKey()) {
				case ESC -> {
					if(focusedOption != -1) {
						resetSelection();
						cursor = null;
					}
				}
				case UP, DOWN, LEFT, RIGHT -> {
					if(mode == Mode.TARGETING) {
						if(cursor == null) {
							cursor = getStartCell(options.get(focusedOption));
						}
						else {
							int dx = switch(in.inputKey()) {
								case LEFT -> -1;
								case RIGHT -> 1;
								default -> 0;
							};
							int dy = switch(in.inputKey()) {
								case UP -> -1;
								case DOWN -> 1;
								default -> 0;
							};
							cursor = new Position(Math.clamp(cursor.x() + dx, 0, map.width() -1), Math.clamp(cursor.y() + dy, 0, map.height() - 1));
						}
					}
					else {
						switch(in.inputKey()) {
							case UP, DOWN -> {
								hoveredOption = hoveredOption == -1? 0 : moveFocus(in, hoveredOption, options.size());
							}
							default -> {}
						}
					}
				}
				case HOVER -> {
					if(in.button() instanceof Button b && b.action() == Button.Area.MAP_CELL) {
						cursor = cells.get(b.index());
						hoveredOption = -1;
					}
					else{
						cursor = null;
						hoveredOption = moveFocus(in, hoveredOption, options.size(),false);
					}
				}
				case ENTER -> {
					if (mode == Mode.IDLE && hoveredOption != -1) {
						selectOption(hoveredOption);
					}
					else if (focusedOption != -1) {
						cAction = CombatAction.ACT;
					}
				}
				case CHAR -> {
					int digit = in.digit(options.size());
					if(digit >= 0) {
						selectOption(digit);
					}
					else {
						switch(Character.toUpperCase(in.ch())){
							case 'E' -> cAction = CombatAction.END;
						}
					}
				}
				case CLICK -> {
					if (in.button() instanceof Button b) {
						//if(b.action() instanceof ScreenAction a) {return a;} //TODO implement inventory and character screen read
						if(b.action() instanceof CombatAction a) {cAction = a;}
						else if(b.action() == Button.Area.MAP_CELL) {
							cursor = cells.get(b.index());
							switch(mode) {
								case IDLE -> {
									focusedTarget = battle.getUnitAt(cursor);
									if(focusedTarget != null) {
										Sound.playEffect(Sound.UI_CONFIRM);
									}
								}
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
			if(battle.getOutcome() != null) {
				drawFrame();
				return showOutcome();
			}
		}
	}

	/** process a click or number key on row i: options without a target run on the second press, the others start or stop aiming */
	private void selectOption(int i) {
		hoveredOption = i;
		ActionOption ao = options.get(i);
		if(i == focusedOption) {
			if(!ao.needsTarget()) {
				execute(ao);
			}
			else {
				resetSelection();
			}
		}
		else {
			focusedOption = i;
			if(ao.needsTarget()) {
				mode = Mode.TARGETING;
				if(cursor == null) {
					cursor = getStartCell(ao);
				}
			}
			else {
				mode = Mode.IDLE;
			}
		}
	}
	
	private void resetSelection() {
		focusedOption = -1;
		mode = Mode.IDLE;
	}
	
	/** returns where the cursor starts when aiming o:
	 *  the pinned unit if o can target it, else the valid target nearest the player, else the player's own cell */
	private Position getStartCell(ActionOption o) {
		if(focusedTarget != null && o.check().apply(focusedTarget.getPosition()).legal()) {
			return focusedTarget.getPosition();
		}
		else {
			return cells.stream()
					.filter(c -> o.check().apply(c).legal())
					.min(Comparator.comparingInt(p -> p.distanceTo(self.getPosition())))
					.orElse(self.getPosition());
		}
	}

	/** the option checked against the cursor: the one answer the rows, Confirm, the preview box and execute all read */
	private Check checkCursor(ActionOption o) {
		return o.check().apply(cursor);
	}

	/** whether the row is lit: an option with a target is ready when any cell is a valid target */
	private boolean isReady(ActionOption o) {
		return o.needsTarget()? cells.stream().anyMatch(p -> o.check().apply(p).legal()) : o.check().apply(null).legal();
	}

	/** runs o on the cursor if its check passes, then returns to IDLE; otherwise shows why (with the error sound).
	 *  every way of running an option (ENTER, Confirm, a map click, a second press on its row) ends here */
	private void execute(ActionOption o) {
		Check c = checkCursor(o);
		if(c.legal()) {
			List<CombatEvent> news = o.action().apply(cursor);
			if (!playEventSounds(news)) {
				Sound.playEffect(Sound.UI_CONFIRM);
			}
			events.addAll(news);
			resetSelection();
		}
		else {
			ui.showWarning(c.reason());
		}
	}

	private void performCombatAction(CombatAction action) {
		switch(action) {
			case ACT -> {
				if(focusedOption > -1) {
					execute(options.get(focusedOption));
				}
			}
			case END -> {
				List<CombatEvent> news = battle.endPlayerTurn();
				playEventSounds(news);
				events.addAll(news);
				resetSelection();
			}
		}
	}
	
	/** plays the sound of the first event that has one; true if one played */
	private boolean playEventSounds(List<CombatEvent> news) {
		for (CombatEvent e : news) {
			String id = switch (e) {
				case CombatEvent.Attacked a when !a.hit() -> Sound.MISS;
				case CombatEvent.Attacked a when a.damage() == 0 -> Sound.BLOCK;
				case CombatEvent.Attacked a -> Sound.HIT;
				case CombatEvent.Defended d -> Sound.BLOCK;
				default -> null;
			};
			if (id != null) {
				Sound.playEffect(id);
				return true;
			}
		}
		return false;
	}
	
	/** shows the result card over the dimmed last frame, drawn and read in its own loop;
	 *  only Continue (Enter or a click) leaves it, returning the outcome */
	private Battle.Outcome showOutcome() {
		Battle.Outcome outcome = battle.getOutcome();
		GameCharacter player = self.getCharacter();
		int mapH = map.height() * CELL / 2;
		int cx = MAP_X + (MAP_W - OUTCOME_W) / 2, cy = MAP_Y / 2 + (mapH - OUTCOME_H) / 2;
		int color = Palette.colorOf(outcomeTone(outcome));

		//what happened and when, then the player's HP (after a defeat: what they come to with)
		List<Span> what = switch(outcome) {
			case VICTORY -> List.of(Span.note("All enemies defeated · round "), Span.plain(battle.getRound()));
			case DEFEATED -> List.of(nameSpan(self), Span.note(" is down · round "), Span.plain(battle.getRound()));
			case ESCAPED -> List.of(nameSpan(self), Span.note(" slips away · round "), Span.plain(battle.getRound()));
		};
		List<Span> hp = outcome == Outcome.DEFEATED
				? List.of(Span.note("You come to with  "), Span.plain("HP " + hpText(player)))
				: List.of(Span.note("HP  "), Span.plain(hpText(player)));

		canvas.dim(); //once: dimming again each round would darken the frame further
		while(true) {
			ui.clearButtons(); //the battle's own buttons stay dead under the card
			canvas.fill(cx, cy, OUTCOME_W, OUTCOME_H, Palette.PANEL);
			canvas.drawBox(cx, cy, OUTCOME_W, OUTCOME_H, color);
			drawCenteredOnMap(cy + 2, List.of(Span.of(String.join(" ", outcome.name().split("")), outcomeTone(outcome))));
			drawCenteredOnMap(cy + 5, what);
			drawCenteredOnMap(cy + 7, hp);
			//redrawn every round so it lights up under the mouse; its action is the outcome run() returns
			ui.drawButton(MAP_X + (MAP_W - Canvas.BUTTON_WIDTH) / 2, cy + OUTCOME_H - Canvas.BUTTON_HEIGHT - 1,
					"Continue", true, new Button(outcome));
			ui.present();
			UIHandler.Input in = ui.readInput();
			if(in.inputKey() == UIHandler.InputKey.ENTER
					|| in.inputKey() == UIHandler.InputKey.CLICK && in.button() instanceof Button b && b.action() == outcome) {
				return outcome;
			}
		}
	}

	/** the color of an outcome, shared by the result card and the log's last line so the two always match */
	private static Tone outcomeTone(Outcome o) {
		return switch(o) {
			case VICTORY -> Tone.HIGHLIGHT;
			case DEFEATED -> Tone.BAD;
			case ESCAPED -> Tone.KEYWORD;
		};
	}

	/** spans on row y, centered over the map (Canvas.drawTextCentered centers on the whole canvas, one column off) */
	private void drawCenteredOnMap(int y, List<Span> spans) {
		int len = spans.stream().mapToInt(s -> s.text().length()).sum();
		canvas.drawSpans(MAP_X + (MAP_W - len) / 2, y, len, spans);
	}

	//rendering
	private void drawFrame() {
		beginFrame();

		//identify scene objects and units
		List<SceneObject> objects = new ArrayList<>();
		List<Combatant> units = battle.getParticipantsAlive();

		for(Position p : cells) {//TODO collect more terrains when implemented
			if(map.terrainAt(p) == Terrain.TREE) {
				objects.add(SceneObject.ofProp(p, TREE_IMG));
			}
		}
		for(Combatant c : units) {
			objects.add(SceneObject.ofUnit(c));
		}

		if(focusedTarget != null && !focusedTarget.getCharacter().isAlive()) {
			focusedTarget = null;
		}

		//draw top-left side panel: player info
		canvas.fill(LEFT_X, SIDE_Y, SIDE_W, LEFT_TOP_H, Palette.PANEL);
		drawCharInfo(LEFT_X + PAD + IMG_W, SIDE_Y + 2, LEFT_X + PAD, SIDE_Y + 4, self);

		//draw bottom-left side panel: action panel
		drawActionPanel();

		//draw top-right side panel: the unit under the cursor, else the pinned one
		{
			canvas.fill(RIGHT_X, SIDE_Y, SIDE_W, RIGHT_TOP_H, Palette.PANEL);
			Combatant hovered = cursor == null? null : battle.getUnitAt(cursor);
			Combatant unit = hovered != null? hovered : focusedTarget;
			if(unit != null) {
				drawCharInfo(RIGHT_X + PAD, SIDE_Y + 2, RIGHT_X + SIDE_W - IMG_W - PAD, SIDE_Y + 4, unit);
			}
		}

		//draw bottom-right side panel: the preview of the focused action, else the log
		{
			int y = SIDE_Y + RIGHT_TOP_H + RIGHT_PANEL_MARGIN;
			int x = RIGHT_X + PAD;
			canvas.fill(RIGHT_X, y, SIDE_W, RIGHT_BOT_H, Palette.PANEL);

			if(focusedOption != -1) {
				//draw preview frame, shows what would happen for selected option and target if any
				canvas.drawBox(RIGHT_X, y, SIDE_W, RIGHT_BOT_H, Palette.KEY);
				canvas.drawText(x, y + 1, "PREVIEW", Palette.KEY);
				DetailView.draw(canvas, x, y + 3, SIDE_W - 2 * PAD, checkCursor(options.get(focusedOption)).preview());
				canvas.drawText(x, y + RIGHT_BOT_H - 2, "Enter confirm   Esc cancel", Palette.FAINT);
			}
			else {
				//draw combat event log, newest at the bottom and keeps the last N when fit; heights measured on a scratch canvas
				canvas.drawText(x, y + 1, "LOG", Palette.FAINT);
				int width = SIDE_W - 2 * PAD;
				int height = RIGHT_BOT_H - 4; //from y + 3 to the row above the panel's bottom

				Canvas scratch = new Canvas();
				int start = events.size(), used = 0;
				while(start > 0) {
					int h = gapAbove(start - 1) + scratch.drawSpans(0, 0, width, describeEvent(events.get(start - 1)));
					if(used + h > height) {
						break;
					}
					used += h;
					start--;
				}
				int ly = y + 3;
				for(int i = start; i < events.size(); i++) {
					if(i > start) {
						ly += gapAbove(i);
					}
					ly = canvas.drawSpans(x, ly, width, describeEvent(events.get(i)));
				}
			}
		}

		//draw map
		drawMap(objects);

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
					.mapToInt(unit -> unit.getCharacter().getName().length() + hpText(unit.getCharacter()).length() + CARD_W_BUFFER)
					.toArray();
			int totalW = Arrays.stream(widths).sum() + CARD_MARGIN * (units.size() - 1);
			int x0 = MAP_X + (MAP_W - totalW) / 2;


			canvas.fill(x0, CARD_Y, totalW, 2, Palette.PANEL);

			int x = x0;
			for(int i = 0; i < units.size(); i++) {
				Combatant unit = units.get(i);
				GameCharacter ch = unit.getCharacter();

				int w = widths[i];
				boolean isCurrent = battle.getCurrentActor().equals(unit);

				if(isCurrent) {
					canvas.fill(x, CARD_Y, w, 2, Palette.PANEL_LIGHT);
					canvas.drawText(x, CARD_Y, FOCUS_INDICATOR, Palette.KEY);
					canvas.drawText(x, CARD_Y + 1, FOCUS_INDICATOR, Palette.KEY);
				}
				canvas.drawText(x + 2, CARD_Y, "▸", isCurrent? Palette.KEY : Palette.MUTED);
				canvas.drawText(x + 4, CARD_Y, ch.getName(), isCurrent? Palette.KEY : sideColor(unit));
				drawRight(x + w - 2, CARD_Y, hpText(ch), isCurrent? Palette.TEXT : Palette.FAINT);
				drawBar(x + 4, CARD_Y + 1, w - 6, ch.getHp(), ch.getMaxHp(), sideColor(unit));

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
			Check c = checkCursor(o);
			boolean ready = isReady(o);

			int labelColor = Palette.MUTED;
			int detailColor = Palette.FAINT;
			int pipColor = ready? Palette.KEY : Palette.FAINT;
			if(hoveredOption == i || focusedOption == i) {
				int indicatorColor = Palette.MUTED;
				if(focusedOption == i) {
					labelColor = ready? Palette.KEY : Palette.MUTED;
					indicatorColor = Palette.KEY;
					canvas.fill(LEFT_X, y, SIDE_W, 4, Palette.PANEL_LIGHT);
				}
				for(int j=0; j<4; j++) {
					canvas.drawText(LEFT_X, y+j, FOCUS_INDICATOR, indicatorColor);
				}
			}
			if(ready) {
				detailColor = Palette.MUTED;
				labelColor = Palette.TAG;
			}
			canvas.drawText(x, y + 1, String.valueOf(i+1), labelColor);
			canvas.drawText(labelX, y + 1, o.label(), labelColor);
			drawPips(LEFT_X + SIDE_W - PAD, y + 1, c.cost(), c.cost(), pipColor);
			canvas.drawParagraph(labelX, y + 2, LEFT_X + SIDE_W - PAD - labelX, c.detail(), detailColor);

			ui.addButtonBox(LEFT_X, y, SIDE_W, 4, new Button(i));

			y += 4;
		}
		int buttonY = SIDE_Y + LEFT_TOP_H + LEFT_BOT_H + LEFT_PANEL_MARGIN - Canvas.BUTTON_HEIGHT;
		ui.drawButton(LEFT_X + 1, buttonY, "End Turn", true, new Button(CombatAction.END));
		//a dim Confirm still takes the click, so performCombatAction can say why it cannot run
		ui.drawButton(LEFT_X + SIDE_W - Canvas.BUTTON_WIDTH - 1, buttonY, "Confirm",
				focusedOption != -1 && checkCursor(options.get(focusedOption)).legal(), new Button(CombatAction.ACT));
	}

	/** draws a unit's name, level, HP, AP (only on its own turn), armor, speed and steps left, with its large figure at iconX, iconY */
	private void drawCharInfo(int x, int y, int iconX, int iconY, Combatant c) {
		GameCharacter ch = c.getCharacter();
		int textW = INFO_TEXT_W;
		boolean isCurrent = battle.getCurrentActor().equals(c);

		PixelImage.largeFigure(figureId(c)).ifPresent(i -> canvas.drawImage(iconX, iconY, i));
		canvas.drawText(x, y, ch.getName(), sideColor(c));
		canvas.drawText(x, ++y, "Level %d".formatted(ch.getLevel()), Palette.MUTED);

		canvas.drawText(x, y+=2, "HP", Palette.MUTED);
		drawRight(x + textW, y, hpText(ch), Palette.TEXT);
		drawBar(x, ++y, textW, ch.getHp(), ch.getMaxHp(), sideColor(c));

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

	/** the unit's figure id: the enemy's own, or the player's */
	private static String figureId(Combatant c) {
		return c.getCharacter() instanceof Enemy e? e.getId() : PLAYER_IMG;
	}

	/** the color of the unit's side, for its name and HP bars */
	private static int sideColor(Combatant c) {
		return Palette.colorOf(c.getSide());
	}

	private static String hpText(GameCharacter ch) {
		return "%d / %d".formatted(ch.getHp(), ch.getMaxHp());
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
	
	/** returns rows above log entry i: two blank rows before each turn's start, except at the very top of the log */
	private int gapAbove(int i) {
	    return i > 0 && events.get(i) instanceof CombatEvent.TurnStarted? 2 : 0;
	}
	
	private static Span nameSpan(Combatant c) { return Span.of(c.getCharacter().getName(), c.getSide()); } 

	/** return the log line of an event in colored pieces;
	 *  names by side, damage red, other numbers bright, states sky, the rest dim */
	private List<Span> describeEvent(CombatEvent e){
		return switch(e) {
			case CombatEvent.TurnStarted t -> List.of(
					Span.of("Round " + t.round() + " · ", Tone.HIGHLIGHT), nameSpan(t.unit()), Span.note("'s turn"));
			case CombatEvent.Moved m -> List.of(
					nameSpan(m.unit()),	Span.note(" moves "), Span.plain(m.path().size()), Span.note(" steps"));
			case CombatEvent.Attacked a when !a.hit() -> List.of(
					nameSpan(a.attacker()),	Span.note(" attacks "),	nameSpan(a.target()), Span.note(" and "), Span.keyword("misses")); 
			case CombatEvent.Attacked a when a.damage() == 0 -> List.of(
					nameSpan(a.attacker()),	Span.note(" attacks "),	nameSpan(a.target()), Span.note(" and could not break their armor"));
			case CombatEvent.Attacked a -> {
				List<Span> s = new ArrayList<>();
				if(a.crit()) {
					s.add(Span.of("Critical! ", Tone.HIGHLIGHT));
				}
				s.addAll(List.of(
						nameSpan(a.attacker()),	Span.note(" attacks "),	nameSpan(a.target()), Span.note(" for "),
						Span.of(a.damage(), Tone.BAD), Span.note(" damage (HP "), Span.plain(a.hpAfter()), Span.note(")")));
				yield s;
			}
			case CombatEvent.Defended d when d.type() == DefenseType.DODGE -> List.of(
					nameSpan(d.unit()), Span.note(" gets ready to "), Span.keyword("dodge"));
			case CombatEvent.Defended d -> List.of(
					nameSpan(d.unit()), Span.keyword(" guards "), Span.plain("+" + d.guard()), Span.note(" armor"));
			case CombatEvent.Defeated d -> List.of(
					nameSpan(d.unit()), Span.note(" is "), Span.keyword("defeated"));
			case CombatEvent.Evacuated v -> List.of(
					nameSpan(v.unit()), Span.keyword(" escapes "), Span.note("the battle"));
			case CombatEvent.BattleEnded b -> switch(b.outcome()) {
					case VICTORY -> List.of(Span.of("Victory", outcomeTone(b.outcome())));
					case DEFEATED -> List.of(Span.of("Defeated", outcomeTone(b.outcome())));
					case ESCAPED -> List.of(Span.of("Escaped", outcomeTone(b.outcome())));
			};
		};
	}
	


	//map drawing
	/** draws the center battle map area and interactable elements within */
	private void drawMap(List<SceneObject> objects) {
		//floor tile
		PixelImage.ground(TILE_IMG).ifPresentOrElse(
				img -> canvas.drawImage(MAP_X, MAP_Y, img) ,
				() -> canvas.fill(MAP_X, MAP_Y / 2, map.width() * CELL, map.height() * CELL / 2, Palette.PANEL));

		//register clickable cells; the button index is the cell's index in cells
		for(int i = 0; i < cells.size(); i++) {
			Position p = cells.get(i);
			ui.addButtonBox(MAP_X + p.x() * CELL, MAP_Y / 2 + p.y() * CELL / 2, CELL, CELL / 2,
					new Button(i, Button.Area.MAP_CELL));
		}

		//tint valid targets in targeting mode (move: deeper where an attack is still affordable after it), the rest dimmed
		if(mode == Mode.TARGETING) {
			ActionOption aimed = options.get(focusedOption);
			for(Position p : cells) {
				Check c = aimed.check().apply(p);
				if(!c.legal()) {
					drawCellMovePreview(p.x(), p.y(), 1, Palette.BG, 0.2);
				}
				else if(focusedOption == MOVE_OPTION) {
					if(self.getAp() - c.cost() >= Combatant.MAIN_ATTACK_COST) {
						drawCellMovePreview(p.x(), p.y(), 1, Palette.SUCCESS, 0.15);
					}
					else {
						drawCellMovePreview(p.x(), p.y(), 1, Palette.WARNING,0.15);
					}
				}
				else {
					drawCellMovePreview(p.x(), p.y(), 1, Palette.DANGER, 0.4);
				}
			}
		}

		//draw grid lines
		for(int y = 0; y < map.height() * CELL; y++) {
			for(int x = 0; x < map.width() * CELL; x++) {
				if(x % CELL == 0 || y % CELL == 0) {
					canvas.shadePixel(MAP_X + x, MAP_Y + y, mode == Mode.TARGETING? 0.35 : 0.08);
				}
			}
		}

		//draw path indication at cursor in targeting mode
		MovePreview aimedMove = null;
		if(mode == Mode.TARGETING &&  focusedOption == MOVE_OPTION && cursor != null) {
			aimedMove = battle.previewMove(cursor);
			
			if(aimedMove.legal()) {
				List<Position> paths = new ArrayList<>(aimedMove.path());
				paths.add(0, self.getPosition());
				for(int c = 1; c < paths.size(); c++) {
					int x0 = MAP_X + paths.get(c-1).x() * CELL + 6;
					int x1 = MAP_X + paths.get(c).x() * CELL + 6;
					int y0 = calcFootY(paths.get(c-1).y());
					int y1 = calcFootY(paths.get(c).y());
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
			objects.add(SceneObject.ofGhost(cursor, PLAYER_IMG, !aimedMove.legal()));
		}

		//reorder objects for rendering sequence using y then x cords, the lower the later for z-layering
		objects.sort(Comparator.comparingInt((SceneObject o) -> o.cell().y())
				.thenComparing(o -> o.cell().x()));

		//draw shadows
		for(SceneObject o : objects) {
			Position cell = o.cell();
			canvas.drawContactShadow(calcImgX(cell.x(), o.image()), calcFootY(cell.y()), o.image(), o.isUnit()? 0.2 : 0.15);
		}

		//draw objects; the one right below a pointed-at unit fades so it does not hide it
		for(SceneObject o : objects) {
			Position cell = o.cell();
			int px = calcImgX(cell.x(), o.image());
			int py = calcFootY(cell.y()) - o.image().getHeight() + 1;
			if(cursor != null && cell.x() == cursor.x() && cell.y() - cursor.y() == 1 && battle.getUnitAt(cursor) != null) {
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
				GameCharacter ch = o.unit().getCharacter();
				int fill = (int) Math.round((double) ch.getHp() * HP_BAR_X_SPAN / ch.getMaxHp());
				int left = MAP_X + o.cell().x() * CELL;
				for(int i=0; i < HP_BAR_X_SPAN; i++) {
					canvas.drawPixel(left + HP_BAR_X_OFFSET + i, calcFootY(o.cell().y()) + HP_BAR_Y_OFFSET,
							i < fill? sideColor(o.unit()) : Palette.LINE);
				}
			}
		}

		//draw corners: the pinned unit, then the cursor over it
		if(focusedTarget != null) {
			drawFocusedCell(focusedTarget.getPosition(), focusedTarget.isEnemy()? Palette.DANGER : Palette.SUCCESS);
		}
		if(aimedMove != null) {
			drawFocusedCell(cursor, aimedMove.legal()? Palette.KEY : Palette.LINE);
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
					canvas.drawPixel(px, py, Palette.mix(canvas.getPixel(px, py), color, 0.67));
				}
			}
		}
	}

	/** returns the pixel-x position of the left origin of the object */
	private int calcImgX(int cellX, PixelImage img) {
		return MAP_X + cellX * CELL + (CELL - img.getWidth()) / 2;
	}

	/** returns the pixel-y position of the foot of a object/unit */
	private int calcFootY(int cellY) {
		return MAP_Y + cellY * CELL + CELL + FOOT_Y_OFFSET;
	}
}
