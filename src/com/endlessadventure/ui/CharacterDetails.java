package com.endlessadventure.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.endlessadventure.entity.Player;
import com.endlessadventure.entity.Skill;

/** the stats the character column leaves out: secondary values, progress and every general skill;
 *  drawn by the character screen and the last step of character creation, next to the character column */
final class CharacterDetails {
	private static final int COL_W = 26; //a general skill's name on the left, its value at the column's right end

	private CharacterDetails() {}

	static void draw(Canvas canvas, int x, int y, int width, int height, Player p) {
		canvas.drawTitledBox(x, y, width, height, Palette.MUTED, "DETAILS");
		int cx = x + 3;

		//secondary values: combat shows dodge and speed itself, and only armor changes them, so they stay out of the column
		int y2 = y + 2;
		canvas.drawText(cx, y2, "SECONDARY", Palette.TAG);
		
		y2 += 2;
		List<Map.Entry<String, String>> rows = List.of(
				Map.entry("Speed", String.valueOf(p.getSpeed())),
				Map.entry("Dodge", p.canDodge() ? "Enabled" : "Disabled"),
				Map.entry("Initiative", String.valueOf(p.getInitiative())));
		for (int i = 0; i < rows.size(); i++) {
			String value = rows.get(i).getValue();
			canvas.drawText(cx, y2 + i, rows.get(i).getKey(), Palette.TEXT);
			canvas.drawText(cx + COL_W - value.length(), y2 + i, value, Palette.TEXT);
		}

		//general skills in two columns, filled top to bottom; untrained ones muted
		int y3 = y2 + rows.size() + 2;
		canvas.drawText(cx, y3, "GENERAL SKILLS", Palette.TAG);
		
		List<Skill> general = Arrays.stream(Skill.values()).filter(s -> s.group == Skill.Group.GENERAL).toList();
		int perCol = (general.size() + 1) / 2;
		for (int i = 0; i < general.size(); i++) {
			Skill s = general.get(i);
			int sx = cx + (i / perCol) * (COL_W + 4), 
				sy = y3 + 2 + i % perCol;
			String value = String.valueOf(p.getSkill(s));
			int color = p.getSkill(s) > 0 ? Palette.TEXT : Palette.MUTED;
			canvas.drawText(sx, sy, s.displayName, color);
			canvas.drawText(sx + COL_W - value.length(), sy, value, color);
		}
	}
}
