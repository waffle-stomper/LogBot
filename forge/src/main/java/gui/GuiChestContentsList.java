package gui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended;

public class GuiChestContentsList extends GuiListExtended{
	
	private static final Minecraft mc = Minecraft.getMinecraft();
	private GuiListExtended.IGuiListEntry[] listEntries = new GuiListExtended.IGuiListEntry[0];
    private int maxListLabelWidth;
    private final GuiScreenChestSearch parent;
    private static final int boxWidth = 200;
    private static final int boxTop = 60;
    private static final int boxBottom = 10;
    private static final int slotHeight = 20;

    
	public GuiChestContentsList(GuiScreenChestSearch _parent) {
		super(mc, boxWidth, _parent.height, boxTop, _parent.height-boxBottom, slotHeight);
		this.parent = _parent;
		this.centerListVertically = false;
		this.setSlotXBoundsFromLeft(_parent.width / 2 + 4);
        this.registerScrollButtons(7, 8);
	}
	
	
	protected void setContents(String _chestContents){
		this.selectedElement = -1;
		// Split the chest contents CSV into individual slots, and group like items
		String searchTerm = this.parent.getSearchTerm().toLowerCase();
		HashMap<String,Integer> compiledItems = new HashMap<String,Integer>();
		String splitter = "(?<!\\\\)" + Pattern.quote(",");
		// __endhelper__ added to make sure that empty trailing slots aren't ignored
		String[] rawSlots = _chestContents.concat(",__endhelper__").split(splitter);
		for (int slotPos=0; slotPos < rawSlots.length-1; slotPos++){
			if (rawSlots[slotPos] != null && !rawSlots[slotPos].isEmpty()){
				int delimPos = rawSlots[slotPos].indexOf('x');
				if (delimPos > 0){
					String item = rawSlots[slotPos].substring(delimPos+1,rawSlots[slotPos].length());
					if (item.toLowerCase().contains(searchTerm)){
						int count = Integer.valueOf(rawSlots[slotPos].substring(0, delimPos));
						if (compiledItems.containsKey(item)){
							compiledItems.put(item, compiledItems.get(item)+count);
						}
						else{
							compiledItems.put(item, count);
						}
					}
				}
			}
		}
		
		// Add the compiled items to the entries list
		int i = 0;
		GuiListExtended.IGuiListEntry[] newEntries = new GuiListExtended.IGuiListEntry[compiledItems.size()];
		for(Iterator<HashMap.Entry<String,Integer>>iter = compiledItems.entrySet().iterator(); iter.hasNext();){
			HashMap.Entry<String,Integer> ent = iter.next();
			String item = ent.getKey();
			if (item.startsWith("minecraft:")){
				item = item.substring(10);
			}
			int count = ent.getValue();
			newEntries[i++] = new ChestContentsEntry(count, item);
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
    
    
    public class ChestContentsEntry implements GuiListExtended.IGuiListEntry{
    	
        private int count;
        private String description;
        
        
        public ChestContentsEntry(int _count, String _description){
        	this.count = _count;
        	this.description = _description;
        }
        

        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected){
        	int refX = x + 36;
        	Minecraft mc = Minecraft.getMinecraft();
        	int countX = refX - mc.fontRendererObj.getStringWidth(this.count + " x ");
        	while(mc.fontRendererObj.getStringWidth(this.description)+36 > listWidth-5){
        		this.description = this.description.substring(0, this.description.length()-1);
        	}
        	mc.fontRendererObj.drawString(this.count + " x ", countX, y, 0xFFFFFF);
        	mc.fontRendererObj.drawString(this.description, refX, y, 0xFFFFFF);
        }
        

        /**
         * Called when the mouse is clicked within this entry. Returning true means that something within this entry was
         * clicked and the list should not be dragged.
         */
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY){
        	GuiChestContentsList.this.slotClicked(slotIndex);
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
