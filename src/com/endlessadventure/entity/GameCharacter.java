package com.endlessadventure.entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.endlessadventure.StatRules;
import com.endlessadventure.item.Equipment;
import com.endlessadventure.item.Equipment.Slot;
import com.endlessadventure.item.Item;
import com.endlessadventure.item.Weapon;

public abstract class GameCharacter extends Entity {
	private final EnumMap<Attribute, Integer> attributes = new EnumMap<>(Attribute.class);
	private final EnumMap<Skill, Integer> skills = new EnumMap<>(Skill.class);
	private final EnumMap<Equipment.Slot, Equipment> equipped = new EnumMap<>(Equipment.Slot.class);
	private final List<Item> inventory = new ArrayList<>();

	public GameCharacter(String name, int level, Map<Attribute, Integer> attributes, Map<Skill, Integer> skills) {
		super(name, level);
		attributes.forEach(this::setAttribute);
		skills.forEach(this::setSkill);
	}
	
	/** returns the initiative, defaults to PER + SCOUTING */ //TODO implement calculation when dynamic initiative picking is implemented
	public int getInitiative() {
		return getAttribute(Attribute.PER) + getSkill(Skill.SCOUTING);
	}

	public int getAttribute(Attribute a) {
		return attributes.getOrDefault(a, 0);
	}
	
	public Map<Attribute, Integer> getAttributes() {
		return Map.copyOf(attributes); 
	}

	/** upper limits depend on the kind of character and are checked by the subclass
	 *  @throws IllegalArgumentException */
	protected void setAttribute(Attribute a, int v) {
		if(v < 0) {
			throw new IllegalArgumentException("Invalid attribute value when setting attribute: " + a.name() + " : " + v);
		}
		attributes.put(a, v);
	}

	public int getSkill(Skill s) {
		return skills.getOrDefault(s, 0);
	}
	
	public Map<Skill, Integer> getSkills() { 
		return Map.copyOf(skills); 
	}

	/** upper limits depend on the kind of character and are checked by the subclass
	 *  @throws IllegalArgumentException */
	protected void setSkill(Skill s, int v) {
		if(v < 0) {
			throw new IllegalArgumentException("Invalid skill value when setting skill: " + s.name() + " : " + v);
		}
		skills.put(s, v);
	}
	
	/** returns a read-only list of items */
	public List<Item> getInventory(){
		return List.copyOf(inventory);
	}
	
	public void addItem(Item item) {
		if(item == null) {
			throw new IllegalArgumentException("Invalid item when adding item: item cannot be null.");
		}
		inventory.add(item);
	}
	
	/** remove the item (first occurrence) from the inventory; returns true if successful, false if the item is not in the inventory */
	public boolean removeItem(Item item) {
		return inventory.remove(item);
	}
	
	public Equipment getEquipped(Equipment.Slot slot) {
		if(slot == null) { 
			throw new IllegalArgumentException("Invalid slot when getting equipped: slot cannot be null.");
		}
		return equipped.getOrDefault(slot, null);
	}
	
	public Map<Equipment.Slot, Equipment> getEquippedAll(){
		return Map.copyOf(equipped);
	}
	
	public Equipment unequip(Equipment.Slot slot) {
		return equipped.remove(slot);
	}
	
	/** wears the piece in the slot; returns everything that had to come off if any
	 * the caller should put the returned equipment back to inventory */
	public List<Equipment> equip(Equipment piece, Equipment.Slot slot){
		if(piece == null) {
			throw new IllegalArgumentException("Invalid piece when equipping: piece cannot be null.");
		}
		if(slot == null) {
			throw new IllegalArgumentException("Invalid slot when equipping: slot cannot be null.");
		}
		if(!piece.fits(slot)) {
			throw new IllegalArgumentException(piece + " cannot be equipped in " + slot.label);
		}
		
		List<Equipment> removed = new ArrayList<>();
		unequip(slot, removed);
		
		if(slot == Slot.MAIN_HAND && isTwoHanded(piece)) {
			unequip(Slot.OFF_HAND, removed);
		}
		else if(slot == Slot.OFF_HAND && isTwoHanded(getEquipped(Slot.MAIN_HAND))) {
			unequip(Slot.MAIN_HAND, removed);
		}
		equipped.put(slot, piece);
		return removed;
	}
	
