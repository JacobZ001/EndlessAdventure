package com.endlessadventure.story;

public class CurrentScene extends Scene {
	public static final int MIN_OPTION_COUNT = 1;
	public static final int MAX_OPTION_COUNT = 3;
	private final String[] options;
	private final String enemyId;	//enemy template present in this scene, or null

	public CurrentScene(String location, String description, int turn, String[] options) {
		this(location, description, turn, options, null);
	}

	/** enemyId is the template ID of an enemy present in the scene, or null when there is none */
	public CurrentScene(String location, String description, int turn, String[] options, String enemyId) {
		super(location, description, turn);
		if(options.length < MIN_OPTION_COUNT || options.length > MAX_OPTION_COUNT) {
			throw new IllegalArgumentException("Invalid option number: " + options.length
					+ ", should be between " + MIN_OPTION_COUNT + " and " + MAX_OPTION_COUNT);
		}
		if(enemyId != null && enemyId.isBlank()) {
			throw new IllegalArgumentException("Enemy id cannot be blank");
		}
		this.options = options.clone();
		this.enemyId = enemyId;
	}

	/** Return a copy so callers cannot change this scene's choices. */
	public String[] getOptions() {
		return options.clone();
	}

	/** the template ID of the enemy present in this scene, or null */
	public String getEnemyId() {
		return enemyId;
	}

	public SceneRecord toRecord(String option) {
		return new SceneRecord(getLocation(), getDescription(), getTurn(), option);
	}
}
