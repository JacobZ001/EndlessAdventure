package com.endlessadventure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorldMap {
	public static final int MAX_LOCATION_LENGTH = 30;
	public static final int MAX_ROOM_LENGTH = MAX_LOCATION_LENGTH;
	public static final int MAX_THEME_LENGTH = 120;

	private final Map<String, Location> locations = new LinkedHashMap<>();
	private final Map<String, Room> rooms = new LinkedHashMap<>();
	private final Map<String, Set<String>> connections = new LinkedHashMap<>();	//both directions of every pair

	/** an empty map, before the opening */
	public WorldMap() {}

	/** rebuild a saved map through the same writers the game uses, so every check runs again;
	 *  ids must run L1, L2, ... and R1, R2, ... as the writers hand them out, or later ids would collide */
	public WorldMap(List<Location> locations, List<Room> rooms, List<Connection> connections) {
		for (Location l : locations) {
			if (!addLocation(l.name(), l.kind(), l.theme(), l.danger()).id().equals(l.id())) {
				throw new IllegalArgumentException("Location ids must run L1, L2, ...: " + l.id());
			}
		}
		for (Room r : rooms) {
			if (!addRoom(r.locationId(), r.name(), r.backdrop()).id().equals(r.id())) {
				throw new IllegalArgumentException("Room ids must run R1, R2, ...: " + r.id());
			}
		}
		for (Connection c : connections) {
			connect(c.a(), c.b());
		}
	}

	public List<Location> getLocations() {
		return List.copyOf(locations.values());
	}

	public List<Room> getRooms() {
		return List.copyOf(rooms.values());
	}

	/** every connection once, with the smaller id first */
	public List<Connection> getConnections() {
		List<Connection> list = new ArrayList<>();
		connections.forEach((a, others) -> others.stream().filter(b -> a.compareTo(b) < 0)
				.forEach(b -> list.add(new Connection(a, b))));
		return list;
	}
	
	/** returns the location mapped with id, or null if none */
	public Location getLocation(String id) {
		return locations.get(id);
	}
	
	/** returns the room mapped with id, or null if none */
	public Room getRoom(String id) {
		return rooms.get(id);
	}
	
	public List<Room> getNeighbors(String roomId){
		return connections.getOrDefault(roomId, Set.of()).stream().map(rooms::get).toList();
	}

	Location addLocation(String name, LocationKind kind, String theme, int danger){
		String id = "L" + (locations.size() + 1);
		Location l = new Location(id, name, kind, theme, danger);
		locations.put(id, l);
		return l;
	}
	
	Room addRoom(String locationId, String name, String backdrop) {
		if(!locations.containsKey(locationId)) {
			throw new IllegalArgumentException("Invalid room: location ID (" + locationId + ") does not exist.");
		}
		String id = "R" + (rooms.size() + 1);
		Room r = new Room(id, locationId, name, backdrop);
		rooms.put(id, r);
		return r;
	}
	
	void connect(String a, String b){
		if(a.equals(b)) {
			throw new IllegalArgumentException("Invalid connection: room must not connect to itself.");
		}
		String n = null;
		if(!rooms.containsKey(a)) {n = a;}
		else if(!rooms.containsKey(b)) {n = b;}
		if(n != null) {
			throw new IllegalArgumentException("Invalid connection: room ID (" + n + ") does not exist.");
		}
		connections.computeIfAbsent(a, k -> new LinkedHashSet<>()).add(b);
		connections.computeIfAbsent(b, k -> new LinkedHashSet<>()).add(a);
	}
	
	public enum LocationKind {
		SETTLEMENT(1.0, 0.0),
		WILDERNESS(0.5, 0.2),
		RUIN(0.5, 0.4);
		
		public final double restRecoveryRatio;
		public final double restInterruptChance;
		
		LocationKind(double restRecoveryRatio, double restInterruptChance) {
			this.restRecoveryRatio = restRecoveryRatio;
			this.restInterruptChance = restInterruptChance;
		}
	}
	
	public record Location(String id, String name, LocationKind kind, String theme, int danger) {
		private static final String ERROR_LABEL = "Invalid location: "; 
		public Location {
			if(id == null || id.isBlank()) {throw new IllegalArgumentException(ERROR_LABEL + "id cannot be null or empty.");}
			if(name == null || name.isBlank() ) {throw new IllegalArgumentException(ERROR_LABEL + "name cannot be null or empty.");}
			if(name.length() > MAX_LOCATION_LENGTH) {throw new IllegalArgumentException(ERROR_LABEL + "maximum location name length exceeded.");}
			if(kind == null) {throw new IllegalArgumentException(ERROR_LABEL + "kind cannot be null or empty.");}
			if(theme == null || theme.isBlank()) {throw new IllegalArgumentException(ERROR_LABEL + "theme cannot be null or empty.");}
			if(theme.length() > MAX_THEME_LENGTH) {throw new IllegalArgumentException(ERROR_LABEL + "maximum theme length exceeded.");}
			if(danger < 1 || danger > StatRules.MAX_LEVEL) {
				throw new IllegalArgumentException(ERROR_LABEL + "danger level must be between 1 and " + StatRules.MAX_LEVEL);
			}
		}
	}
	
	/** two rooms connected both ways */
	public record Connection(String a, String b) {}

	public record Room(String id, String locationId, String name, String backdrop) {
		private static final String ERROR_LABEL = "Invalid room: "; 
		public Room{
			if(id == null || id.isBlank()) {throw new IllegalArgumentException(ERROR_LABEL + "id cannot be null or empty.");}
			if(locationId == null || locationId.isBlank()) {throw new IllegalArgumentException(ERROR_LABEL + "locationId cannot be null or empty.");}
			if(name == null || name.isBlank()) {throw new IllegalArgumentException(ERROR_LABEL + "name cannot be null or empty.");}
			if(name.length() > MAX_ROOM_LENGTH) {throw new IllegalArgumentException(ERROR_LABEL + "maximum room name length exceeded.");}
			if(backdrop == null || backdrop.isBlank()) {throw new IllegalArgumentException(ERROR_LABEL + "backdrop cannot be empty.");}
		}
	}
}