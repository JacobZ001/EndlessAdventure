package com.endlessadventure.entity;

public enum Skill{
	MELEE(Group.COMBAT, Attribute.STR, "Melee",
			"Close-range weapons and unarmed fighting. The attribute that scales an attack is set by the weapon or special action."),
	
	RANGED(Group.COMBAT, Attribute.DEX, "Ranged",
			"Shooting and throwing weapons, including non-combat targets such as cutting a rope. Simply hurling a heavy object is not weapon use."),
	
	SPELLCASTING(Group.COMBAT, Attribute.INT, "Spellcasting",
			"Casting magic you have already learned; the scaling attribute is set by each ability. Does not grant spells or all magical knowledge."),
	

	ATHLETICS(Group.GENERAL, Attribute.STR, "Athletics",
			"Climbing, swimming, carrying, pushing and pulling: sustained or forceful physical effort."),

	ACROBATICS(Group.GENERAL, Attribute.DEX, "Acrobatics",
			"Balance, vaulting, landing and whole-body coordination; separate from fine hand work."),
	
	SLEIGHT_OF_HAND(Group.GENERAL, Attribute.DEX, "Sleight of Hand",
			"Lifting or hiding items unnoticed, picking locks and other fine handiwork. Tools and knowledge of the mechanism still matter."),
	
	STEALTH(Group.GENERAL, Attribute.DEX, "Stealth",
			"Hiding yourself, approaching quietly and avoiding notice. Does not cover taking items from someone's person (that is Sleight of Hand)."),
	

	ARCANA(Group.GENERAL, Attribute.INT, "Arcana",
			"Magical theory, runes and the supernatural. Recognising a spell and casting one are judged separately."),
	
	HISTORY(Group.GENERAL, Attribute.INT, "History",
			"History, cultures, organisations and institutions."),
	
	INVESTIGATION(Group.GENERAL, Attribute.INT, "Investigation",
			"Sorting evidence, analysing structure and cause, reconstructing what happened. Does not supply missing expertise."),
	
	NATURE(Group.GENERAL, Attribute.INT, "Nature",
			"Species, ecology, terrain and natural phenomena. Knowing the theory is not the same as surviving in the wild."),
	
	RELIGION(Group.GENERAL, Attribute.INT, "Religion",
			"Faiths, rituals, doctrines and religious orders. Does not grant divine power."),
	

	ANIMAL_HANDLING(Group.GENERAL, Attribute.PER, "Animal Handling",
			"Training commands, everyday handling and working together with animals; not a catch-all for every non-human interaction."),
	
	INSIGHT(Group.GENERAL, Attribute.PER, "Insight",
			"Reading emotions, motives and patterns of behaviour. It is not mind reading."),
	
	MEDICINE(Group.GENERAL, Attribute.PER, "Medicine",
			"Diagnosis, first aid, treating wounds and poison. Each treatment still needs a matching rule."),
	
	SCOUTING(Group.GENERAL, Attribute.PER, "Scouting",
			"Searching, keeping watch, surveillance and spotting clues in the environment."),
	
	SURVIVAL(Group.GENERAL, Attribute.PER, "Survival",
			"Navigation, tracking, foraging, choosing a camp and coping in the wild; practical rather than theoretical."),

	
	INTIMIDATION(Group.GENERAL, Attribute.WIL, "Intimidation",
			"Forcing concessions with a credible threat or sheer presence."),
	

	PERSUASION(Group.GENERAL, Attribute.CHA, "Persuasion",
			"Winning willing cooperation with reasons, interests or feelings. The terms of the request still matter."),
	
	DECEPTION(Group.GENERAL, Attribute.CHA, "Deception",
			"Inventing, concealing and keeping up a false impression. Cannot make an impossible claim true."),
	
	PERFORMANCE(Group.GENERAL, Attribute.CHA, "Performance",
			"Music, speeches, acting and holding an audience's attention.");

	public enum Group {
		COMBAT,
		GENERAL;
	}

	private final Group group;
	private final Attribute defaultAttribute;
	private final String displayName;
	private final String description;

	private Skill(Group group, Attribute defaultAttribute, String displayName, String description) {
		this.group = group;
		this.defaultAttribute = defaultAttribute;
		this.displayName = displayName;
		this.description = description;
	}

	public Group getGroup() {
		return this.group;
	}

	public Attribute getDefaultAttribute() {
		return defaultAttribute;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDescription() {
		return description;
	}
}
