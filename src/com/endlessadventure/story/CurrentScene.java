package com.endlessadventure.story;

public class CurrentScene extends Scene {
	public static final int MIN_OPTION_COUNT = 1;
	public static final int MAX_OPTION_COUNT = 3;
	private final String[] options;

	public CurrentScene(String location, String description, int turn, String[] options) {
		super(location, description, turn);
		if(options.length < MIN_OPTION_COUNT || options.length > MAX_OPTION_COUNT) {
			throw new IllegalArgumentException("Invalid option number: " + options.length
					+ ", should be between " + MIN_OPTION_COUNT + " and " + MAX_OPTION_COUNT);
		}
		this.options = options.clone();
	}

	/** Return a copy so callers cannot change this scene's choices. */
	public String[] getOptions() {
		return options.clone();
	}
	
	public SceneRecord toRecord(String option) {
		return new SceneRecord(location, description, turn, option);
	}
}
