package com.endlessadventure.inventory;

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
		
		private final String label;
		
		Grip(String label){
			this.label = label;
		}

		public String getLabel() {return label;}
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
    	
        private final String label;
        private final int base;
        private final Attribute attribute;
        private final Grip grip;
        private final Set<Property> properties;

        Type(String label, int base, Attribute attribute, Grip grip, Property... properties) {
        	this.label = label;
        	this.base = base;
        	this.attribute = attribute;
        	this.grip = grip;
            this.properties = Set.of(properties);
        }
        
        public String getLabel() {return label;}
        
        public int getBase() {return base;}
                
        public Attribute getAttribute() {return attribute;}
        
        public Grip getGrip() {return grip;}
        
        public Set<Property> getProperties(){return properties;}
    }
	
    public enum Property {
        PIERCE("Pierce", 2 ,"Reduces target armor by %d"),
        BREAK_GUARD("Break Guard",0, "Ignores the target's guard"),
        LETHAL("Lethal",0, "Critical hit on targets at half HP or less"),
        ASSASSINATE("Assassinate",0, "Critical hit on targets that have not acted yet");
        
    	private final String label;
    	private final int potencyPerTier;
    	private final String description;
    	
    	Property(String label, int perTier, String description){
    		this.label = label;
    		this.potencyPerTier = perTier;
    		this.description = description;
    	}
		
		public String getLabel() {return label;}
		
		public int getPotencyPerTier() {return potencyPerTier;}
		
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
		int bonus = doubleGripped && type.getGrip() == Grip.VERSATILE? VERSATILE_BONUS_PER_TIER : 0;
		return (type.getBase() + bonus) * getQuality().getTier();
	}
	
	public boolean hasProperty(Property p) {
		return type.getProperties().contains(p);
	}
	
	public int getPenetration() {
		return hasProperty(Property.PIERCE)? Property.PIERCE.getPotencyPerTier() * getQuality().getTier() : 0;
	}

	@Override
	public String getCategoryLabel() {
		return type.getLabel();
	}

	@Override
	public boolean fits(Slot slot) {
		return switch(slot) {
			case MAIN_HAND -> true;
			case ARMOR -> false;
			case OFF_HAND -> getType().getGrip() == Grip.LIGHT;
		};
	}

	@Override
	protected List<Span> tags() {
		List<Span> tags = super.tags();
		tags.add(Span.of(type.getGrip().getLabel(), Tone.NOTE));
		return tags;
	}

	@Override
	protected void addStats(List<Detail> details) {
		String power = type.getGrip() == Grip.VERSATILE ? getBaseDmg(false) + " (" + getBaseDmg(true) + ")" : String.valueOf(getBaseDmg(false));
		details.add(Detail.Stat.of("Scaling", type.getAttribute().name(), type.getAttribute()));
		details.add(Detail.Stat.of("Attack Power", power, type.getAttribute()));
	}

	@Override
	protected void addProperties(List<Detail> details) {
		for (Property p : type.getProperties()) {
			details.add(new Detail.Trait(Span.of(p.getLabel(), Tone.KEYWORD), p.describe(getQuality().getTier())));
		}
	}
}
