package gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import wafflestomper.logbot.LogBotRecordsRetrievedEvent;
import wafflestomper.logbot.database.DBThread;
import wafflestomper.logbot.database.Record;
import wafflestomper.logbot.database.RecordChest;
import wafflestomper.wafflecore.WaffleCore;

public class GuiScreenChestSearch extends GuiScreen{
	
	private GuiTextField searchField;
	private static int searchFieldWidth = 160;
	private long requestID = 0;
	private static Logger logger = LogManager.getLogger("LogBot:GuiChestSearch");
	private static DBThread db = DBThread.INSTANCE;
	private static WaffleCore wafflecore = WaffleCore.INSTANCE;
	private List<RecordChest> searchResults  = new ArrayList<RecordChest>();
	private boolean noRecordsReceived = false;
	private GuiChestPositionList chestPositionList;
	private GuiChestContentsList chestContentsList;
	
	
	public void initGui(){
		this.searchField = new GuiTextField(0, this.fontRendererObj, this.width/2-searchFieldWidth/2, 30, searchFieldWidth, 20);
		this.searchField.setFocused(true);
		this.chestPositionList = new GuiChestPositionList(this);
		this.chestContentsList = new GuiChestContentsList(this);
	}
	
	
	protected void keyTyped(char typedChar, int keyCode) throws IOException{
		super.keyTyped(typedChar, keyCode);
		if (this.searchField.isFocused() && keyCode == 28){
			// Enter pressed in search field
			if (!this.searchField.getText().isEmpty()){
				this.noRecordsReceived = false;
				this.searchResults.clear();
				// Send request
				this.requestID = System.currentTimeMillis();
				try {
					String searchTerm = "%" + this.searchField.getText() + "%";
					String serverIP = wafflecore.worldInfo.getNiceServerIP();
					String worldName = wafflecore.worldInfo.getWorldName();
		    		logger.warn("Adding request to queue");
					db.toDBqueue.put(new RecordChest(serverIP, worldName, 10, searchTerm, this.requestID));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else{
			this.searchField.textboxKeyTyped(typedChar, keyCode);
		}
	}
	
	
	public String getSearchTerm(){
		return(this.searchField.getText());
	}
	
	
	public void ChestRecordsReceived(LogBotRecordsRetrievedEvent event){
		if (event.getRequestID() != this.requestID){
			return;
		}
		if (event.getRecords().size() < 1){
			this.noRecordsReceived = true;
			logger.warn("No results for query...");
			return;
		}
		logger.warn("Got " + event.getRecords().size() + " records");
		for (Record r : event.getRecords()){
			this.searchResults.add((RecordChest)r);
		}
		this.chestPositionList.addRecords(this.searchResults);
	}
	
	
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException{
		super.mouseClicked(mouseX, mouseY, mouseButton);
    	this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
    	if (this.chestPositionList != null){
    		this.chestPositionList.mouseClicked(mouseX, mouseY, mouseButton);
    	}
    }
	
	
    /**
     * Handles mouse input.
     */
    public void handleMouseInput() throws IOException{
        super.handleMouseInput();
        if (this.chestPositionList != null){
        	this.chestPositionList.handleMouseInput();
        }
    }
    
    
	public void updateScreen(){
        super.updateScreen();
        this.searchField.updateCursorCounter();
    }
    
	
    public void drawScreen(int mouseX, int mouseY, float partialTicks){
    	super.drawScreen(mouseX, mouseY, partialTicks);
    	this.drawBackground(0);
    	this.chestPositionList.drawScreen(mouseX, mouseY, partialTicks);
    	this.chestContentsList.drawScreen(mouseX, mouseY, partialTicks);
    	this.searchField.drawTextBox();
    	this.drawCenteredString(this.fontRendererObj, "Chest Search Tool", this.width/2, 10, 0xFFFFFF);	
    }
    
    
    public void chestPosSlotClicked(int index){
    	if (index < this.searchResults.size()){
    		this.chestContentsList.setContents(this.searchResults.get(index).contents);
    	}
    }
}
