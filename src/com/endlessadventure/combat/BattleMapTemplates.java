package com.endlessadventure.combat;

class BattleMapTemplates {
	static final BattleMap FAR = BattleMap.from(
"...T......",
		    ".......T..",
		    ".P......E.",
		    "....T.....",
		    "..T....TE.",
		    "......T...",
		    ".T.......T");

	static final BattleMap NEAR = BattleMap.from(
".T....T..T",
		    "..T...T...",
		    "..P....E..",
		    "......E.T.",
		    "..TT......",
		    "........T.",
		    "T...T.....");
}
