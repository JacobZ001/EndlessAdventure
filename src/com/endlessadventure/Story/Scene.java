package com.endlessadventure.Story;

import java.util.Objects;

/** Current adventure text and choices. Developed with GenAI assistance. */
public class Scene {
	private final String location;
	private final String description;
	private final String[] options;

	public Scene() {
		this("", "", new String[0]);
	}

	public Scene(String location, String description, String[] options) {
		this.location = Objects.requireNonNull(location);
		this.description = Objects.requireNonNull(description);
		this.options = options.clone();
	}

	public String getLocation() {
		return location;
	}

	public String getDescription() {
		return description;
	}

	/** Return a copy so callers cannot change this scene's choices. */
	public String[] getOptions() {
		return options.clone();
	}
}
