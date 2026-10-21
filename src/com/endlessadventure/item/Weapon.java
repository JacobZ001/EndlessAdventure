package com.endlessadventure.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.endlessadventure.entity.Attribute;

public final class Weapon extends Equipment {
	private static final int VERSATILE_BONUS_PER_TIER = 2;
	
	public enum Grip { 
		ONE_HANDED("One Handed"), 
		LIGHT("Light"), 
		TWO_HANDED("Two Handed"), 
		VERSATILE("Versatile");
		
		public final String label;
		
		Grip(String label){
			this.label = label;
		}
	}
	
    public enum Type {
        LONGSWORD("Longsword", 6, Attribute.STR, Grip.VERSATILE),
        MACE("Mace", 6, Attribute.STR, Grip.ONE_HANDED,
        		Property.PIERCE),
        SCIMITAR("Scimitar", 6, Attribute.DEX, Grip.ONE_HANDED),
        GREATSWORD("Greatsword", 9, Attribute.STR, Grip.TWO_HANDED,
        		Property.BREAK_GUARD),
        SHORTSWORD("Shortsword", 3, Attribute.DEX, Grip.LIGHT,
        		Property.LETHAL),
        DAGGER("Dagger", 3, Attribute.DEX, Grip.LIGHT,
        		Property.ASSASSINATE);
    	
        public final String label;
        public final int base;
        public final Attribute attribute;
        public final Grip grip;
        public final Set<Property> properties;

        Type(String label, int base, Attribute attribute, Grip grip, Property... properties) {
        	this.label = label;
        	this.base = base;
        	this.attribute = attribute;
        	this.grip = grip;
            this.properties = Set.of(properties);
        }
    }
	
    public enum Property {
        PIERCE("Pierce", 2 ,"Reduces target armor by %d"),
        BREAK_GUARD("Break Guard",0, "Ignores the target's guard"),
        LETHAL("Lethal",0, "Critical hit on targets at half HP or less"),
        ASSASSINATE("Assassinate",0, "Critical hit on targets that have not acted yet");
        
    	public final String label;
    	public final int potencyPerTier;
    	public final String description;
    	
    	Property(String label, int perTier, String description){
    		this.label = label;
    		this.potencyPerTier = perTier;
    		this.description = description;
    	}

		/** the description for a weapon of this tier, with its number as a separate span so the ui can color it */
		public List<Span> describe(int tier) {
			String[] parts = description.split("%d", 2);
			if (parts.length == 1) {
				return List.of(Span.of(description, Tone.NOTE));
			}
			List<Span> spans = new ArrayList<>();
			if (!parts[0].isEmpty()) {
				spans.add(Span.of(parts[0], Tone.NOTE));
			}
			spans.add(Span.plain(potencyPerTier * tier));
			if (!parts[1].isEmpty()) {
				spans.add(Span.of(parts[1], Tone.NOTE));
			}
			return spans;
		}
	}

    private final Type type;

	public Weapon(String id, String name, String description, Quality quality, Type type) {
		super(id, name, description, quality);
		if(type == null) {
			throw new IllegalArgumentException("Illegal weapon: type cannot be null.");
		}
		this.type = type;
	}
	
	public Type getType() {return type;}
	
	public int getBaseDmg(boolean doubleGripped) {
		int bonus = doubleGripped && type.grip == Grip.VERSATILE? VERSATILE_BONUS_PER_TIER : 0;
		return (type.base + bonus) * getQuality().tier;
	}
	
	public boolean hasProperty(Property p) {
		return type.properties.contains(p);
	}
	
	public int getPenetration() {
		return hasProperty(Property.PIERCE)? Property.PIERCE.potencyPerTier * getQuality().tier : 0;
	}

	@Override
	public String getCategoryLabel() {
		return type.label;
	}

	@Override
	public boolean fits(Slot slot) {
		return switch(slot) {
			case MAIN_HAND -> true;
			case ARMOR -> false;
			case OFF_HAND -> getType().grip == Grip.LIGHT;
		};
	}

	@Override
	protected List<Span> tags() {
		List<Span> tags = super.tags();
		tags.add(Span.of(type.grip.label, Tone.NOTE));
		return tags;
	}

	@Override
	protected void addStats(List<Detail> details) {
		String power = type.grip == Grip.VERSATILE ? getBaseDmg(false) + " (" + getBaseDmg(true) + ")" : String.valueOf(getBaseDmg(false));
		details.add(Detail.Stat.of("Scaling", type.attribute.name(), type.attribute));
		details.add(Detail.Stat.of("Attack Power", power, type.attribute));
	}

	@Override
	protected void addProperties(List<Detail> details) {
		for (Property p : type.properties) {
			details.add(new Detail.Trait(Span.of(p.label, Tone.KEYWORD), p.describe(getQuality().tier)));
		}
	}
}
