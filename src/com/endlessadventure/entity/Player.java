package com.endlessadventure.entity;

import java.util.EnumMap;
import java.util.Map;

import com.endlessadventure.StatRules;

public class Player extends GameCharacter {
	
	private int exp;

	/** constructor for loading a saved player; attributes and skills may not exceed what the level allows
	 *  @throws IllegalArgumentException */
	public Player(String name, int level, int hp, int exp, Map<Attribute, Integer> attributes, Map<Skill, Integer> skills) {
		super(name, level, attributes, skills);
		checkGrowthLimits(level, attributes, skills);
		setExp(exp);
		setHp(hp);
	}

	/** constructor for a new player, see PlayerBuilder; starts at full HP */
	private Player(PlayerBuilder b) {
		this(b.name, 1, 0, 0, b.attributes, b.skills);
		setHp(getMaxHp());
	}

	@Override
	public int getMaxHp() {
		return StatRules.getPlayerMaxHp(getLevel(), getAttribute(Attribute.CON));
	}

	public int getExp() {
		return exp;
	}

	/** Set valid within-level progress, for example when loading a save. */
	public void setExp(int exp) {
		if(exp < 0 || (getLevel() == StatRules.MAX_LEVEL && exp != 0) || (getLevel() < StatRules.MAX_LEVEL && exp >= StatRules.getXpToNext(getLevel()))) {
			throw new IllegalArgumentException("Invalid exp for level: Lv " + getLevel() + ": " + exp);
		}
		this.exp = exp;
	}

	/** direct level change, does not leave incompatible EXP behind. */
	@Override
	public void setLevel(int level) {
		if ((level == StatRules.MAX_LEVEL && exp != 0) || (level < StatRules.MAX_LEVEL && exp >= StatRules.getXpToNext(level))) {
			throw new IllegalArgumentException("Current EXP is invalid for level: " + level);
		}
		super.setLevel(level);
	}

	/** returns the total amount of skill points of the argument skill EnumMap that is within the selected group */
	private static int getSkillTotal(Map<Skill, Integer> skills, Skill.Group group) {
		return skills.entrySet()
				.stream()
				.filter(e -> e.getKey().group == group)
				.mapToInt(Map.Entry::getValue)
				.sum();
	}

	/**checks if the argument attributes and skills are valid according to the caps and budgets values
	 * checks upper limits only, so a loaded player may still have unspent points
	 *  @throws IllegalArgumentException */
	private static void checkGrowthLimits(int level, Map<Attribute, Integer> attributes, Map<Skill, Integer> skills) {
		int attributeTotal = 0;
		int highest = 0;
		for (int v : attributes.values()) {
			if (v > StatRules.getAttributeCap(level)) {
				throw new IllegalArgumentException("Attribute too high for Lv " + level + ": " + v);
			}
			attributeTotal += v;
			highest = Math.max(highest, v);
		}
		if (attributeTotal > StatRules.getAttributeBudgetLimit(level)) {
			throw new IllegalArgumentException("Too many attribute points for Lv " + level + ": " + attributeTotal);
		}

		int skillCap = StatRules.getSkillCap(level, highest);
		for (int v : skills.values()) {
			if (v > skillCap) {
				throw new IllegalArgumentException("Skill too high for Lv " + level + ": " + v);
			}
		}
		for (Skill.Group g : Skill.Group.values()) {
			if (getSkillTotal(skills, g) > StatRules.getSkillBudgetLimit(g, level)) {
				throw new IllegalArgumentException("Too many " + g + " skill points for Lv " + level);
			}
		}
	}
	
	@Override
	public String getDescription() {
		return getName() + " the adventurer";
	}

	/** creates a new level 1 player; build() requires every attribute and skill point to be spent */
	public static class PlayerBuilder {
		private final String name;
		private final EnumMap<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
		private final EnumMap<Skill, Integer> skills = new EnumMap<>(Skill.class);

		/** @throws IllegalArgumentException */
		public PlayerBuilder(String name) {
			this.name = checkName(name);
		}
		
		public String getName() {
			return name;
		}

		public int getAttribute(Attribute a) {
			return attributes.getOrDefault(a, 0);
		}

		public int getSkill(Skill s) {
			return skills.getOrDefault(s, 0);
		}

		public int getRemainingAttributePoints() {
			return StatRules.getAttributeBudgetLimit(1) - attributes.values()
					.stream()
					.mapToInt(Integer::intValue)
					.sum();
		}

		public int getRemainingSkillPoints(Skill.Group group) {
			return StatRules.getSkillBudgetLimit(group, 1) - getSkillTotal(skills, group);
		}

		/** @throws IllegalArgumentException */
		public PlayerBuilder incrAttribute(Attribute a) {
			if (getRemainingAttributePoints() < 1) {
				throw new IllegalArgumentException("No attribute points left");
			}
			if (getAttribute(a) >= StatRules.ATTRIBUTE_START_CAP) {
				throw new IllegalArgumentException(a.displayName + " is already at the starting cap of " + StatRules.ATTRIBUTE_START_CAP);
			}
			attributes.put(a, getAttribute(a) + 1);
			return this;
		}

		/** @throws IllegalArgumentException */
		public PlayerBuilder decrAttribute(Attribute a) {
			if (getAttribute(a) < 1) {
				throw new IllegalArgumentException(a.displayName + " has no points to remove");
			}
			attributes.put(a, getAttribute(a) - 1);
			return this;
		}

		/** @throws IllegalArgumentException */
		public PlayerBuilder incrSkill(Skill s) {
			if (getRemainingSkillPoints(s.group) < 1) {
				throw new IllegalArgumentException("No " + s.group + " skill points left");
			}
			if (getSkill(s) >= StatRules.SKILL_START_CAP) {
				throw new IllegalArgumentException(s + " is already at the starting cap of " + StatRules.SKILL_START_CAP);
			}
			skills.put(s, getSkill(s) + 1);
			return this;
		}

		/** @throws IllegalArgumentException */
		public PlayerBuilder decrSkill(Skill s) {
			if (getSkill(s) < 1) {
				throw new IllegalArgumentException(s + " has no points to remove");
			}
			skills.put(s, getSkill(s) - 1);
			return this;
		}

		/** @throws IllegalStateException if any point is unspent */
		public Player build() {
			for (Skill.Group g : Skill.Group.values()) {
				if (getRemainingSkillPoints(g) != 0) {
					throw new IllegalStateException(getRemainingSkillPoints(g) + " " + g + " skill points left");
				}
			}
			if (getRemainingAttributePoints() != 0) {
				throw new IllegalStateException(getRemainingAttributePoints() + " attribute points left");
			}
			return new Player(this);
		}
	}
}
