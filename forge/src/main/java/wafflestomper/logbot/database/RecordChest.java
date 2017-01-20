package wafflestomper.logbot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import wafflestomper.logbot.LogBotRecordsRetrievedEvent;
import wafflestomper.logbot.util.Printer;

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
	private int requestLimit = 1; // How many records should be returned
	private String searchTerm = "";
	private long requestID = 0;
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
	 * Simplified blockpos constructor for making requests about a specific position
	 */
	public RecordChest(String _serverName, String _worldName, BlockPos _pos, boolean _minecartChest, int _requestLimit, long _requestID){
		this(_serverName, _worldName, _pos, _minecartChest, "", "");
		this.isRequest = true;
		this.requestLimit = _requestLimit;
		this.requestID = _requestID;
	}
	
	
	/**
	 * Simplified constructor for making requests with a search term
	 */
	public RecordChest(String _serverName, String _worldName, int _requestLimit, String _searchTerm, long _requestID){
		this(_serverName, _worldName, new BlockPos(0,0,0), false, "", "");
		this.isRequest = true;
		this.requestLimit = _requestLimit;
		this.searchTerm = _searchTerm;
		this.requestID = _requestID;
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
			prep.setBoolean(6, this.minecartChest);
			prep.setString(7, this.contents);
			prep.setString(8, this.notes);
			prep.execute();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
			Printer.gamePrint("\u00A7cDatabase block insert failed!");
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
	
	
	public void setTimestamp(String newTimestamp){
		this.timestamp = newTimestamp;
	}


	/**
	 * Tries to retrieve records matching the data supplied, and if it's successful, an event is posted with the result(s)
	 * Note that this is intented to be used by the DB thread, not a mod directly.
	 * If you want a chest record, add a new RecordChest object to the queue with the isRequest flag set to true
	 * If it's successful, an event will be posted on the Forge event bus with the most recent [resultLimit] results
	 */
	@Override
	public void requestRecord(Connection c) {
		List<Record> results = new ArrayList<Record>();
		
		try{
			PreparedStatement prep = null;
			if (this.searchTerm.isEmpty()){
				// Search for a chest by its x,y,z co-ordinates
				prep = c.prepareStatement("SELECT * FROM CHESTS WHERE WORLDNAME == ? AND X == ? AND Y == ? AND Z == ? " +
															"AND MINECART == ? ORDER BY ID DESC LIMIT ?;");
				prep.setString(1, this.worldName);
				prep.setInt(2, this.x);
				prep.setInt(3, this.y);
				prep.setInt(4, this.z);
				prep.setBoolean(5, this.minecartChest);
				prep.setInt(6, this.requestLimit);
			}
			else{
				// Search for an item within the chests, returning the most recent result for each unique chest
				//SELECT * FROM CHESTS WHERE CONTENTS LIKE '%diamond%' GROUP BY X || '-' || Y ||'-' || Z;
				
				prep = c.prepareStatement("SELECT * FROM CHESTS WHERE WORLDNAME == ? AND CONTENTS LIKE ? GROUP BY X || '-' || Y ||'-' || Z ORDER BY ID DESC LIMIT ?;");
				prep.setString(1, this.worldName);
				prep.setString(2, this.searchTerm);
				prep.setInt(3, this.requestLimit);
			}
			
			//System.out.println("executing query");
			ResultSet rs = prep.executeQuery();
			while(rs.next()){
				int x = rs.getInt("X");
				int y = rs.getInt("Y");
				int z = rs.getInt("Z");
				String contents = rs.getString("CONTENTS");
				boolean minecartChest = rs.getBoolean("MINECART");
				String timestamp = rs.getString("TIMESTAMP");
				//System.out.println(timestamp + " " + x + "," + y + "," + z);
				results.add(new RecordChest(this.serverName, this.worldName, x, y, z, minecartChest, contents, ""));
				int id = rs.getInt("ID");
				//System.out.println(id + ">>>> CONTENTS: " + contents);
			}
			rs.close();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
			Printer.gamePrint("\u00A7cDatabase chest request failed!");
		}
		
		LogBotRecordsRetrievedEvent lbrre = new LogBotRecordsRetrievedEvent(results, this.requestID);
		MinecraftForge.EVENT_BUS.post(lbrre);
	}
	
	
	@Override
	public long getTimeExisted(){
		return(System.currentTimeMillis()-this.createdTime);
	}
}
