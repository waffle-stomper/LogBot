package wafflestomper.logbot;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ConfigManager {
	
	private Configuration config;
	protected boolean logChests = true;
	protected boolean logMinedBlocks = true;
	protected boolean onlyLogOres = true;
	protected boolean logToDB = true;
	protected boolean logToTextFiles = false;
	
	
	public void preInit(FMLPreInitializationEvent event){
	    this.config = new Configuration(event.getSuggestedConfigurationFile());
	    this.config.load();
	    
	    this.logChests = this.config.get("input", "log_chest_contents", true, "Log the contents of any chests you open").getBoolean(true);
	    this.logMinedBlocks = this.config.get("input", "log_mined_blocks", true, "Log the positions of blocks you mine").getBoolean(true);
	    this.onlyLogOres = this.config.get("input", "only_log_ores", true, "Only log mined ores (ignore all other blocks)").getBoolean(true);
	    
	    this.logToDB = this.config.get("output", "output_to_db", true, "SQLite database output").getBoolean(true);
	    this.logToTextFiles = this.config.get("output", "output_to_textfiles", false, "Text file output").getBoolean(false);
	    
	    this.config.save();
    }
}
