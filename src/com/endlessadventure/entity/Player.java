package com.endlessadventure.entity;

import java.util.EnumMap;
import java.util.Map;

import com.endlessadventure.inventory.Inventory;

public class Player extends Combatant {
	private static final int[] XP_TO_NEXT = { //index = current level; value = amount of exp required for leveling up
			48, 64, 80, 96,				//1-4
			196, 224, 252, 280, 308,	//5-9
			480, 520, 560, 600, 640,	//10-14
			816, 864, 912, 960, 1008	//15-19
	};
	private static final int ATTRIBUTE_BUDGET = 8;
	private static final int ATTRIBUTE_START_CAP = 4;
	private static final int COMBAT_BUDGET = 8;
	private static final int GENERAL_BUDGET = 12;
	private static final int TRAINING_START_CAP = 8;
	
	private final Inventory inventory = new Inventory();
	
	private int exp;

	/** constructor for loading a saved player; attributes and trainings may not exceed what the level allows
	 *  @throws IllegalArgumentException */
	public Player(String name, int level, int hp, int exp, Map<Attribute, Integer> attributes, Map<Training, Integer> trainings) {
		super(name, level);
		checkGrowthLimits(level, attributes, trainings);
		attributes.forEach(this::setAttribute);
		trainings.forEach(this::setTraining);
		setExp(exp);
		setHp(hp);
	}

	/** constructor for a new player, see Builder; starts at full HP */
	private Player(Builder b) {
		this(b.name, 1, 0, 0, b.attributes, b.trainings);
		setHp(getMaxHp());
	}

	@Override
	public int getMaxHp() {
		return 18 * (7 + getLevel()) + 8 * getAttribute(Attribute.CON);
	}

	/** returns the amount of exp required for the selected level to level-up */
	public static int xpToNext(int level) {
		if (level < 1 || level >= MAX_LEVEL) return Integer.MAX_VALUE;
		return XP_TO_NEXT[level-1];
	}

	public int getExp() {
		return exp;
	}

	/** Set valid within-level progress, for example when loading a save. */
	public void setExp(int exp) {
		if(exp < 0 || (getLevel() == MAX_LEVEL && exp != 0) || (getLevel() < MAX_LEVEL && exp >= xpToNext(getLevel()))) {
			throw new IllegalArgumentException("Invalid exp for level: Lv " + getLevel() + ": " + exp);
		}
		this.exp = exp;
	}

	/** direct level change, does not leave incompatible EXP behind. */
	@Override
	public void setLevel(int level) {
		if ((level == MAX_LEVEL && exp != 0) || (level < MAX_LEVEL && exp >= xpToNext(level))) {
			throw new IllegalArgumentException("Current EXP is invalid for level: " + level);
		}
		super.setLevel(level);
	}

	public Inventory getInventory() {
		return inventory;
	}

	/** returns the training point budget for selected level and group
	 *  combat trainings get 1 point and general trainings 2 points per level */
	private static int trainingBudget(Training.Group group, int level) {
		return group == Training.Group.COMBAT ? COMBAT_BUDGET + (level - 1) : GENERAL_BUDGET + 2 * (level - 1);
	}

	/** returns the total amount of training points of the input training EnumMap that is within the selected group */
	private static int trainingTotal(Map<Training, Integer> trainings, Training.Group group) {
		int total = 0;
		for (Map.Entry<Training, Integer> e : trainings.entrySet()) {
			if (e.getKey().getGroup() == group) {
				total += e.getValue();
			}
		}
		return total;
	}

