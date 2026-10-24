package com.endlessadventure.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import com.endlessadventure.StatRules;
import com.endlessadventure.combat.Battle.CombatEvent.Defeated;
import com.endlessadventure.combat.Combatant.DefenseType;
import com.endlessadventure.combat.Combatant.Side;
import com.endlessadventure.entity.Enemy;
import com.endlessadventure.entity.Player;

public class Battle {
	public enum Outcome { VICTORY, DOWNED, EVACUATED }
	
	public sealed interface CombatEvent {
	    record TurnStarted(Combatant unit, int round) implements CombatEvent {}
	    record Moved(Combatant unit, List<Position> path, int apSpent) implements CombatEvent {}
	    record Attacked(Combatant attacker, Combatant target, boolean hit, int raw, int damage, int hpAfter) implements CombatEvent{}
	    record Defended(Combatant unit, Combatant.DefenseType type, int guard) implements CombatEvent{}
	    record Defeated(Combatant unit) implements CombatEvent{}
	    record Evacuated(Combatant unit) implements CombatEvent{}
	    record BattleEnded(Outcome outcome) implements CombatEvent{}
	}
	
	/** what moving to the destination looks like; nothing changes until move() is invoked */
	public record MovePreview(boolean legal, String reason, List<Position> path, int apCost) {}
	
	/** what attacking this target would do; nothing changes until attack() is invoked */
	public record AttackPreview(boolean legal, String reason, int raw, int armor, int damage, int hitChance) {}
	
	/** what defending would do; nothing changes until defend() is invoked */
	public record DefensePreview(boolean legal, String reason, Combatant.DefenseType type, int guard, Map<Combatant,Integer> dodgeChances) {}
	
	/** whether evacuation can be executed; nothing changes until evacuate() is invoked */
	public record EvacuatePreview(boolean legal, String reason) {}
	
	private final BattleMap map;
	private final List<Combatant> participants = new ArrayList<>(); //ordered by turn order
	private final RandomGenerator rng;
	
	private Outcome outcome = null;
	private int round = 1;
	private int currentInd = 0; //index of the current actor
	
	public Battle(BattleMap map, Player player, List<Enemy> enemies, RandomGenerator rng) {
		this.map = map;
		this.rng = rng;
		if(enemies.size() > map.getEnemySpawns().size()) {
			throw new IllegalArgumentException("Invalid battle: enemy count exceeds spawn positions.");
		}
		
		participants.add(new Combatant(player, Side.PLAYER, map.getPlayerSpawn()));
		for(int i = 0; i < enemies.size(); i ++) {
			participants.add(new Combatant(enemies.get(i), Side.ENEMY, map.getEnemySpawns().get(i)));
		}
		
		participants.sort(Comparator.comparingInt((Combatant c ) -> c.getCharacter().getInitiative())
				.reversed()
				.thenComparing(Combatant::getSide));
	}
	
	BattleMap getMap() {return map;}
	
	/** shortest paths for this combatant */
	BattleMap.SearchedPaths pathsFor(Combatant mover){
		return map.searchPaths(mover.getPosition(), p -> occupiedByOther(mover, p));
	}
	
	public List<Combatant> getParticipants(){return List.copyOf(participants);}
	
	public Outcome getOutcome() {return outcome;}
	
	private Combatant currentActor() {
		return participants.get(currentInd);
	}
	
	private void requireOngoing(String action) {
		if(outcome != null) {
			throw new IllegalStateException("Illegal " + action + " action: the battle has already ended.");
		}
	}
	
	
	
	//turn logics
	/** start the encounter, starts the turn of the first combatant */
	public List<CombatEvent> start(){
		List<CombatEvent> events = new ArrayList<>();
		startTurn(events);
		return runEnemyTurns(events);
	}
	
	/** reset the actor's states */
	private void startTurn(List<CombatEvent> events) {
		Combatant actor = currentActor();
		actor.startTurn();
		events.add(new CombatEvent.TurnStarted(actor, round));
	}
	
	/**the player ends their turn;
	 * runs the following enemy turns and stops at the next beginning of player turn */
	public List<CombatEvent> endPlayerTurn(){
		requireOngoing("end turn");
		if (currentActor().getSide() != Side.PLAYER) {
			throw new IllegalStateException("Illegal battle endTurn invocation: Not the player's turn.");
		}
		List<CombatEvent> events = new ArrayList<>();
		nextTurn(events);
		return runEnemyTurns(events);
	}
	
