package com.endlessadventure;

import com.endlessadventure.WorldMap.LocationKind;
import com.endlessadventure.WorldMap.Room;

/** offline check helper: builds a world map through the package-private writers GameEngine uses.
 *  Developed with GenAI assistance. */
public final class WorldFixture {
	private WorldFixture() {}

	/** add a location with its first room and put the player there */
	public static Room enter(GameState state, String location, LocationKind kind, String room, String backdrop) {
		WorldMap map = state.getWorldMap();
		Room r = map.addRoom(map.addLocation(location, kind, "A test theme.", 1).id(), room, backdrop);
		state.moveTo(r.id());
		return r;
	}

	/** add a room to the current location, connected to the current room; the player stays */
	public static Room addExit(GameState state, String room, String backdrop) {
		Room here = state.getCurrentRoom();
		Room r = state.getWorldMap().addRoom(here.locationId(), room, backdrop);
		state.getWorldMap().connect(here.id(), r.id());
		return r;
	}
}
