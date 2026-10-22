package com.endlessadventure.ui;

import java.util.List;

import com.endlessadventure.Displayable.Detail;
import com.endlessadventure.Displayable.Span;

/** draws Displayable details top-down: the model says what each part means, this class picks layout and colors */
final class DetailView {
	private DetailView() {}

	/** draws the details from x, y within width columns; returns the row after the last one drawn.
	 *  a blank row separates groups of different kinds, except under a Line, which heads what follows;
	 *  a Description sets itself apart under a divider */
	static int draw(Canvas canvas, int x, int y, int width, List<Detail> details) {
		Detail previous = null;
		for (Detail d : details) {
			if (previous != null && !(previous instanceof Detail.Line) && !(d instanceof Detail.Description)
					&& d.getClass() != previous.getClass()) {
				y ++;
			}
			y = switch (d) {
				case Detail.Line l -> drawSpans(canvas, x, y, width, List.of(l.span()));
				case Detail.Tags t -> {
					int tx = x;
					for (Span s : t.tags()) {
						canvas.drawText(tx, y, s.text(), Palette.colorOf(s.meaning()));
						tx += s.text().length() + 2;
					}
					yield y + 1;
				}
				case Detail.Stat s -> {
					canvas.drawText(x, y, s.label(), Palette.MUTED);
					yield drawSpans(canvas, x + s.label().length() + 2, y, width - s.label().length() - 2, List.of(s.value()));
				}
				case Detail.Trait t -> {
					canvas.drawText(x, y, t.name().text(), Palette.colorOf(t.name().meaning()));
					yield drawSpans(canvas, x, y + 1, width, t.description());
				}
				case Detail.Description des -> { //empty row, divider, empty row, then the text
					canvas.drawText(x, y + 1, "─".repeat(width), Palette.LINE);
					yield canvas.drawParagraph(x, y + 3, width, des.span().text(), Palette.colorOf(des.span().meaning()));
				}
			};
			previous = d;
		}
		return y;
	}

	/** rows the details take at this width, counted by drawing them on a scratch canvas,
	 *  so measuring always follows the same layout rules as drawing */
	static int height(int width, List<Detail> details) {
		return draw(new Canvas(), 0, 0, width, details);
	}

	/** the spans as one paragraph, wrapped at spaces within width; returns the row after it */
	static int drawSpans(Canvas canvas, int x, int y, int width, List<Span> spans) {
		int col = 0;
		for (Span s : spans) {
			int color = Palette.colorOf(s.meaning());
			for (String word : s.text().split("(?<= )")) { //split after each space, so every word keeps its space
				if (col > 0 && col + word.stripTrailing().length() > width) {
					y++;
					col = 0;
				}
				canvas.drawText(x + col, y, word, color);
				col += word.length();
			}
		}
		return y + 1;
	}
}