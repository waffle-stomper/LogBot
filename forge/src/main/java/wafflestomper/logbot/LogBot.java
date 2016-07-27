package wafflestomper.logbot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWrittenBook;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

@Mod(modid = LogBot.MODID, version = LogBot.VERSION, name = LogBot.NAME, updateJSON = "https://raw.githubusercontent.com/waffle-stomper/LogBot/master/update.json")
public class LogBot{
	
    public static final String MODID = "LogBot";
    public static final String VERSION = "0.2.2";
    public static final String NAME = "LogBot";
    
    Minecraft mc;
    private boolean devEnv = true;
    private KeyBindings keybindings;
    private WorldInfo worldInfo;
    private ConfigManager config;
    private boolean logMasterEnable = true;
    private DBInsertThread db = new DBInsertThread();
    
    
    @EventHandler
	public void preInit(FMLPreInitializationEvent event) {
    	this.mc = Minecraft.getMinecraft();
    	FMLCommonHandler.instance().bus().register(this);
		MinecraftForge.EVENT_BUS.register(this);
		this.keybindings = new KeyBindings(this);
		this.devEnv = (Boolean)Launch.blackboard.get("fml.deobfuscatedEnvironment");
		this.worldInfo = new WorldInfo();
    	this.worldInfo.preInit(event);
    	this.config = new ConfigManager();
    	this.config.preInit(event);
    	// Make sure the sqlite driver is available
    	try {
			Class.forName("org.sqlite.JDBC");
			this.db.start();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			DBInsertThread.setSQLDriverMissing();
		}
    }
    
    
    public void loggerKeyPressed(){
    	if (this.logMasterEnable){
    		this.logMasterEnable = false;
			this.mc.thePlayer.addChatMessage(new TextComponentString("\u00A7bLogger disabled"));
    	}
    	else{
    		this.logMasterEnable = true;
    		this.mc.thePlayer.addChatMessage(new TextComponentString("\u00A7bLogger enabled"));
    	}
    }
    
    
    public boolean writeFile(List<String> toWrite, String fileName){
    	if (!this.logMasterEnable || !this.config.logToTextFiles){
    		return true;
    	}
    	long methodStart = System.currentTimeMillis();
		String basepath = Minecraft.getMinecraft().mcDataDir.getAbsolutePath();
		if (basepath.endsWith(".")){
			basepath = basepath.substring(0, basepath.length()-2);
		}
		String serverIP = this.worldInfo.getSanitizedServerIP();
		File serverPath = new File(basepath, "mods" + File.separator + "LogBot" + File.separator + serverIP);
		if (!serverPath.exists()) serverPath.mkdirs();
		File filePath = new File(serverPath, fileName);
		
		boolean failedFlag = false;
		
		//Create directory if it doesn't exist
		File path = filePath.getParentFile();
		if (!path.exists()){
			if (!path.mkdirs()){
				failedFlag = true;
			}
		}
		
		//Write file
		if (!failedFlag){
			try {
				Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, true), "UTF-8"));
				try{
					for (String s : toWrite){
						out.write(s + '\n');
					}
				}
				catch (IOException e){
					failedFlag = true;
					System.out.println(e.getMessage());
					System.out.println("Write failed!");
					return false;
				}
				finally{
					out.close();
				}
			} 
			catch (IOException e) {
				failedFlag = true;
				System.out.println(e.getMessage());
				System.out.println("Write failed!");
				return false;
			}
		}
		
		if (failedFlag){
			this.mc.thePlayer.addChatMessage(new TextComponentString("\u00A7cWRITING TO DISK FAILED!"));
			return false;
		}		
		//System.out.println("Text file write took " + (System.currentTimeMillis()-methodStart) + "ms");
		return true;
	}
    
    
    public boolean writeFile(String toWrite, String fileName){
    	ArrayList<String> temp = new ArrayList();
    	temp.add(toWrite);
    	return(writeFile(temp, fileName));
    }
    
    
    /* This is a dirty hack, but I'm a terrible person, so what do you expect? */    
    private boolean chestWasOpen = false;
    private List<ItemStack> cachedInv = new ArrayList();
    private BlockPos chestPos;
    @SubscribeEvent
    public void playerTick(PlayerTickEvent event){
    	if (!this.logMasterEnable){
    		return;
    	}
    	if (this.mc.currentScreen instanceof GuiChest && this.config.logChests){
    		// Get the position of the chest we're pointing at
    		GuiChest chestGui = (GuiChest)this.mc.currentScreen;
    		RayTraceResult currMOP = this.mc.objectMouseOver;
    		if (currMOP != null){
    			chestPos = currMOP.getBlockPos();
    		}
    		
    		List<ItemStack> chestInv = chestGui.inventorySlots.getInventory();
    		if (chestInv == null){ return; }
    		
    		// If it's a double chest, get the co-ordinate of the most north-western chest block
    		// As far as I can tell, that should just consist of testing the blocks one to the north, and one to the west
    		// If one of them is also a chest, then it must be linked
    		int chestSize = 27;
    		if (chestInv.size() == 90){
    			chestSize = 54;
    			if (this.mc.theWorld.getBlockState(this.chestPos.north()).getBlock().getUnlocalizedName().equals("tile.chest")){
    				this.chestPos = this.chestPos.north();
    			}
    			else if (this.mc.theWorld.getBlockState(this.chestPos.west()).getBlock().getUnlocalizedName().equals("tile.chest")){
    				this.chestPos = this.chestPos.west();
    			}
    		}
    		
    		// Store the chest contents in the cache
    		this.chestWasOpen = true;
    		for(int i=0; i<chestSize; i++){
    			if (i >= cachedInv.size()){
    				cachedInv.add(chestInv.get(i));
    			}
    			else{
    				if (cachedInv.get(i) != chestInv.get(i)){
    					cachedInv.set(i,  chestInv.get(i));
    				}
    			}
    		}
    	}
    	else if(this.mc.currentScreen == null && this.chestWasOpen  && this.config.logChests){
    		this.chestWasOpen = false;
    		// Compile chest contents into list of strings
    		String contents = "";
    		for(int i=0; i<cachedInv.size(); i++){
    			ItemStack currStack = cachedInv.get(i);
    			if (currStack != null){
    				
    				if (currStack.getItem() instanceof ItemWrittenBook){
    					if (currStack.hasTagCompound()){
    						NBTTagCompound tags = currStack.getTagCompound();
    						String bookDetails = "";
    						if (!tags.hasKey("title", 10)){
    				            String title = tags.getTag("title").toString();
    				            if (!title.isEmpty()){
    				            	bookDetails = title + " ";
    				            }
    				        }
    						if (!tags.hasKey("author", 10)){
    	    					bookDetails = bookDetails + "by " + tags.getTag("author").toString();
    	    				}
    						contents = contents + String.valueOf(currStack.stackSize) + "x" + "Written Book" + " [" + bookDetails + "]";
    					}
    				}
    				else{
    					contents = contents + String.valueOf(currStack.stackSize) + "x" + currStack.getDisplayName();
    				}
    			}
    			if  (i < cachedInv.size()-1){
    				contents = contents + ",";
    			}
    		}
    		
    		// Dump to text file
    		if (this.config.logToTextFiles){
	    		String suffix = "";
	    		if (chestPos != null){
	    			suffix = "chestposxyz_" + chestPos.getX() + "_" + chestPos.getY() + "_" + chestPos.getZ();
	    		}
	    		else{
	    			suffix = "playerposxyz_" + (int)this.mc.thePlayer.posX + "_" + (int)this.mc.thePlayer.posY + "_" + (int)this.mc.thePlayer.posZ;
	    		}
	    		writeFile(contents, "chests" + File.separator + this.mc.thePlayer.getName() + "_" + suffix + ".txt");
    		}
    		
    		// Write to DB
    		if (this.config.logToDB){
	    		String serverName = this.worldInfo.getSanitizedServerIP();
		    	String worldName = this.worldInfo.getWorldName();
		    	long first = System.currentTimeMillis();
		    	try {
					this.db.queue.put(new RecordChest(serverName, worldName, chestPos.getX(), chestPos.getY(), chestPos.getZ(), contents));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		    	//System.out.println("Chest put took " + (System.currentTimeMillis()-first) + "ms");
    		}
    		
    		// Clear cache
    		cachedInv.clear();
    	}
    }
    
    
    @SubscribeEvent
    public void rightClickBlock(PlayerInteractEvent.RightClickBlock event){
    	BlockPos pos = event.getPos();
    }
    

    // An alternate event is PlayerInteractEvent$LeftClickBlock
    List<BlockPos> minedList = new ArrayList();
    @SubscribeEvent
    public void breakingBlock(BreakSpeed event){
    	if (!this.logMasterEnable || !this.config.logMinedBlocks){
    		return;
    	}
    	BlockPos pos = event.getPos();
    	Block currBlock = this.mc.theWorld.getBlockState(pos).getBlock();
		String uName = currBlock.getUnlocalizedName();
		
		if (this.config.onlyLogOres){
			if (!uName.startsWith("tile.ore") && !uName.equals("tile.netherquartz")){
				return;
			}
		}
		
		if (minedList.contains(pos)){
			return;
		}
		minedList.add(pos);
		
		int playerY = (int)this.mc.thePlayer.posY;
		if (this.config.logToTextFiles){
        	writeFile(uName + "," + pos.getX() + "x," + pos.getY() + "y," + pos.getZ() + "z," + playerY + "y_player," + 
                      (int)(System.currentTimeMillis()/1000), 
                      this.worldInfo.getWorldName() + ".txt");
        }
		
		if (this.config.logToDB){
	    	String serverName = this.worldInfo.getSanitizedServerIP();
	    	String worldName = this.worldInfo.getWorldName();
	    	
	    	long first = System.currentTimeMillis();
	    	try {
				this.db.queue.put(new RecordBlock(serverName, worldName, uName, pos.getX(), pos.getY(), pos.getZ(), true, "player_y=" + playerY));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    	//System.out.println("Block break put took " + (System.currentTimeMillis()-first) + "ms");
		}
		
    }
}