	/** overload helper method of unequip, unequip at slot and add to removed list */
	private void unequip(Slot slot, List<Equipment> removed) {
		Equipment e = unequip(slot);
		if (e != null) {removed.add(e);}
	}
	
	private boolean isTwoHanded(Equipment e) {
		return e instanceof Weapon w && w.getType().grip == Weapon.Grip.TWO_HANDED;
	}
	
	/** overload of equip that takes a piece and automatically chooses the corresponding slot to equip */ 
	public List<Equipment> equip(Equipment piece){
		return equip(piece, chooseSlot(piece));
	}
	
	/** wears a piece from the inventory by the default slot rules (§7.1); whatever comes off goes back to the inventory.
	 *  @throws IllegalArgumentException if the piece is null or not in the inventory; nothing changes then */
	public void equipFromInventory(Equipment piece) {
		if (piece == null || !inventory.contains(piece)) {
			throw new IllegalArgumentException("Invalid equipment when equipping from inventory: not in the inventory: " + piece);
		}
		List<Equipment> removed = equip(piece); //throws before changing anything if the piece fits no slot
		inventory.remove(piece);
		inventory.addAll(removed);
	}
	
	/** takes off what the slot holds and puts it in the inventory; returns false when the slot is empty */
	public boolean unequipToInventory(Equipment.Slot slot) {
		Equipment piece = unequip(slot);
		if (piece == null) {
			return false;
		}
		addItem(piece);
		return true;
	}
	
	/** returns the default slot for the equipped */
	private Equipment.Slot chooseSlot(Equipment piece){
		if (piece instanceof Weapon w && w.getType().grip == Weapon.Grip.LIGHT) {
			if(getEquipped(Slot.MAIN_HAND) == null) {
				return Slot.MAIN_HAND;
			}
			if(getEquipped(Slot.OFF_HAND) == null) {
				return Slot.OFF_HAND;
			}
			return Slot.MAIN_HAND;
		}
		for(Slot s : Slot.values()) {
			if(piece.fits(s)) {
				return s;
			}
		}
		throw new IllegalStateException("Equipment cannot fit into existing slots: " + piece.getName());
	}
	
	public Attribute getMainAttackAttribute() {
		return getEquipped(Slot.MAIN_HAND) instanceof Weapon w? w.getType().attribute : Attribute.STR;
	}
	
	public Attribute getOffHandAttackAttribute() {
		return getEquipped(Slot.OFF_HAND) instanceof Weapon w? w.getType().attribute : null;
	}
	
	/** returns the value for checking hit chance when the target is dodging */
	public int getMainMeleeCheck() {
		//TODO use range skill when ranged weapons implemented 
		return getAttribute(getMainAttackAttribute()) + getSkill(Skill.MELEE);
	}
	
	/** returns the melee check for an off-hand attack */
	public int getOffHandMeleeCheck() {
		return  getAttribute(getOffHandAttackAttribute()) + getSkill(Skill.MELEE);
	}

	/** the value an attacker's melee check is compared against while this character is dodging */
	public int getDodgeCheck() {
		return getAttribute(Attribute.DEX) + getLevel() + 11;
	}
	
	public int getMainAttackPower() {
		//TODO use range skill when ranged weapons implemented 
		return getAttribute(getMainAttackAttribute()) + getSkill(Skill.MELEE) + 
				(getEquipped(Slot.MAIN_HAND) instanceof Weapon w? w.getBaseDmg(getEquipped(Slot.OFF_HAND) == null) : 0);
	}
	
	public int getOffHandAttackPower() {
		return getEquipped(Slot.OFF_HAND) instanceof Weapon w? w.getBaseDmg(false) + getSkill(Skill.MELEE) : 0;
	}
	
	public int getArmor() {
		int total = 0;
		for(Equipment e : equipped.values()) {
			total += e.getArmor();
		}
		return total;
	}
	
	public int getShieldGuard() {
		int total = 0;
		for(Equipment e : equipped.values()) {
			total += e.getGuard();
		}
		return total;
	}
	
	public int getSpeed() {
		int base = StatRules.BASE_SPEED;
		for(Equipment e : equipped.values()) {
			base -= e.getSpeedPenalty();
		}
		return base;
	}
	
	public boolean canDodge() {
		for(Equipment e : equipped.values()) {
			if(!e.allowsDodge()) {
				return false;
			}
		}
		return true;
	}
}
