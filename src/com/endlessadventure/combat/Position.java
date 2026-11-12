package com.endlessadventure.combat;

public record Position(int x, int y) implements Comparable<Position> {
	public int distanceTo(Position other) {
		return Math.max(Math.abs(other.x() - x), Math.abs(other.y() - y));
	}

	@Override
	public String toString() {
		return "(%d, %d)".formatted(x, y);
	}

	@Override
	/** compares positions from top to bottom, then left to right; consistent with drawing order */
	public int compareTo(Position o) {
		int byRow = Integer.compare(y, o.y);
		return byRow != 0 ? byRow : Integer.compare(x, o.x); 
	}
}