	private List<CombatEvent> runEnemyTurns(List<CombatEvent> events){
		while(outcome == null && currentActor().getSide() == Side.ENEMY) {
			events.addAll(EnemyAI.takeTurn(this, currentActor()));
			if(outcome == null) {
				nextTurn(events);
			}
		}
		return events;
	}
	
	/** skip to the next living combatant and start its turn */
	private void nextTurn(List<CombatEvent> events) {
		do {
			currentInd = (currentInd + 1) % participants.size();
			if(currentInd == 0) {
				round++;
			}
		} while(!currentActor().getCharacter().isAlive()); //skip until the next living combatant
		startTurn(events);
	}
	
	/** cells the mover cannot enter: where other living combatants stand */
	private boolean occupiedByOther(Combatant mover, Position p) {
	    for (Combatant c : participants) {
	        if (c != mover && c.getCharacter().isAlive() && c.getPosition().equals(p)) {
	            return true;
	        }
	    }
	    return false;
	}
	
	
	
	//actions
	public MovePreview previewMove(Position dest) {
		Combatant actor = currentActor();
		if(!map.inBounds(dest)) {
			return new MovePreview(false, "destination out of bound.", List.of(), 0);
		}
		
		BattleMap.SearchedPaths paths = pathsFor(actor);
		int steps = paths.stepsTo(dest);
		if(steps < 1 ) {
			return new MovePreview(false, "in-place or unreachable destination", List.of(), 0);
		}
		int left = actor.getStepsLeft();
		int cost = left >= steps ? 0 : Math.ceilDiv((steps - left), actor.getCharacter().getSpeed()) * Combatant.MOVE_COST;
		if(cost > actor.getAp()) {
			return new MovePreview(false, "unaffordable", paths.pathTo(dest), cost);
		}
		return new MovePreview(true, "", paths.pathTo(dest), cost);
	}
	
	public List<CombatEvent> move(Position dest){
		requireOngoing("move");
		Combatant actor = currentActor();
		MovePreview preview = previewMove(dest);
		if(!preview.legal()) {
			throw new IllegalArgumentException("Illegal move action: " + preview.reason());
		}
		actor.moveTo(dest, preview.path().size(), preview.apCost());
		return List.of(new CombatEvent.Moved(actor, preview.path(), preview.apCost()));
	}
	
	public AttackPreview previewAttack(Combatant target) {
		if(target == null) {
			throw new IllegalArgumentException("Invalid attack preview: target cannot be null.");
		}
		Combatant actor = currentActor();
		int armor = target.getCharacter().getArmor() + target.getGuard(); //effective armor: worn armor plus the active defense
		if(!target.getCharacter().isAlive() || actor.getSide() == target.getSide()) {
			return new AttackPreview(false, "not a valid target", 0, armor, 0, 0);
		}
		if(!map.canStep(actor.getPosition(), target.getPosition())) {
			return new AttackPreview(false, "unreachable target", 0, armor, 0, 0);
		}
		if(actor.getAp() < Combatant.ATTACK_COST) {
			return new AttackPreview(false, "not enough AP", 0, armor, 0, 0);
		}

		int raw = actor.getCharacter().getMainAttackPower();
		int hitChance = target.getDefense() != DefenseType.DODGE? 100 :
			StatRules.checkChance(actor.getCharacter().getMeleeCheck(), target.getCharacter().getDodgePower());
		return new AttackPreview(true, "", raw, armor, Math.max(0, raw - armor), hitChance);
	}
	
	public List<CombatEvent> attack(Combatant target){
		requireOngoing("attack");
		AttackPreview preview = previewAttack(target);
		if(!preview.legal()) {
			throw new IllegalArgumentException("Illegal attack action: " + preview.reason());
		}
		List<CombatEvent> events = new ArrayList<>();
		Combatant actor = currentActor();
		actor.spendAp(Combatant.ATTACK_COST);

		boolean hit = true;
		if(preview.hitChance() < 100) {
			hit = rng.nextInt(100) < preview.hitChance();
		}
		int dmg = preview.damage();
		if(target.getSide() == Side.PLAYER && !target.hasActed() && target.getCharacter().getHp() <= dmg){
			//activates first-round-protection
			dmg = target.getCharacter().getHp() - 1; 
		}
		if(hit) {
			target.getCharacter().takeDamage(dmg);
		}
		else {
			dmg = 0;
		}
		events.add(new CombatEvent.Attacked(actor, target, hit, preview.raw(), dmg, target.getCharacter().getHp()));
		checkEnd(target, events);
		return events;
	}
	
