package com.endlessadventure.combat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.endlessadventure.StatRules;
import com.endlessadventure.combat.Battle.AttackPreview;
import com.endlessadventure.combat.Battle.DefensePreview;
import com.endlessadventure.entity.GameCharacter;
import com.endlessadventure.item.Equipment.Slot;
import com.endlessadventure.item.Weapon;

/** one unit in a battle: the character plus the state that only lasts for this battle.
 *  It answers for its own side of every rule: what its attacks do, how it takes a hit, what it can afford;
 *  Battle decides whose turn it is and checks what needs the map or the other units. */
public class Combatant{
	private static final int MAX_AP = 3;

	//AP Cost constants
	static final int MOVE_COST = 1;
	static final int MAIN_ATTACK_COST = 2;
	static final int OFF_HAND_ATTACK_COST = 1;
	static final int DEFEND_COST = 1;
	static final int EVACUATE_COST = 2;

	public enum Side { PLAYER, ENEMY }

	public enum AttackType { MAIN, OFF_HAND }

	public enum DefenseType { SIMPLE, SHIELD, DODGE }

	private final GameCharacter character;
	private final Side side;

	private Position position;
	private Position turnStartPosition;
	private int ap = MAX_AP;
	private int stepsLeft = 0;
	private boolean acted = false; //whether or not the combatant has started a turn in the current battle
	private boolean mainAttacked = false; //whether or not the combatant has used it main atk this turn, used to enable off hand atk
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

	public boolean getMainAttacked() {return mainAttacked;}

	public DefenseType getDefenseType() {return defense;}

	int getGuardValue() {
		return checkGuardValue(defense);
	}

	/** everything a unit resets when its own turn begins; a defense lasts until this point */
	void startTurn() {
		turnStartPosition = position;
		ap = MAX_AP;
		stepsLeft = 0;
		acted = true;
		mainAttacked = false;
		defense = null;
	}

	void spendAp(int cost) {
		ap -= cost;
	}



	//moving
	/** AP needed to walk this many steps, using the movement already bought this turn first */
	int costOfMove(int steps) {
		return steps <= stepsLeft ? 0 : Math.ceilDiv(steps - stepsLeft, character.getSpeed()) * MOVE_COST;
	}

	/** buys movement with apCost AP (speed steps each), then walks steps cells to dest */
	void moveTo(Position dest, int steps, int apCost) {
		ap -= apCost;
		stepsLeft += apCost / MOVE_COST * character.getSpeed() - steps;
		position = dest;
	}



	//attacking
	static int costOfAttack(AttackType type) {
		return type == AttackType.MAIN? MAIN_ATTACK_COST : OFF_HAND_ATTACK_COST;
	}

	/** the weapon this attack is made with, or null when that hand holds no weapon */
	private Weapon weaponOfType(AttackType type) {
		Slot slot = type == AttackType.MAIN? Slot.MAIN_HAND : Slot.OFF_HAND;
		return character.getEquipped(slot) instanceof Weapon w? w : null;
	}

	/** what this unit's attack on target would do; Battle has already checked that target is a living, adjacent enemy.
	 *  This side supplies the weapon, raw damage and melee check; the target answers for its armor, guard and dodge */
	AttackPreview previewAttack(Combatant target, AttackType type) {
		Weapon weapon = weaponOfType(type);
		if(type == AttackType.OFF_HAND) {
			if(!mainAttacked) {
				return AttackPreview.illegal("must perform main hand attack first");
			}
			if(weapon == null) {
				return AttackPreview.illegal("no off hand weapon equipped");
			}
		}
		if(ap < costOfAttack(type)) {
			return AttackPreview.illegal("not enough AP");
		}

		boolean crit = target.isCritBy(weapon);
		int raw = (type == AttackType.MAIN? character.getMainAttackPower() : character.getOffHandAttackPower())
				* (crit? StatRules.CRIT_MULTIPLIER : 1);
		boolean breakGuard = weapon != null && weapon.hasProperty(Weapon.Property.BREAK_GUARD);
		int pen = weapon == null? 0 : weapon.getPenetration();
		int armor = target.effectiveArmor(breakGuard, pen);
		int check = type == AttackType.MAIN? character.getMainMeleeCheck() : character.getOffHandMeleeCheck();
		return new AttackPreview(true, "", raw, crit, armor, breakGuard? target.getGuardValue() : 0, pen,
				Math.max(0, raw - armor), target.hitChanceAgainst(check));
	}

	/** pays for the attack and, if it hit, deals the previewed damage; returns the damage the target actually took */
	int attack(Combatant target, AttackType type, AttackPreview preview, boolean hit) {
		spendAp(costOfAttack(type));
		if(type == AttackType.MAIN) {
			mainAttacked = true;
		}
		return hit? target.receiveHit(preview.damage()) : 0;
	}



	//being attacked
	/** armor against one attack: worn armor plus the active defense's guard (unless ignored), less penetration, never below 0 */
	int effectiveArmor(boolean ignoreGuard, int penetration) {
		return Math.max(0, character.getArmor() + (ignoreGuard? 0 : getGuardValue()) - penetration);
	}

	/** percent chance that an attack with this melee check hits; only a dodging unit can be missed */
	int hitChanceAgainst(int meleeCheck) {
		return defense == DefenseType.DODGE? StatRules.checkChance(meleeCheck, character.getDodgeCheck()) : 100;
	}

	/** whether an attack with this weapon crits this unit: lethal at half HP or less, assassinate before this unit has acted */
	boolean isCritBy(Weapon weapon) {
		if(weapon == null) {
			return false;
		}
		return weapon.hasProperty(Weapon.Property.ASSASSINATE) && !acted ||
				weapon.hasProperty(Weapon.Property.LETHAL) && character.getHp() * 2 <= character.getMaxHp();
	}

	/** takes the damage of a hit and returns how much was taken;
	 *  first-round protection: a player-side unit that has not acted yet keeps at least 1 HP */
	int receiveHit(int damage) {
		if(side == Side.PLAYER && !acted) {
			damage = Math.min(damage, character.getHp() - 1);
		}
		character.takeDamage(damage);
		return damage;
	}



	//defending
	/** whether this unit can take this defense now; for a dodge, also how likely each opponent is to hit it */
	DefensePreview previewDefend(DefenseType type, List<Combatant> opponents) {
		if(defense != null) {
			return DefensePreview.illegal(type, "already in defense");
		}
		if(ap < DEFEND_COST) {
			return DefensePreview.illegal(type, "not enough AP");
		}
		if(type == DefenseType.SHIELD && character.getShieldGuard() == 0) {
			return DefensePreview.illegal(type, "shield is not equipped");
		}
		if(type == DefenseType.DODGE && !character.canDodge()) {
			return DefensePreview.illegal(type, "dodge is disabled");
		}
		Map<Combatant, Integer> chances = type != DefenseType.DODGE? Map.of() : opponents.stream()
				.collect(Collectors.toMap(Function.identity(),
						c -> StatRules.checkChance(c.getCharacter().getMainMeleeCheck(), character.getDodgeCheck())));
		return new DefensePreview(true, "", type, checkGuardValue(type), chances);
	}

	void defend(DefenseType type) {
		spendAp(DEFEND_COST);
		defense = type;
	}

	int checkGuardValue(DefenseType defense) {
		return switch(defense) {
			case SIMPLE, SHIELD -> {
				yield StatRules.scaleToLevel(StatRules.SIMPLE_GUARD_PER_TIER, character.getLevel()) +
				(defense == DefenseType.SHIELD? character.getShieldGuard() : 0);
			}
			case null, default -> 0; //DODGE or null
		};
	}
}
