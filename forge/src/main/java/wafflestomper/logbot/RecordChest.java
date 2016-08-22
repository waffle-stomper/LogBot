package wafflestomper.logbot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;

public class RecordChest implements Record{
	
	public static final String createTableStatement = 	"CREATE TABLE IF NOT EXISTS CHESTS " +
														"(ID			INTEGER		PRIMARY KEY ASC, " +
														"TIMESTAMP		DATETIME 	DEFAULT CURRENT_TIMESTAMP, " +	
														"WORLDNAME		TEXT		NOT NULL, " +
														"X				INTEGER		NOT NULL, " + 
														"Y				INTEGER		NOT NULL, " + 
														"Z				INTEGER		NOT NULL, " + 
														"MINECART		INTEGER		NOT NULL, " + 
														"CONTENTS		TEXT, " +
														"NOTES			TEXT)"; 
	public String serverName;
	public String worldName;
	public String timestamp;
	public int x;
	public int y;
	public int z;
	public boolean minecartChest;
	public String contents;
	public String notes;
	private boolean isRequest = false;
	private long createdTime = System.currentTimeMillis();
	
	
	/**
	 * Standard insert record constructor
	 */
	public RecordChest(String _serverName, String _worldName, int _x, int _y, int _z, boolean _minecartChest, String _contents, String _notes){
		this.timestamp = DBThread.getUTCTimestamp();
		this.serverName = _serverName;
		this.worldName = _worldName;
		this.x = _x;
		this.y = _y;
		this.z = _z;
		this.minecartChest = _minecartChest;
		this.contents = _contents;
		this.notes = _notes;
	}
	
	
	/**
	 * BlockPos insert record constructor
	 */
	public RecordChest(String _serverName, String _worldName, BlockPos _pos, boolean _minecartChest, String _contents, String _notes){
		this(_serverName, _worldName, _pos.getX(), _pos.getY(), _pos.getZ(), _minecartChest, _contents, _notes);
	}
	
	
	/**
	 * Simplified blockpos constructor for making requests
	 */
	public RecordChest(String _serverName, String _worldName, BlockPos _pos, boolean _minecartChest){
		this(_serverName, _worldName, _pos, _minecartChest, "", "");
		this.isRequest = true;
	}
	
	
	@Override
	public void insertRecord(Connection c){
		try {
			PreparedStatement prep = null;
			prep = c.prepareStatement("INSERT INTO CHESTS (TIMESTAMP, WORLDNAME, X, Y, Z, MINECART, CONTENTS, NOTES) " + 
									  "VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
			prep.setString(1, this.timestamp);
			prep.setString(2, this.worldName);
			prep.setInt(3, this.x);
			prep.setInt(4, this.y);
			prep.setInt(5, this.z);
			prep.setInt(6, this.minecartChest?1:0);
			prep.setString(7, this.contents);
			prep.setString(8, this.notes);
			prep.execute();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
			Minecraft.getMinecraft().thePlayer.addChatMessage(new TextComponentString("\u00A7cDatabase block insert failed!"));
		}
	}


	@Override
	public String getServerName() {
		return(this.serverName);
	}
	
	
	@Override
	public boolean isRequest(){
		return(this.isRequest);
	}


	/**
	 * Tries to retrieve a record matching the data supplied, and if it's successful, an event is posted with the result
	 * Note that this is intented to be used by the DB thread, not a mod directly.
	 * If you want a chest record, add a new RecordChest object to the queue with the isRequest flag set to true
	 * If it's successful, an event will be posted on the Forge event bus with the most recent result
	 */
	@Override
	public void requestRecord(Connection c) {
		String contents = null;
		try{
			PreparedStatement prep = c.prepareStatement("SELECT * FROM CHESTS WHERE WORLDNAME == ? AND X == ? AND Y == ? AND Z == ? " +
														"AND MINECART == ? ORDER BY ID DESC LIMIT 1;");
			prep.setString(1, this.worldName);
			prep.setInt(2, this.x);
			prep.setInt(3, this.y);
			prep.setInt(4, this.z);
			prep.setInt(5, this.minecartChest?1:0);
			ResultSet rs = prep.executeQuery();
			if (rs.next()){
				contents = rs.getString("CONTENTS");
				//int id = rs.getInt("ID");
				//System.out.println(id + ">>>>>>>>>>>>>>>>>>>>>>>>>> CONTENTS: " + contents);
			}
			rs.close();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
			Minecraft.getMinecraft().thePlayer.addChatMessage(new TextComponentString("\u00A7cDatabase chest request failed!"));
		}
		
		if (contents != null){
			RecordChest result = new RecordChest(this.serverName, this.worldName, this.x, this.y, this.z, this.minecartChest, contents, "");
			//System.out.println("Posting event");
			LogBotRecordRetrievedEvent lbrre = new LogBotRecordRetrievedEvent(result);
			MinecraftForge.EVENT_BUS.post(lbrre);
		}
	}
	
	
	@Override
	public long getTimeExisted(){
		return(System.currentTimeMillis()-this.createdTime);
	}
}
