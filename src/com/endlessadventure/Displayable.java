package com.endlessadventure;

import java.util.List;

/** implemented by things the screens can show: a name plus structured details; the ui decides layout and colors */
public interface Displayable {
	/** a piece of text and what it means; the ui interprets the meaning and assign a color for the text */
	public record Span(String text, Enum<?> meaning) {
		/** static factory for converting object values into Span */ 
		public static Span of(Object value, Enum<?> meaning) {
			return new Span(String.valueOf(value), meaning);
		}
		/** helper method for default meaning(plain text) */
		public static Span plain(Object value) {
			return of(value, Tone.PLAIN);
		}
	}

	/** one part of a details panel; the UI decides how each kind is laid out */
	public sealed interface Detail {
		/** a whole line that is a single span, e.g. the name or a one-line note */
		record Line(Span span) implements Detail {
			public static Line of(Object value, Enum<?> meaning) {
				return new Line(Span.of(value, meaning));
			}
		}

		/** short labels shown together on one line, e.g. "Rare Longsword", "Versatile"; the ui adds the separators */
		record Tags(List<Span> tags) implements Detail {
			public Tags {
				tags = List.copyOf(tags);
			}
		}

		/** a labeled value, e.g. "Attack Power" -> 6 (8) */
		record Stat(String label, Span value) implements Detail {
			public static Stat of(String label, Object value) {
				return new Stat(label, Span.plain(value));
			}

			public static Stat of(String label, Object value, Enum<?> meaning) {
				return new Stat(label, Span.of(value, meaning));
			}
		}

		/** a named rule with its description, e.g. "Pierce" -> "Reduces target armor by 4" */
		record Trait(Span name, List<Span> description) implements Detail {
			public Trait {
				description = List.copyOf(description);
			}
		}
	}

	/** what a span means; the ui picks the color */
	public enum Tone {
		PLAIN, NOTE, KEYWORD, GOOD, BAD,
		TIER_1, TIER_2, TIER_3, TIER_4, TIER_5;

		/** tier color for anything tiered; tiers beyond the last share its color */
		public static Tone getToneByTier(int tier) {
			return switch (tier) {
				case 1 -> TIER_1;
				case 2 -> TIER_2;
				case 3 -> TIER_3;
				case 4 -> TIER_4;
				case 5 -> TIER_5;
				default -> {
					if (tier < 1) {
						throw new IllegalArgumentException("Illegal tier: " + tier);
					}
					yield TIER_5;
				}
			};
		}
	}

	String getName();

	String getDescription();

	List<Detail> getDetails();

	/** what the name means wherever it is shown alone, e.g. an item's quality color in a list or a log line */
	default Enum<?> getNameMeaning() {
		return Tone.PLAIN;
	}
}
