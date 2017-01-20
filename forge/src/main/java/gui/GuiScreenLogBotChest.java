package gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import wafflestomper.logbot.LogBot;
import wafflestomper.logbot.LogBotRecordsRetrievedEvent;
import wafflestomper.logbot.database.DBThread;
import wafflestomper.logbot.database.Record;
import wafflestomper.logbot.database.RecordChest;
import wafflestomper.logbot.util.ConfigManager;
import wafflestomper.logbot.util.Printer;
import wafflestomper.wafflecore.WaffleCore;

public class GuiScreenLogBotChest extends GuiContainer{

    /** The ResourceLocation containing the chest GUI texture. */
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    private final IInventory upperChestInventory;
    private final IInventory lowerChestInventory;
    /** window height is calculated with these values; the more rows, the higher it is */
    private final int inventoryRows;    
    private boolean minecartChest = false;
    private BlockPos chestPos;
    private BlockPos chestPos2;
    private static final Logger logger = LogManager.getLogger("LogBot:GuiLogBotChest");
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final WaffleCore wafflecore = WaffleCore.INSTANCE;
    private ArrayList<DumbItemStack> dbChest = new ArrayList<DumbItemStack>();
    private static ConfigManager config = LogBot.getConfigManager();
    private static DBThread db = DBThread.INSTANCE;
    private long dbRequestID = 0;
    
    private static final int RED = 0xAAFF0000;
    private static final int GREEN = 0xAA00FF00;
    private static final int BLUE = 0xAA0000FF;
    
