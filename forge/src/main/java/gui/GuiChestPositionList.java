package gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiListExtended;
import wafflestomper.logbot.database.RecordChest;

public class GuiChestPositionList extends GuiListExtended{
	
	private static final Minecraft mc = Minecraft.getMinecraft();
	private GuiListExtended.IGuiListEntry[] listEntries = new GuiListExtended.IGuiListEntry[0];
    private int maxListLabelWidth;
    private final GuiScreenChestSearch parent;
    private static final int boxWidth = 200;
    private static final int boxTop = 60;
    private static final int boxBottom = 10;
    private static final int slotHeight = 20;

    
	public GuiChestPositionList(GuiScreenChestSearch _parent) {
		super(mc, boxWidth, _parent.height, boxTop, _parent.height-boxBottom, slotHeight);
		this.parent = _parent;
		this.centerListVertically = false;
		this.setSlotXBoundsFromLeft(_parent.width / 2 - 4 - boxWidth);
        this.registerScrollButtons(7, 8);
	}
	
	private class ChestDistance{
		public double distance;
		public RecordChest record;
		public ChestDistance(double _distance, RecordChest _record){
			this.distance = _distance;
			this.record = _record;
		}
		/*
		@Override
		public int compareTo(ChestDistance otherChest) {
			return((int)((this.distance-otherChest.distance)*100000));
		}
		*/
		
		
	}
	
	public static final Comparator<ChestDistance> distanceComparitor = new Comparator<ChestDistance>(){
		@Override
		public int compare(ChestDistance chest1, ChestDistance chest2){
			return((int)((chest1.distance-chest2.distance)*100000));
		}
	};

	public void addRecords(List<RecordChest> _records){
		this.selectedElement = -1;
		
		// Sort the records according to their distance
		ArrayList<ChestDistance> distances = new ArrayList<ChestDistance>();
		for (RecordChest r : _records){
			double currDistance = this.mc.player.getDistance(r.x, r.y, r.z);
			distances.add(new ChestDistance(currDistance, r));
		}
		distances.sort(this.distanceComparitor);
		
		// Assemble listEntries, respecting the distances
		GuiListExtended.IGuiListEntry[] newEntries = new GuiListExtended.IGuiListEntry[_records.size()];
		int i = 0;
		for (ChestDistance c : distances){
			System.out.println(c.distance);
			newEntries[i++] = new GuiChestPositionList.ChestPosEntry(c.record.x, c.record.y, c.record.z, c.distance);
		}
		this.listEntries = newEntries;
	}
    
    protected int getScrollBarX(){
        return this.right - 6;
    }
    

	@Override
	public IGuiListEntry getListEntry(int index) {
		return this.listEntries[index];
	}
	

	@Override
	protected int getSize() {
		return this.listEntries.length;
	}
	
	
    public int getListWidth(){
        return this.width;
    }
    
    
    public boolean isSelected(int slotIndex){
    	return this.selectedElement == slotIndex;
    }
    
    
    public void slotClicked(int slotIndex){
    	this.parent.chestPosSlotClicked(slotIndex);
    	this.selectedElement = slotIndex;
    }
    
    
    public class ChestPosEntry implements GuiListExtended.IGuiListEntry{
    	
        private final int posX;
        private final int posY;
        private final int posZ;
        private final String distance;

        
        public ChestPosEntry(int x, int y, int z, double _distance){
        	this.posX = x;
        	this.posY = y;
        	this.posZ = z;
        	String dist = String.valueOf((int)_distance);
        	while(Minecraft.getMinecraft().fontRendererObj.getStringWidth(dist) > 32){
        		dist = dist.substring(0, dist.length()-1);
        	}
        	this.distance = "(" + dist + "m)";
        }
        

        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected){
        	int yPos = y + slotHeight - GuiChestPositionList.mc.fontRendererObj.FONT_HEIGHT - 1;
        	FontRenderer f = Minecraft.getMinecraft().fontRendererObj;
        	f.drawString("X: " + this.posX, x+4, yPos, 0xFFFFFF);
        	f.drawString("Y: " + this.posY, x+4+50, yPos, 0xFFFFFF);
        	f.drawString("Z: " + this.posZ, x+4+50+40, yPos, 0xFFFFFF);
        	f.drawString(this.distance, x+listWidth-8-f.getStringWidth(this.distance), yPos, 0xFFFFFF);
        	
        }
        

        /**
         * Called when the mouse is clicked within this entry. Returning true means that something within this entry was
         * clicked and the list should not be dragged.
         */
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY){
        	GuiChestPositionList.this.slotClicked(slotIndex);
            return false;
        }
        
        
        public boolean isSelected(int slotIndex){
            return false;
        }


        /**
         * Fired when the mouse button is released. Arguments: index, x, y, mouseEvent, relativeX, relativeY
         */
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY){}
        

		@Override
		public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {}
    }
}
