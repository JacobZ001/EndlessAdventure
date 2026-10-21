/**
 * 
 */
/**
 * 
 */
module EndlessAdventure {
	requires java.net.http;
	requires org.jline.reader;
	requires org.jline.terminal;
	requires com.google.gson;
	opens com.endlessadventure.data to com.google.gson;   // the private row records
	opens com.endlessadventure.item to com.google.gson;   // enum constants are read by reflection
	opens com.endlessadventure.entity to com.google.gson; // Attribute and Skill are map keys in save files
	opens com.endlessadventure.save to com.google.gson;   // the save file records
}