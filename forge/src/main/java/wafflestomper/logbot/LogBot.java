package wafflestomper.logbot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import wafflestomper.wafflecore.WaffleCore;

@Mod(modid = LogBot.MODID, version = LogBot.VERSION, name = LogBot.NAME, updateJSON = "https://raw.githubusercontent.com/waffle-stomper/LogBot/master/update.json")
public class LogBot{
	
    public static final String MODID = "LogBot";
    public static final String VERSION = "0.2.8";
    public static final String NAME = "LogBot";
    
    Minecraft mc;
    private boolean devEnv = true;
    private KeyBindings keybindings;
    private ConfigManager config;
    private boolean logMasterEnable = true;
    private DBThread db = DBThread.INSTANCE;
    private static final Logger logger = LogManager.getLogger("LogBot");
    private static WaffleCore wafflecore;
    
    
    @EventHandler
	public void preInit(FMLPreInitializationEvent event) {
    	this.mc = Minecraft.getMinecraft();
    	FMLCommonHandler.instance().bus().register(this);
		MinecraftForge.EVENT_BUS.register(this);
		this.keybindings = new KeyBindings(this);
		this.devEnv = (Boolean)Launch.blackboard.get("fml.deobfuscatedEnvironment");
		wafflecore = WaffleCore.INSTANCE;
    	this.config = new ConfigManager();
    	this.config.preInit(event);
    	// Make sure the sqlite driver is available
    	try {
			Class.forName("org.sqlite.JDBC");
			this.db.registerCreateTable(RecordBlock.createTableStatement);
			this.db.registerCreateTable(RecordChest.createTableStatement);
			this.db.start();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			this.db.setSQLDriverMissing();
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
		String serverIP = wafflecore.worldInfo.getNiceServerIP();
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
					logger.error(e.getMessage());
					logger.error("Write failed!");
					return false;
				}
				finally{
					out.close();
				}
			} 
			catch (IOException e) {
				failedFlag = true;
				logger.error(e.getMessage());
				logger.error("Write failed!");
				return false;
			}
		}
		
		if (failedFlag){
			this.mc.thePlayer.addChatMessage(new TextComponentString("\u00A7cWRITING TO DISK FAILED!"));
			return false;
		}		
		logger.debug("Text file write took " + (System.currentTimeMillis()-methodStart) + "ms");
		return true;
	}
    
    
    public boolean writeFile(String toWrite, String fileName){
    	ArrayList<String> temp = new ArrayList<String>();
    	temp.add(toWrite);
    	return(writeFile(temp, fileName));
    }
    
    
    private String getDetailedItemName(ItemStack stack){
    	// We start with the registry name (e.g. minecraft.stone)
    	String assembledName = stack.getItem().getRegistryName().toString();
    	
    	// Get the tooltip and process it
    	String toolTip = "|";
    	List<String> tipList = stack.getTooltip(this.mc.thePlayer, false);
    	for (String tipLine : tipList){
    		if (tipLine.startsWith("When in main hand")){ 
    			break; 
    		}
    		if (!toolTip.equals("|")){
    			toolTip += " ";
    		}
    		toolTip += tipLine.replaceAll("\u00A7[a-fA-Fk-oK-Or0-9]", "");
    	}
    	
    	assembledName += toolTip;
    	return(assembledName.replaceAll(",", "\\\\,"));
    }
    
    
    /* This is a dirty hack, but I'm a terrible person, so what do you expect? */    
    private boolean chestWasOpen = false;
    private List<ItemStack> cachedInv = new ArrayList<ItemStack>();
    private BlockPos chestPos;
    private boolean minecartChest = false;
    @SubscribeEvent
    public void playerTick(PlayerTickEvent event){
    	if (!this.logMasterEnable){
    		return;
    	}
    	if (this.mc.currentScreen instanceof GuiChest && this.config.logChests){
    		// Get the position of the chest we're pointing at
    		GuiChest chestGui = (GuiChest)this.mc.currentScreen;
    		RayTraceResult currMOP = this.mc.objectMouseOver;
    		this.minecartChest = false;
    		if (currMOP != null){
    			chestPos = currMOP.getBlockPos();
    			if (chestPos == null && currMOP.entityHit != null && currMOP.entityHit instanceof EntityMinecartChest){
    				chestPos = currMOP.entityHit.getPosition();
    				this.minecartChest = true;
    			}
    			if (chestPos == null){
    				logger.error("Couldn't get chest pos");
    				this.mc.thePlayer.addChatMessage(new TextComponentString("\u00A7cCouldn't get chest pos"));
    			}
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
    				contents = contents + String.valueOf(currStack.stackSize) + "x" + getDetailedItemName(currStack);
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
	    		String serverName = wafflecore.worldInfo.getNiceServerIP();
		    	String worldName = wafflecore.worldInfo.getWorldName();
		    	long first = System.currentTimeMillis();
		    	try {
					this.db.queue.put(new RecordChest(serverName, worldName, chestPos.getX(), chestPos.getY(), chestPos.getZ(), this.minecartChest, contents, ""));
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
    
    
    /*
     * Here's a rough guide to the mine logging process:
     * User 'hits/left clicks' a block, the block is added to watch list with a timestamp
     * If the block does disappear, it's considered mined and it's added to a list while we wait for the drop
     * While all of this is going on, we're keeping track of new entites, specifically items and their initial position.
     * We wait for some settling time to see if the new items change (all blocks are spawned as stone and have their real attributes swapped in later)
     * Once we are sure that the item has settled on its final form, we compare its initial position with the positions and of all recently mined blocks
     * If eveything matches up, we can finally add it to the database and remove the item and block position from the lists
     */
    
    private class BlockDetail{
    	public BlockPos pos;
    	public String blockName;
    	public long hitTime = -1; // When the block was last hit
    	public long airTime = -1; // When the block became air (i.e. was finished mining)
    	
    	public BlockDetail(BlockPos _pos, String _blockName, long _hitTime, long _airTime){
    		this.pos = _pos;
    		this.blockName = _blockName;
    		this.hitTime = _hitTime;
    		this.airTime = _airTime;
    	}
    	
    	public boolean hasAirTime(){ 
			return(this.airTime > -1); 
		}
    	
    	public String toString(){
    		return(this.blockName + "@" + this.pos.toString());
    	}
    }
    
    
    private class DroppedItem{
    	public int id;
    	public BlockPos sourcePos;
    	public String drop;
    	public long createdTime;
    	
    	public DroppedItem(int _id, BlockPos _sourcePos, String _drop){
    		this.createdTime = System.currentTimeMillis();
    		this.id = _id;
    		this.sourcePos = _sourcePos;
    		this.drop = _drop;
    	}
    }
    
    
    private <T extends Comparable<T>> String getDetailedBlockName(BlockPos blockpos){
    	IBlockState iblockstate = this.mc.theWorld.getBlockState(blockpos);

    	if (this.mc.theWorld.getWorldType() != WorldType.DEBUG_WORLD){
    		iblockstate = iblockstate.getActualState(this.mc.theWorld, blockpos);
    	}

    	String assembledName = String.valueOf(Block.REGISTRY.getNameForObject(iblockstate.getBlock()));

    	for (Entry < IProperty<?>, Comparable<? >> entry : iblockstate.getProperties().entrySet()){
    		IProperty<T> iproperty = (IProperty)entry.getKey();
            T t = (T)entry.getValue();
            String s = iproperty.getName(t);
    		assembledName += "|" + iproperty.getName() + ":" + entry.getValue();
    	}
    	return(assembledName);
    }
    
    
    private HashMap<BlockPos, BlockDetail> miningBlocks = new HashMap();
    private HashMap<Integer, DroppedItem> newItems = new HashMap();
    
    
    @SubscribeEvent
    public void entityJoinWorld(EntityJoinWorldEvent event){
    	Entity e = event.getEntity();
    	if (e == null){ return; }
    	if (e instanceof EntityItem){
    		EntityItem i = (EntityItem)e;
    		ItemStack is = i.getEntityItem();
    		if (is == null){ return; }
    		
    		// Round down to convert to block-style co-ordinates
    		int posX = (int)Math.floor(e.posX);
    		int posY = (int)Math.floor(e.posY);
    		int posZ = (int)Math.floor(e.posZ);
    		BlockPos pos = new BlockPos(posX, posY, posZ);
    		
    		// Add the item to the list of recently dropped items
    		this.newItems.put(e.getEntityId(), new DroppedItem(e.getEntityId(), pos, getDetailedItemName(is)));
    		logger.debug("New item at [" + posX+ "," +  posY+ "," +  posZ + "]");
    	}
    }
    
    
    @SubscribeEvent
    public void userHitBlock(PlayerInteractEvent.LeftClickBlock event){
    	if (!this.config.logMinedBlocks){ return; }
    	BlockPos pos = event.getPos();
    	if (pos != null){
    		Block block = this.mc.theWorld.getBlockState(pos).getBlock();
	    	if (block != null){
	    		if (block.getRegistryName().toString().endsWith("_ore") == false && this.config.onlyLogOres){ return; }
	    		long currTime = System.currentTimeMillis();
	    		miningBlocks.put(pos, new BlockDetail(pos, getDetailedBlockName(pos), currTime, -1));
	    	}
    	}
    }
    
    
    private void addBlockToTextFile(BlockDetail block, String drops){
    	if (this.config.logToTextFiles){
	    	String serverName = wafflecore.worldInfo.getNiceServerIP();
			String worldName = wafflecore.worldInfo.getWorldName();
			String blockName = block.blockName;
			int x = block.pos.getX();
			int y = block.pos.getY();
			int z = block.pos.getZ();
			logger.debug("Adding " + block.toString() + " to blocks file");
			String outString = worldName + "," + blockName + "," + drops + "," + x + "," + y + "," + z;
			this.writeFile(outString, "blocks.txt");
		}
		
    }
    
    
    private void addBlockToDB(BlockDetail block, String drops){
    	if (this.config.logToDB){
	    	String serverName = wafflecore.worldInfo.getNiceServerIP();
			String worldName = wafflecore.worldInfo.getWorldName();
			String blockName = block.blockName;
			int x = block.pos.getX();
			int y = block.pos.getY();
			int z = block.pos.getZ();
			try {
				logger.debug("Adding " + block.toString() + " to database");
				this.db.queue.put(new RecordBlock(serverName, worldName, blockName, drops, x, y, z, true, ""));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
    }
    
    
    private long minerTickLastExecute = 0;
    @SubscribeEvent
    public void minerTick(PlayerTickEvent event){
    	//if (System.currentTimeMillis() - this.minerTickLastExecute < 5000){ return; } //rate limiting during testing
    	//this.minerTickLastExecute = System.currentTimeMillis(); logger.warn("minerTick() IS RATE LIMITED! DISABLE THIS LIMITING ASAP OR THE ALGORITHM WON'T WORK CORRECTLY!");
    	
    	// Update items in newItems if they've changed (since almost everything seems to spawn as stone)
    	if (this.mc.theWorld == null){ return; }
    	List entityList = mc.theWorld.loadedEntityList;
    	for (Object o : entityList){
    		Entity e = (Entity) o;
    		if (e instanceof EntityItem) {
    			int id = e.getEntityId();
    			BlockPos pos = e.getPosition();
				if (this.newItems.containsKey(id)){
					DroppedItem originalItem = this.newItems.get(id);
					EntityItem ei = (EntityItem) e;
					ItemStack latestStack = ei.getEntityItem();
					int stackSize = ei.getEntityItem().stackSize;
					String latestName = getDetailedItemName(latestStack);
					//TODO: Use a faster comparison method?
					if (!latestName.equals(originalItem.drop)){
						logger.debug("id "+e.getEntityId() + " changed from " +
								originalItem.drop + " to " +
								latestName + "x" + stackSize +", time: " + 
								(System.currentTimeMillis()-originalItem.createdTime) + "ms" );
						this.newItems.put(originalItem.id, new DroppedItem(id, originalItem.sourcePos, latestName));
					}
				}
    		}
    	}
    	
    	// Clean up stale items
    	for(Iterator<HashMap.Entry<Integer,DroppedItem>>it = this.newItems.entrySet().iterator(); it.hasNext();){
    		HashMap.Entry<Integer, DroppedItem> entry = it.next();
    		if (System.currentTimeMillis() - entry.getValue().createdTime > 30000){
    			it.remove();
    		}
    	}
    	
    	// Go through hit blocks looking for any that have timed out, completed mining, or have mature drops
    	for(Iterator<HashMap.Entry<BlockPos,BlockDetail>>blockIter = this.miningBlocks.entrySet().iterator(); blockIter.hasNext();){
    		HashMap.Entry<BlockPos,BlockDetail> blockEntry = blockIter.next();
    		BlockDetail currBlock = blockEntry.getValue();
    		
    		// Remove blocks that are 30 seconds (or older) from the list, or are not in a loaded chunk, adding them if they were last air
    		if (System.currentTimeMillis() - currBlock.hitTime > 30000 || !this.mc.theWorld.isBlockLoaded(currBlock.pos)){
    			logger.debug("Removing stale entry [" + currBlock.toString() + "] from miningBlocks");
    			if (currBlock.hasAirTime()){
    				this.addBlockToDB(currBlock, "");
    				this.addBlockToTextFile(currBlock, "");
    			}
    			blockIter.remove();
    			continue;
    		}
    		
    		// If the block has become air, set airTime. Otherwise make sure the airTime is cleared (in case the block break was erroneous and it's re-appeared)
    		Block b = this.mc.theWorld.getBlockState(currBlock.pos).getBlock();
    		if (b.equals(Blocks.AIR)){
    			if (!currBlock.hasAirTime()){
	    			logger.debug("It looks like block " + currBlock.pos + " has become air");
	    			currBlock.airTime = System.currentTimeMillis();
	    			continue;
    			}
    		}
    		else if (currBlock.hasAirTime()){
    			logger.debug("It looks like block " + currBlock.pos + " has become solid again");
    			currBlock.airTime = -1;
    			continue;
    		}
    		
    		// Collect drops for this block, making sure that all of them have had at least 100ms of settling time
    		logger.debug("Collecting items related to " + currBlock.toString());
    		ArrayList<DroppedItem> dropMatches = new ArrayList<DroppedItem>();
    		for(Iterator<HashMap.Entry<Integer,DroppedItem>>dropIter = this.newItems.entrySet().iterator(); dropIter.hasNext();){
        		HashMap.Entry<Integer,DroppedItem> dropEntry = dropIter.next();
        		DroppedItem currDrop = dropEntry.getValue();
        		if (!currBlock.pos.equals(currDrop.sourcePos)){ continue; }
        		if (System.currentTimeMillis() - currDrop.createdTime < 100){
        			dropMatches.clear();
        			break; 
        		}
    			dropMatches.add(currDrop);
    			logger.debug("adding " + currDrop.id + "[" + currDrop.drop + "]");
    		}
    		
    		// If we've successfully collected drops for this block, add it to the database and remove it and the drops from their respective lists
    		if (!dropMatches.isEmpty()){
    			HashMap<String,Integer> dropCounts = new HashMap<String,Integer>();
    			// Group drops and remove them from newItems
    			for(DroppedItem d : dropMatches){
    				if (dropCounts.containsKey(d.drop)){
    					dropCounts.put(d.drop, dropCounts.get(d.drop)+1);
    				}
    				else{
    					dropCounts.put(d.drop, 1);
    				}
    				this.newItems.remove(d.id);
    			}
    			
    			String drops = "";
    			for(Iterator<HashMap.Entry<String,Integer>>dIter = dropCounts.entrySet().iterator(); dIter.hasNext();){
            		HashMap.Entry<String,Integer> dEntry = dIter.next();
            		String dropName = dEntry.getKey();
            		int dropCount = dEntry.getValue();
            		if (drops.isEmpty()){
            			drops = dropCount + "x" + dropName; 
            		}
            		else{
            			drops = drops + "," + dropCount + "x" + dropName; 
            		}
    			}
    			
    			this.addBlockToDB(currBlock, drops);
    			this.addBlockToTextFile(currBlock, drops);
    			
    			blockIter.remove();
    		}
    		
    	}
    }
}