	private void checkEnd(Combatant target, List<CombatEvent> events) {
		if(target.getCharacter().isAlive()) {return;}
		
		events.add(new Defeated(target));
		if(target.getCharacter() instanceof Player) {
			outcome = Outcome.DOWNED;
		}
		else{
			boolean victory = true;
			for(Combatant p : participants) {
				if(p.getSide() == Side.ENEMY && p.getCharacter().isAlive()) {
					victory = false;
					break;
				}
			}
			if(victory) {
				outcome = Outcome.VICTORY;
			}
		}
		if(outcome != null) {
			events.add(new CombatEvent.BattleEnded(outcome));
		}
	}
	
	public DefensePreview previewDefend(Combatant.DefenseType type) {
		Combatant actor = currentActor();
		if(type == null) {
			throw new IllegalArgumentException("Invalid defend action: type cannot be null");
		}
		if(actor.getDefense() != null) {
			return new DefensePreview(false, "already in defense", type, 0, Map.of());
		}
		if(actor.getAp() < Combatant.DEFEND_COST) {
			return new DefensePreview(false, "not enough AP", type, 0, Map.of());
		}
		Map<Combatant, Integer> chances = Map.of();
		switch(type) {
			case SHIELD -> {
				if(actor.getCharacter().getShieldGuard() == 0) {
					return new DefensePreview(false, "shield is not equipped", type, 0, Map.of());
				}
			}
			case DODGE -> {
				if(!actor.getCharacter().canDodge()) {
					return new DefensePreview(false, "dodge is disabled", type, 0, Map.of());
				}
				chances = participants.stream()
						.filter(c -> c.getSide() != actor.getSide() && c.getCharacter().isAlive())
						.collect(Collectors.toMap(Function.identity(),
								c -> StatRules.checkChance(c.getCharacter().getMeleeCheck(), actor.getCharacter().getDodgePower())));
			}
			default -> {}
		}
		return new DefensePreview(true, "", type, actor.getGuard(type), chances);
	}
	
	public List<CombatEvent> defend(Combatant.DefenseType type){
		requireOngoing("defend");
		DefensePreview preview = previewDefend(type);
		if(!preview.legal()) {
			throw new IllegalArgumentException("Illegal defend action: " + preview.reason());
		}
		
		Combatant actor = currentActor();
		List<CombatEvent> events = new ArrayList<>();
		actor.defend(type);
		events.add(new CombatEvent.Defended(actor, type, actor.getGuard()));
		return events;
	}
	
	public EvacuatePreview previewEvacuate() {
		Combatant actor = currentActor();
		if(actor.getSide() != Side.PLAYER) { //TODO change when enemy evacuation is implemented
			return new EvacuatePreview(false, "only player can evacuate");
		}
		if(!actor.getPosition().equals(actor.getTurnStartPosition())) {
			return new EvacuatePreview(false, "cannot evacuate after moving in the same turn");
		}
		if(!map.isEdge(actor.getPosition())) {
			return new EvacuatePreview(false, "must be on the edge of the map");
		}
		if(actor.getAp() < Combatant.EVACUATE_COST) {
			return new EvacuatePreview(false, "not enough AP");
		}
		return new EvacuatePreview(true, "");
	}
	
	public List<CombatEvent> evacuate(){
		requireOngoing("evacuate");
		EvacuatePreview preview = previewEvacuate();
		if(!preview.legal()) {
			throw new IllegalArgumentException("Illegal evacuate action: " + preview.reason());
		}
		
		Combatant actor = currentActor();
		actor.spendAp(Combatant.EVACUATE_COST);
		outcome = Outcome.EVACUATED;
		return List.of(new CombatEvent.Evacuated(actor), new CombatEvent.BattleEnded(outcome));
	}
}
