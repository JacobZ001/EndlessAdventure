package com.endlessadventure.story;

import java.util.List;

public class CurrentScene extends Scene {
	public static final int MIN_OPTION_COUNT = 1;
	public static final int MAX_OPTION_COUNT = 3;
	private final String[] options;
	private final List<String> enemyIds;	//enemy templates present, one entry per enemy; empty when none

	public CurrentScene(String location, String description, int turn, String[] options) {
		this(location, description, turn, options, List.of());
	}

	/** enemyIds are the template ID of the enemies present in the scene, or empty when there is none */
	public CurrentScene(String location, String description, int turn, String[] options, List<String> enemyIds) {
		super(location, description, turn);
		if(options.length < MIN_OPTION_COUNT || options.length > MAX_OPTION_COUNT) {
			throw new IllegalArgumentException("Invalid option number: " + options.length
					+ ", should be between " + MIN_OPTION_COUNT + " and " + MAX_OPTION_COUNT);
		}
		for(String id : enemyIds) {
			if(id.isBlank()) {
				throw new IllegalArgumentException("Enemy id cannot be blank");
			}
		}
		this.options = options.clone();
		this.enemyIds = List.copyOf(enemyIds);
	}

	/** Return a copy so callers cannot change this scene's choices. */
	public String[] getOptions() {
		return options.clone();
	}

	/** the template ID of the enemies present in this scene, or empty */
	public List<String> getEnemyIds() {
		return enemyIds;
	}

	public PastScene toPastScene(String option) {
		return new PastScene(getLocation(), getDescription(), getTurn(), option);
	}
}
