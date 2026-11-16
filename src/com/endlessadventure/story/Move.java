package com.endlessadventure.story;

import com.endlessadventure.WorldMap.LocationKind;

/** where a story reply takes the player. StoryGenerator only checks it against the world map;
 *  GameEngine applies it, so a rejected reply never changes the map. Developed with GenAI assistance. */
public sealed interface Move {

	/** the player stays in the current room */
	record Stay() implements Move {}

	/** the player goes to a known room connected to the current one */
	record ToRoom(String roomId) implements Move {}

	/** the player finds a new room in the current location, reached from the current room */
	record NewRoom(String roomName, String backdrop) implements Move {}

	/** the player arrives at a new location, in its first room */
	record NewLocation(String name, LocationKind kind, String theme, int danger,
			String roomName, String backdrop) implements Move {}
}
