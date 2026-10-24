package com.endlessadventure.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.endlessadventure.combat.Battle.CombatEvent;
import com.endlessadventure.combat.BattleMap.SearchedPaths;
import com.endlessadventure.combat.Combatant.DefenseType;
import com.endlessadventure.entity.Enemy;

/** provides static method takeTurn() that controls enemy action based on their behavior */ 
class EnemyAI {
	
	/** runs the current enemy's whole turn; returns everything that happened */
	static List<CombatEvent> takeTurn(Battle battle, Combatant self) {
	    List<CombatEvent> events = new ArrayList<>();
	    while (battle.getOutcome() == null) {
	        List<CombatEvent> step = switch (((Enemy) self.getCharacter()).getBehavior()) {
	            case PRESSING -> performPressingAction(battle, self);
	            //TODO add additional cases and xxxStep methods when new behaviors are implemented
	        };
	        if (step == null) {
	            break;            // no rule applies: the turn is over
	        }
	        events.addAll(step);
	    }
	    return events;
	}
	
	private static List<CombatEvent> performPressingAction(Battle battle, Combatant self){
		SearchedPaths paths = battle.pathsFor(self);
		Combatant target = battle.getParticipants().stream() //targets the closest living combatant that is not of self's side
				.filter(p -> p.getSide() != self.getSide() && p.getCharacter().isAlive())
				.min(Comparator.comparingInt(p -> self.getPosition().distanceTo(p.getPosition())))
				.orElse(null);

		if(target == null) {
			throw new IllegalStateException("Invalid battle: enemy " + self.getCharacter().toString() + "cannot find a valid target");
		}
		Position tpos = target.getPosition();
		BattleMap map = battle.getMap();
		boolean adjacent = map.canStep(self.getPosition(), tpos);
		
		//if adjacent to target and can attack, attack
		if(adjacent && battle.previewAttack(target).legal()) {
			return battle.attack(target);
		}
		
		//create BFS paths and choose the closest attackable position
		List<Position> targetSurroundings = new ArrayList<>();
		for(int dx = -1; dx <= 1; dx++) {
			for(int dy = -1; dy <= 1; dy++) {
				targetSurroundings.add(new Position(tpos.x() + dx, tpos.y() + dy));
			}
		}
		Position targetCell = targetSurroundings.stream()
				.filter(p -> map.inBounds(p) && map.canStep(p, tpos) && paths.stepsTo(p) >= 1)
				.min(Comparator.comparingInt(p -> paths.stepsTo(p)))
				.orElse(null);
		
		
		//if not adjacent but has attackable position
		if(!adjacent && targetCell != null) {
			if(battle.previewMove(targetCell).legal()) {//if can move to target position, move
				return battle.move(targetCell);
			}
			else {//if cannot move directly, go to the closest position
				List<Position> path = paths.pathTo(targetCell);
				for(Position p : path.reversed()) {
					if(battle.previewMove(p).legal()) {
						return battle.move(p);
					}
				}
			}
		}
		
		//if no movement available, defend; if nothing is available; do nothing (return null)
		if(battle.previewDefend(DefenseType.SHIELD).legal()) {
			return battle.defend(DefenseType.SHIELD);
		}
		if (battle.previewDefend(DefenseType.SIMPLE).legal()){
			return battle.defend(DefenseType.SIMPLE);
		}
		return null;
	}
}
