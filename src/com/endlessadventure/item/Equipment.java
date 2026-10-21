package com.endlessadventure.item;

import java.util.ArrayList;
import java.util.List;

public abstract class Equipment extends Item {
	private final Quality quality;
	
	public Equipment(String id, String name, String description, Quality quality) {
		super(id, name, description);
		if(quality == null) {
			throw new IllegalArgumentException("Illegal equipment: quality cannot be null.");
		}
		this.quality = quality;
	}
	
	public Quality getQuality() {return quality;}
	
    /** whether this piece can go in the slot */
    public abstract boolean fits(Slot slot);
    
    // what wearing this piece adds; each subclass overrides only its own part
    public int getArmor()   { return 0; }
    public int getGuard()   { return 0; }
    public int getSpeedPenalty() { return 0; }
    public boolean allowsDodge() { return true; }
    
    @Override
    public Enum<?> getNameMeaning() {
    	return quality.tone;
    }

    /** the layout every piece of equipment shares: name, tags, stats, properties; subclasses fill the parts */
    @Override
    public final List<Detail> getDetails() {
    	List<Detail> details = new ArrayList<>();
    	details.add(Detail.Line.of(getName(), quality.tone));
    	details.add(new Detail.Tags(tags()));
    	addStats(details);
    	addProperties(details);
    	return details;
    }

    /** quality and kind, e.g. "Rare Longsword"; returns a new list that subclasses may add to */
    protected List<Span> tags() {
    	List<Span> tags = new ArrayList<>();
    	tags.add(Span.of(quality.label + " " + getCategoryLabel(), quality.tone));
    	return tags;
    }

    protected abstract void addStats(List<Detail> details);

    /** named rules such as weapon properties; most equipment has none */
    protected void addProperties(List<Detail> details) {}
    
	public enum Slot { 
		MAIN_HAND("Main Hand"), 
		ARMOR("Armor"), 
		OFF_HAND("Off Hand");
		
		public final String label;
		
		Slot(String label){
			this.label = label;
		}
	}
	
	public enum Quality {
		COMMON(1, "Common", Tone.TIER_1),
		FINE(2, "Fine", Tone.TIER_2),
		RARE(3,"Rare", Tone.TIER_3),
		MASTER(4,"Master", Tone.TIER_4),
		EPIC(5, "Epic", Tone.TIER_5);
		
		public final int tier;
		public final String label;
		public final Tone tone;
		
		Quality(int tier, String label, Tone tone){
			this.tier = tier;
			this.label = label;
			this.tone = tone;
		}
	}

}
