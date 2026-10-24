package com.endlessadventure.combat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

class BattleMap {
	private static final char PLAYER_MARKER = 'P';
	private static final char ENEMY_MARKER = 'E';
	private static final int BORDER_SPAWN_BUFFER = 1; //entities should not spawn in this range from the map border

	// {dx, dy}: orthogonal first, then diagonal; the fixed order makes equal-length paths come out the same every time
	private static final int[][] DIRECTIONS = {
			{0, -1}, {1, 0}, {0, 1}, {-1, 0},
			{1, -1}, {1, 1}, {-1, 1}, {-1, -1}};

	private final Terrain[][] map;
	private final Position playerSpawn;
	private final List<Position> enemySpawns;
	
	private BattleMap(Terrain[][] map, Position playerSpawn, List<Position> enemySpawns) {
		this.map = map;
		this.playerSpawn = playerSpawn;
		this.enemySpawns = List.copyOf(enemySpawns);
	}
	
	static BattleMap from(String... symbolMap) {
		if(symbolMap == null || symbolMap.length == 0 || symbolMap[0].isBlank()) {
			throw new IllegalArgumentException("Invalid symbol map: rows cannot be null or empty.");
		}
		
		Terrain[][] map = new Terrain[symbolMap.length][symbolMap[0].length()];
		Position player = null;
		List<Position> enemies = new ArrayList<>();
		
		for(int i=0; i<symbolMap.length; i++) {
			String row = symbolMap[i];
			if(row.length() != symbolMap[0].length() || row.isBlank()) {
				throw new IllegalArgumentException("Invalid symbol map: " + i + ": length must be consistent and non-empty.");
			}
			for(int j=0; j<row.length(); j++) {
				char token = row.charAt(j);
				boolean matched = false;
				
				for(Terrain t : Terrain.values()) {
					if(t.symbol == token) {
						map[i][j] = t;
						matched = true;
					}
				}
				if(token == PLAYER_MARKER || token == ENEMY_MARKER) {
					map[i][j] = Terrain.FLOOR;
					if(i < BORDER_SPAWN_BUFFER || i >= symbolMap.length - BORDER_SPAWN_BUFFER ||
							j < BORDER_SPAWN_BUFFER || j >= row.length() - BORDER_SPAWN_BUFFER) {
						throw new IllegalArgumentException("Invalid symbol map: spawn points cannot be close to the border");
					}
					if(token == PLAYER_MARKER) {
						if(player != null) { //TODO remove de-dup checks when player party is implemented
							throw new IllegalArgumentException("Invalid symbol map: duplicate player spawns.");
						}
						player = new Position(j, i);
						matched = true;
					}
					else{
						enemies.add(new Position(j, i));
						matched = true;
					}
				}
				else if(!matched) {
					throw new IllegalArgumentException("Invalid symbol map: unknown token: " + token);
				}
			}
		}
		if(player == null || enemies.isEmpty()) {
			throw new IllegalArgumentException("Invalid symbol map: there must be at least one player and one enemy spawn.");
		}
		return new BattleMap(map, player, enemies);
	}
	
	public int width() {
		return map[0].length;
	}
	
	public int height() {
		return map.length;
	}
	
	public boolean inBounds(Position p) {
		return p.x() >= 0 && p.y() >= 0 && p.x() <= width() - 1 && p.y() <= height() - 1;
	}
	
	public boolean blocks(Position p) {
		return !inBounds(p) || map[p.y()][p.x()].blocks;
	}
	
	public boolean isEdge(Position p) {
		return inBounds(p) && (p.x() == 0 || p.y() == 0 || p.x() == width() - 1 || p.y() == height() - 1);
	}
	
	public Position getPlayerSpawn() {
		return playerSpawn;
	}
	
	public List<Position> getEnemySpawns(){
		return List.copyOf(enemySpawns);
	}
	
	public boolean canStep(Position from, Position to) {
		return  from.distanceTo(to) == 1 && !blocks(to) && (from.x() != to.x() && from.y() != to.y()? 
				!blocks(new Position(to.x(), from.y())) && !blocks(new Position(from.x(), to.y())): true);
	}
	
	/** breadth-first search from start; occupied cells cannot be entered, but only terrain blocks corners */
	SearchedPaths searchPaths(Position start, Predicate<Position> occupied) {
		int[][] steps = new int[height()][width()];
		for (int[] row : steps) {
			Arrays.fill(row, -1);
		}
		Position[][] previous = new Position[height()][width()];

		Deque<Position> queue = new ArrayDeque<>();
		steps[start.y()][start.x()] = 0;
		queue.add(start);

		while (!queue.isEmpty()) {
			Position cur = queue.poll();
			for (int[] d : DIRECTIONS) {
				Position next = new Position(cur.x() + d[0], cur.y() + d[1]);
				//canStep first: it rules out cells off the map before the array is read
				if (!canStep(cur, next) || occupied.test(next) || steps[next.y()][next.x()] != -1) {
					continue;
				}
				steps[next.y()][next.x()] = steps[cur.y()][cur.x()] + 1;
				previous[next.y()][next.x()] = cur;
				queue.add(next);
			}
		}
		return new SearchedPaths(steps, previous);
	}
	
	private enum Terrain { 
		FLOOR(false, '.'),
		TREE(true, 'T'),
		WALL(true, '#'),
		WATER(true, '~');
		
		public final boolean blocks;
		public final char symbol;

		private Terrain(boolean blocks, char symbol) {
			this.blocks = blocks;
			this.symbol = symbol;
		}
	}
	
	/** the result of one search from a start cell: fewest steps to every cell (-1 if unreachable) and the cell each was reached from */
	record SearchedPaths(int[][] steps, Position[][] previous) {
		int stepsTo(Position p) {
			return steps[p.y()][p.x()];
		}

		/** the cells to walk through, excluding the start; empty if the target is unreachable or is the start */
		List<Position> pathTo(Position target) {
			List<Position> path = new ArrayList<>();
			if (stepsTo(target) <= 0) {
				return path;
			}
			//walk back from the target until the start, whose previous is null
			for (Position p = target; previous[p.y()][p.x()] != null; p = previous[p.y()][p.x()]) {
				path.add(p);
			}
			Collections.reverse(path);
			return path;
		}
	}
	
}
