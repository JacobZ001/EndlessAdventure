package com.endlessadventure.combat;

public record Position(int x, int y) {
	public int distanceTo(Position other) {
		return Math.max(Math.abs(other.x() - x), Math.abs(other.y() - y));
	}
}