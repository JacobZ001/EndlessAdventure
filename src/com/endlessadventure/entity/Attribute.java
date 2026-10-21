package com.endlessadventure.entity;

public enum Attribute{
	STR("Strength",
			"Applying physical force and overcoming resistance.",
			"Force-based attacks, shoving and breaking free. Does not raise every attack's damage."),
	DEX("Dexterity",
			"Coordination, balance and fine motor control.",
			"Precise attacks and dodging. Does not add action points, speed or extra attacks."),
	CON("Constitution",
			"Health, carrying loads and keeping going.",
			"Extra HP, physical resistance and abilities that scale with constitution."),
	INT("Intelligence",
			"Reasoning, understanding structure and applying knowledge.",
			"Analytical and knowledge-based actions, including spells. Does not raise all spell power."),
	PER("Perception",
			"Picking up cues from surroundings and behaviour. Does not reveal hidden facts on its own.",
			"Default initiative and aimed actions with explicit rules."),
	WIL("Willpower",
			"Staying in control and focused under pressure and distraction.",
			"Mental resistance, concentration and willpower-driven abilities."),
	CHA("Charisma",
			"Expression, presence and influence on others. Not looks or fame.",
			"Influence and provocation. Social success is not free control over enemies.");

	public final String displayName;
	public final String description;	//what the attribute covers in general
	public final String combatRole;	//what the attribute does in combat

	private Attribute(String displayName, String description, String combatRole) {
		this.displayName = displayName;
		this.description = description;
		this.combatRole = combatRole;
	}
}