	/**checks if the input attributes and trainings are valid according to the caps and budgets values
	 * checks upper limits only, so a loaded player may still have unspent points
	 *  @throws IllegalArgumentException */
	private static void checkGrowthLimits(int level, Map<Attribute, Integer> attributes, Map<Training, Integer> trainings) {
		int attributePoints = (level - 1) / 2; // granted at levels 3, 5, 7...
		int attributeTotal = 0;
		int highest = 0;
		for (int v : attributes.values()) {
			if (v > ATTRIBUTE_START_CAP + attributePoints) {
				throw new IllegalArgumentException("Attribute too high for Lv " + level + ": " + v);
			}
			attributeTotal += v;
			highest = Math.max(highest, v);
		}
		if (attributeTotal > ATTRIBUTE_BUDGET + attributePoints) {
			throw new IllegalArgumentException("Too many attribute points for Lv " + level + ": " + attributeTotal);
		}

		// single training cap = min(7 + level, 8 * tier of the highest attribute), tier at least 3
		int trainingCap = Math.min(7 + level, 8 * Math.max(3, (highest + 3) / 4));
		for (int v : trainings.values()) {
			if (v > trainingCap) {
				throw new IllegalArgumentException("Training too high for Lv " + level + ": " + v);
			}
		}
		for (Training.Group g : Training.Group.values()) {
			if (trainingTotal(trainings, g) > trainingBudget(g, level)) {
				throw new IllegalArgumentException("Too many " + g + " training points for Lv " + level);
			}
		}
	}

	/** creates a new level 1 player; build() requires every attribute and training point to be spent */
	public static class Builder {
		private final String name;
		private final EnumMap<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
		private final EnumMap<Training, Integer> trainings = new EnumMap<>(Training.class);

		/** @throws IllegalArgumentException */
		public Builder(String name) {
			this.name = checkName(name);
		}

		public int getAttribute(Attribute a) {
			return attributes.getOrDefault(a, 0);
		}

		public int getTraining(Training t) {
			return trainings.getOrDefault(t, 0);
		}

		public int remainingAttributePoints() {
			int total = 0;
			for (int v : attributes.values()) {
				total += v;
			}
			return ATTRIBUTE_BUDGET - total;
		}

		public int remainingTrainingPoints(Training.Group group) {
			return trainingBudget(group, 1) - trainingTotal(trainings, group);
		}

		/** @throws IllegalArgumentException */
		public Builder addAttribute(Attribute a) {
			if (remainingAttributePoints() < 1) {
				throw new IllegalArgumentException("No attribute points left");
			}
			if (getAttribute(a) >= ATTRIBUTE_START_CAP) {
				throw new IllegalArgumentException(a.getDisplayName() + " is already at the starting cap of " + ATTRIBUTE_START_CAP);
			}
			attributes.put(a, getAttribute(a) + 1);
			return this;
		}

		/** @throws IllegalArgumentException */
		public Builder removeAttribute(Attribute a) {
			if (getAttribute(a) < 1) {
				throw new IllegalArgumentException(a.getDisplayName() + " has no points to remove");
			}
			attributes.put(a, getAttribute(a) - 1);
			return this;
		}

		/** @throws IllegalArgumentException */
		public Builder addTraining(Training t) {
			if (remainingTrainingPoints(t.getGroup()) < 1) {
				throw new IllegalArgumentException("No " + t.getGroup() + " training points left");
			}
			if (getTraining(t) >= TRAINING_START_CAP) {
				throw new IllegalArgumentException(t + " is already at the starting cap of " + TRAINING_START_CAP);
			}
			trainings.put(t, getTraining(t) + 1);
			return this;
		}

		/** @throws IllegalArgumentException */
		public Builder removeTraining(Training t) {
			if (getTraining(t) < 1) {
				throw new IllegalArgumentException(t + " has no points to remove");
			}
			trainings.put(t, getTraining(t) - 1);
			return this;
		}

		/** @throws IllegalStateException if any point is unspent */
		public Player build() {
			for (Training.Group g : Training.Group.values()) {
				if (remainingTrainingPoints(g) != 0) {
					throw new IllegalStateException(remainingTrainingPoints(g) + " " + g + " training points left");
				}
			}
			if (remainingAttributePoints() != 0) {
				throw new IllegalStateException(remainingAttributePoints() + " attribute points left");
			}
			return new Player(this);
		}
	}
}