    public GuiScreenLogBotChest(IInventory upperInv, IInventory lowerInv){
        super(new ContainerChest(upperInv, lowerInv, Minecraft.getMinecraft().player));
        this.upperChestInventory = upperInv;
        this.lowerChestInventory = lowerInv;
        this.allowUserInput = false;
        int i = 222;
        int j = 114;
        this.inventoryRows = lowerInv.getSizeInventory() / 9;
        this.ySize = 114 + this.inventoryRows * 18;
        
        // Get the location(s) of the chest block(s)
        RayTraceResult currMOP = mc.objectMouseOver;
		if (currMOP != null){
			BlockPos cPos = currMOP.getBlockPos();
			if (cPos == null && currMOP.entityHit != null && currMOP.entityHit instanceof EntityMinecartChest){
				cPos = currMOP.entityHit.getPosition();
				this.minecartChest = true;
			}
			if (cPos != null){
				if (this.inventoryRows == 3){
					// This is a single chest, so we set this.chestPos to the location of the chest and leave this.chestPos2 null
					this.chestPos = cPos;
				}
				else if (this.inventoryRows == 6){
					// This is a double chest. The most northwestern block location will go into this.chestPos 
					// and the other one will go into this.chestPos2
	    			if (mc.world.getBlockState(cPos.north()).getBlock().getUnlocalizedName().equals("tile.chest")){
	    				this.chestPos = cPos.north();
	    				this.chestPos2 = cPos;
	    			}
	    			else if (this.mc.world.getBlockState(cPos.west()).getBlock().getUnlocalizedName().equals("tile.chest")){
	    				this.chestPos = cPos.west();
	    				this.chestPos2 = cPos;
	    			}
	    			else if (this.mc.world.getBlockState(cPos.south()).getBlock().getUnlocalizedName().equals("tile.chest")){
	    				this.chestPos = cPos;
	    				this.chestPos2 = cPos.south();
	    			}
	    			else{
	    				this.chestPos = cPos;
	    				this.chestPos2 = cPos.east();
	    			}
				}
			}
			else{
				logger.error("Couldn't get chest pos");
				Printer.gamePrint("\u00A7cCouldn't get chest pos");
				return;
			}
		}
		
		// Request the last known chest data from the database
		if (this.chestPos != null){
			String serverName = wafflecore.worldInfo.getNiceServerIP();
			String worldName = wafflecore.worldInfo.getWorldName();
			try {
				this.dbRequestID = System.currentTimeMillis();
				DBThread.toDBqueue.put(new RecordChest(serverName, worldName, this.chestPos, this.minecartChest, 5, this.dbRequestID));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
    }
    
    
    /**
     * Initially I was catching the event here, but it would fire multiple times and I didn't have the energy
     * to debug it, so I moved it to the main class
     */
    public void chestRecordsReceived(LogBotRecordsRetrievedEvent event){
    	List<Record> rChests = event.getRecords();
    	if (rChests.isEmpty() || event.getRequestID() != this.dbRequestID){ 
    		return; 
    	}
    	RecordChest rChest = (RecordChest)rChests.get(0);
		BlockPos pos = new BlockPos(rChest.x, rChest.y, rChest.z);
		if (pos.equals(this.chestPos)){
			String splitter = "(?<!\\\\)" + Pattern.quote(",");
			// __endhelper__ added to make sure that empty trailing slots aren't ignored
			String[] dbRawSlots = rChest.contents.concat(",__endhelper__").split(splitter);
			for (int slotPos=0; slotPos < dbRawSlots.length-1; slotPos++){
    			String dbItem = "";
    			int dbCount = 0;
    			if (dbRawSlots[slotPos] != null && !dbRawSlots[slotPos].isEmpty()){
    				int delimPos = dbRawSlots[slotPos].indexOf('x');
    				if (delimPos > 0){
    					dbCount = Integer.valueOf(dbRawSlots[slotPos].substring(0, delimPos));
    					dbItem = dbRawSlots[slotPos].substring(delimPos+1,dbRawSlots[slotPos].length());
    					logger.debug("+++ Got " + dbCount + " of " + dbItem);
    					this.dbChest.add(new DumbItemStack(dbItem, dbCount));
    					continue;
    				}
    			}
    			// If the control flow falls to here, the slot is presumed empty
    			this.dbChest.add(null);
    		}
		}
    }

    
    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items)
     */
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY){
        this.fontRendererObj.drawString(this.lowerChestInventory.getDisplayName().getUnformattedText(), 8, 6, 4210752);
        this.fontRendererObj.drawString(this.upperChestInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);
    }
    
    
    /**
     * Highlights the chosen slot with the chosen color
     * Used for displaying slots that have changed since the last time the player accessed the chest
     */
    private void highlightSlot(int slotNum, int color){
		Slot slot = this.inventorySlots.getSlot(slotNum);
		GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        int j1 = slot.xPos;// .xDisplayPosition;
        int k1 = slot.yPos;// yDisplayPosition;
        GlStateManager.colorMask(true, true, true, false);
        drawRect(j1, k1, j1+16, k1+16, color);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
    }
    
    
    /**
     * Draws the background layer of this container (behind the items).
     * Also updates the tooltip cache
     */
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY){
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.inventoryRows * 18 + 17);
        this.drawTexturedModalRect(i, j + this.inventoryRows * 18 + 17, 0, 126, this.xSize, 96);
        
        // Highlight any slots that have changed
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)i, (float)j, 0.0F);
		for (int slotNum=0; slotNum < this.dbChest.size() && slotNum < this.lowerChestInventory.getSizeInventory(); slotNum++){
			Slot currSlot = this.inventorySlots.getSlot(slotNum);
			ItemStack currStack = currSlot.getStack();
			DumbItemStack dbStack = this.dbChest.get(slotNum);
			/*
			 * States:
			 * - Empty and now occupied - green
			 * - occupied and now empty - red
			 * - Increase same item - green
			 * - Decrease same item - red
			 * - Change item - blue
			 */
			if (dbStack == null && currStack == null){
				continue;
			}
			else if (dbStack == null && currStack != null){
				// Empty -> occupied
				this.highlightSlot(slotNum, GREEN);
			}
			else if (dbStack != null && currStack == null){
				// Occupied -> Empty
				this.highlightSlot(slotNum, RED);
			}
			else{
				String currItem = LogBot.getDetailedItemName(currStack);
				int currCount = currStack.getCount();
				String dbItem = dbStack.item;
				int dbCount = dbStack.count;
				if (currItem.equals(dbItem)){
					if (currCount > dbCount){
						// Items are the same but the count has increased
						this.highlightSlot(slotNum, GREEN);
					}
					else if (currCount < dbCount){
						// Items are the same but the count has decreased
						this.highlightSlot(slotNum, RED);
					}
				}
				else{
					// The type of item has changed
					this.highlightSlot(slotNum, BLUE);
				}
			}
    	}
		GlStateManager.popMatrix();
    }
    
    
    /**
     * Returns a CSV representation of the chest contents
     * @return
     */
	public String inventoryToCSV(){
		String contents = "";
		for(int i=0; i<this.lowerChestInventory.getSizeInventory(); i++){
			ItemStack currStack = this.lowerChestInventory.getStackInSlot(i);
			if (currStack != null){
				contents = contents + String.valueOf(currStack.getCount()) + "x" + LogBot.getDetailedItemName(currStack);
			}
			if  (i < this.lowerChestInventory.getSizeInventory()-1){
				contents = contents + ",";
			}
		}
		return(contents);
	}
    
	
    @Override
    public void onGuiClosed()
    {
    	if (this.chestPos == null || (this.inventoryRows !=3 && this.inventoryRows != 6)){
    		return;
    	}
    	// Compile chest contents into list of strings
		String contents = inventoryToCSV();
		// Dump to text file
		if (config.logToTextFiles){
    		String suffix = "";
    		if (chestPos != null){
    			suffix = "chestposxyz_" + chestPos.getX() + "_" + chestPos.getY() + "_" + chestPos.getZ();
    		}
    		else{
    			suffix = "playerposxyz_" + (int)this.mc.player.posX + "_" + (int)this.mc.player.posY + "_" + (int)this.mc.player.posZ;
    		}
    		LogBot.writeFile(contents, "chests" + File.separator + this.mc.player.getName() + "_" + suffix + ".txt");
		}
		
		// Write to DB
		if (config.logToDB){
    		String serverName = wafflecore.worldInfo.getNiceServerIP();
	    	String worldName = wafflecore.worldInfo.getWorldName();
	    	logger.debug("Writing entry for world: " + WaffleCore.worldInfo.getWorldName());
	    	long first = System.currentTimeMillis();
	    	try {
				db.toDBqueue.put(new RecordChest(serverName, worldName, chestPos.getX(), chestPos.getY(), chestPos.getZ(), this.minecartChest, contents, ""));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
    	super.onGuiClosed();
    }
    
    
    /** Removes the default tooltip */
    @Override
    protected void renderToolTip(ItemStack stack, int x, int y){}
    
    
    private static List<String> makeDBToolTip(DumbItemStack dbStack, ItemStack currStack){
    	ArrayList<String> out = new ArrayList<String>();
    	out.add(TextFormatting.GRAY + "Was:");
    	if (dbStack == null){
    		out.add(TextFormatting.WHITE + " Empty");
    	}
    	else{
    		String[] dbSplit = dbStack.item.split("\\|");
    		if (dbSplit.length < 1){
    			out.add("");
    		}
    		else{
    			out.add(TextFormatting.WHITE + " " + String.valueOf(dbStack.count) + " x " + dbSplit[0]);
    			for (int sPos=1; sPos<dbSplit.length; sPos++){
    				out.add(TextFormatting.WHITE + "  " + dbSplit[sPos]);
    			}
    		}
    	}
		out.add(TextFormatting.GRAY + "Now:");
		if (currStack == null){
    		out.add(TextFormatting.WHITE + " Empty");
    	}
    	else{
    		//out.add(TextFormatting.WHITE + " " + String.valueOf(currStack.stackSize) + " x " + LogBot.getDetailedItemName(currStack));
    		String[] currSplit = LogBot.getDetailedItemName(currStack).split("\\|");
    		if (currSplit.length < 1){
    			out.add("");
    		}
    		else{
    			out.add(TextFormatting.WHITE + " " + String.valueOf(currStack.getCount()) + " x " + currSplit[0]);
    			for (int sPos=1; sPos<currSplit.length; sPos++){
    				out.add(TextFormatting.WHITE + "  " + currSplit[sPos]);
    			}
    		}
    	}
    	return(out);
    }
    
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
    	super.drawScreen(mouseX, mouseY, partialTicks);
    	
    	// Draw tooltip and/or database information
    	InventoryPlayer inventoryplayer = this.mc.player.inventory;
    	for (int slotNum=0; slotNum < this.inventorySlots.inventorySlots.size(); slotNum++){
    		Slot slot = this.inventorySlots.getSlot(slotNum);
    		if (this.isPointInRegion(slot.xPos, slot.yPos, 16, 16, mouseX, mouseY) && slot.canBeHovered()){
    			List<String> toolTip = new ArrayList<String>();
    			FontRenderer font = null;
    			ItemStack currStack = null;
    			// Standard tooltip
		    	if (inventoryplayer.getItemStack() == null && slot != null && slot.getHasStack()){
		    		currStack = slot.getStack();
		        	font = currStack.getItem().getFontRenderer(currStack);
		            toolTip = currStack.getTooltip(this.mc.player, this.mc.gameSettings.advancedItemTooltips);

		            for (int i = 0; i < toolTip.size(); ++i){
		                if (i == 0){
		                	toolTip.set(i, currStack.getRarity().rarityColor + (String)toolTip.get(i));
		                }
		                else{
		                	toolTip.set(i, TextFormatting.GRAY + (String)toolTip.get(i));
		                }
		            }
		        }
		    	
		    	if (slotNum < this.dbChest.size()){
		    		DumbItemStack dbStack = this.dbChest.get(slotNum);
		    		
		    		int oldCount = 0;
	    			int newCount = 0;
	    			String oldItem = "";
	    			String newItem = "";
	    			
	    			if (dbStack != null){
	    				oldCount = dbStack.count;
	    				oldItem = dbStack.item;
	    			}
	    			if (currStack != null){
	    				newCount = currStack.getCount();
	    				newItem = LogBot.getDetailedItemName(currStack);
	    			}
	    			
		    		if (oldCount != newCount || oldItem != newItem){
	    				if (toolTip.size() > 0){
	    		            toolTip.add("");
	    				}
	    				toolTip.addAll(makeDBToolTip(dbStack, currStack));
		    		}
		    	}
		    	this.drawHoveringText(toolTip, mouseX, mouseY, (font == null ? fontRendererObj : font));
		    	break;
	    	}
    	}

    }
    
    
    private class DumbItemStack{
    	public String item;
    	public int count;
    	public DumbItemStack(String _item, int _count){
    		this.item = _item;
    		this.count = _count;
    	}
    }
}
