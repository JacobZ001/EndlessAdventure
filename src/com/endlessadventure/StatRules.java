package com.endlessadventure;

import com.endlessadventure.entity.Skill;

/** contains the tunable numbers and growth formulas
 *  values "per tier" grow with the growth unit n = (7 + level) / 8, which is 1 at level 1 and +1 every tier */
public final class StatRules {
	public static final int MAX_LEVEL = 20; //temporary design, might change in future development
	public static final int LEVELS_PER_TIER = 8;

	// a typical specialist gains this much per tier; one tier's worth is also the starting cap
	public static final int ATTRIBUTE_PER_TIER = 4;
	public static final int SKILL_PER_TIER = 8;
	// skill caps never drop below the human limit tier (T3)
	public static final int HUMAN_LIMIT_TIER = 3;

	// character creation
	public static final int ATTRIBUTE_START_BUDGET = 8;
	public static final int ATTRIBUTE_START_CAP = ATTRIBUTE_PER_TIER;
	public static final int SKILL_START_CAP = SKILL_PER_TIER;
	public static final int COMBAT_SKILL_START_BUDGET = 8;
	public static final int GENERAL_SKILL_START_BUDGET = 12;

	// level-up growth
	public static final int LEVELS_PER_ATTRIBUTE_POINT = 2; // points at levels 3, 5, 7...
	public static final int COMBAT_SKILL_PER_LEVEL = 1;
	public static final int GENERAL_SKILL_PER_LEVEL = 2;

	// derived stats
	public static final int PLAYER_HP_PER_TIER = 144; // MaxHP = 18 * (7 + Level) + 8 * CON; or +18 per Level
	public static final int PLAYER_HP_PER_CON = 8;
	
	//other stats
	public static final int BASE_SPEED = 4;
	public static final int SIMPLE_GUARD_PER_TIER = 3;
	
	//index = current level - 1; value = amount of exp required for leveling up
	private static final int[] XP_TO_NEXT = { 
			48, 64, 80, 96,				//1-4
			196, 224, 252, 280, 308,	//5-9
			480, 520, 560, 600, 640,	//10-14
			816, 864, 912, 960, 1008	//15-19
	};

	private StatRules() {}

	/** returns the value at the selected level of something worth perTierValue per tier, i.e. perTierValue * n rounded half up */
	public static int scaleToLevel(int perTierValue, int level) {
		return (perTierValue * (LEVELS_PER_TIER - 1 + level) + LEVELS_PER_TIER / 2) / LEVELS_PER_TIER;
	}

	/** returns the budget limit of attribute points available at the selected level */
	public static int getAttributeBudgetLimit(int level) {
		return ATTRIBUTE_START_BUDGET + (level - 1) / LEVELS_PER_ATTRIBUTE_POINT;
	}

	/** returns the highest value a single attribute can reach at the selected level */
	public static int getAttributeCap(int level) {
		return ATTRIBUTE_START_CAP + (level - 1) / LEVELS_PER_ATTRIBUTE_POINT;
	}

	/** returns the budget limit of skill points in the selected group at the selected level */
	public static int getSkillBudgetLimit(Skill.Group group, int level) {
		return switch (group) {
			case COMBAT -> COMBAT_SKILL_START_BUDGET + (level - 1) * COMBAT_SKILL_PER_LEVEL;
			case GENERAL -> GENERAL_SKILL_START_BUDGET + (level - 1) * GENERAL_SKILL_PER_LEVEL;
		};
	}

	/** returns the single skill cap: one tier's skill points per tier of level,
	 *  but no more than one tier's skill points per tier of the highest attribute (never below the human limit) */
	public static int getSkillCap(int level, int highestAttribute) {
		int attributeTier = Math.max(HUMAN_LIMIT_TIER, Math.ceilDiv(highestAttribute, ATTRIBUTE_PER_TIER));
		return Math.min(scaleToLevel(SKILL_PER_TIER, level), SKILL_PER_TIER * attributeTier);
	}

	/** returns the player's max HP at the selected level and CON */
	public static int getPlayerMaxHp(int level, int con) {
		return scaleToLevel(PLAYER_HP_PER_TIER, level) + PLAYER_HP_PER_CON * con;
	}

	/** returns the amount of exp required for the selected level to level-up */
	public static int getXpToNext(int level) {
		if (level < 1 || level >= MAX_LEVEL) return Integer.MAX_VALUE;
		return XP_TO_NEXT[level - 1];
	}
	
	/** percent chance that value beats difficulty: 75% plus 5% per point of difference, kept within 0–100 */
	public static int checkChance(int value, int difficulty) {
		return Math.max(0, Math.min(100, 75 + 5 * (value - difficulty)));
	}
}
