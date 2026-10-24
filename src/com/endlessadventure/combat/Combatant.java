package com.endlessadventure.combat;

import com.endlessadventure.StatRules;
import com.endlessadventure.entity.GameCharacter;

/** one unit in a battle: the character plus the state that only lasts for this battle */
public class Combatant{
	private static final int MAX_AP = 3;

	//AP Cost constants
	static final int MOVE_COST = 1;
	static final int ATTACK_COST = 2;
	static final int DEFEND_COST = 1;
	static final int EVACUATE_COST = 2;

	public enum Side { PLAYER, ENEMY }

	public enum DefenseType { SIMPLE, SHIELD, DODGE }

	private final GameCharacter character;
	private final Side side;

	private Position position;
	private Position turnStartPosition;
	private int ap = MAX_AP;
	private int stepsLeft = 0;
	private boolean acted = false; //whether or not the combatant has started a turn in the current battle
	private DefenseType defense = null;

	Combatant(GameCharacter character, Side side, Position position) {
		this.character = character;
		this.side = side;
		this.position = position;
		turnStartPosition = position;
	}

	public GameCharacter getCharacter() {return character;}

	public Side getSide() {return side;}

	public Position getPosition() {return position;}

	Position getTurnStartPosition() {return turnStartPosition;}

	public int getAp() {return ap;}

	public int getStepsLeft() {return stepsLeft;}

	public boolean hasActed() {return acted;}

	public DefenseType getDefense() {return defense;}

	/** everything a unit resets when its own turn begins; a defense lasts until this point */
	void startTurn() {
		turnStartPosition = position;
		ap = MAX_AP;
		stepsLeft = 0;
		acted = true;
		defense = null;
	}

	void spendAp(int cost) {
		ap -= cost;
	}

	/** buys movement with apCost AP (speed steps each), then walks steps cells to dest */
	void moveTo(Position dest, int steps, int apCost) {
		ap -= apCost;
		stepsLeft += apCost * character.getSpeed() - steps;
		position = dest;
	}

	void defend(DefenseType type) {
		spendAp(DEFEND_COST);
		defense = type;
	}

	int getGuard() {
		return getGuard(defense);
	}

	int getGuard(DefenseType defense) {
		return switch(defense) {
			case SIMPLE, SHIELD -> {
				yield StatRules.scaleToLevel(StatRules.SIMPLE_GUARD_PER_TIER, character.getLevel()) +
				(defense == DefenseType.SHIELD? character.getShieldGuard() : 0);
			}
			case null, default -> 0; //DODGE or null
		};
	}
}
