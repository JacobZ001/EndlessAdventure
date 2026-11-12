package com.endlessadventure.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.endlessadventure.Displayable;
import com.endlessadventure.entity.Attribute;

public final class Weapon extends Equipment {
	private static final int VERSATILE_BONUS_PER_TIER = 2;
	
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
		return type.toString();
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
		tags.add(Span.of(type.grip, Tone.KEYWORD));
		return tags;
	}

	@Override
	protected void addStats(List<Detail> details) {
		String power = type.grip == Grip.VERSATILE ? getBaseDmg(false) + " (" + getBaseDmg(true) + ")" : String.valueOf(getBaseDmg(false));
		details.add(Detail.Stat.of("Scaling", type.attribute.name(), type.attribute));
		details.add(Detail.Stat.of("Attack Power", power, Tone.PLAIN));
	}

	@Override
	protected void addProperties(List<Detail> details) {
		for (Property p : type.properties) {
			details.add(new Detail.Trait(Span.of(p, Tone.KEYWORD), p.describe(getQuality().tier)));
		}
	}
	
	public enum Grip { 
		ONE_HANDED,
		LIGHT,
		TWO_HANDED,
		VERSATILE;

		@Override
		public String toString() {
			return Displayable.titleCase(name());
		}
	}
	
    public enum Type {
        LONGSWORD(6, Attribute.STR, Grip.VERSATILE),
        MACE(6, Attribute.STR, Grip.ONE_HANDED,
        		Property.PIERCE),
        SCIMITAR(6, Attribute.DEX, Grip.ONE_HANDED),
        GREATSWORD(9, Attribute.STR, Grip.TWO_HANDED,
        		Property.BREAK_GUARD),
        SHORTSWORD(3, Attribute.DEX, Grip.LIGHT,
        		Property.LETHAL),
        DAGGER(3, Attribute.DEX, Grip.LIGHT,
        		Property.ASSASSINATE);

        public final int base;
        public final Attribute attribute;
        public final Grip grip;
        public final Set<Property> properties;

        Type(int base, Attribute attribute, Grip grip, Property... properties) {
        	this.base = base;
        	this.attribute = attribute;
        	this.grip = grip;
            this.properties = Set.of(properties);
        }

        @Override
        public String toString() {
        	return Displayable.titleCase(name());
        }
    }
	
    public enum Property {
        PIERCE(2, "Reduces target armor by %d"),
        BREAK_GUARD(0, "Ignores the target's guard"),
        LETHAL(0, "Critical hit on targets at half HP or less"),
        ASSASSINATE(0, "Critical hit on targets that have not acted yet");

    	public final int potencyPerTier;
    	public final String description;

    	Property(int perTier, String description){
    		this.potencyPerTier = perTier;
    		this.description = description;
    	}

		@Override
		public String toString() {
			return Displayable.titleCase(name());
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
}
