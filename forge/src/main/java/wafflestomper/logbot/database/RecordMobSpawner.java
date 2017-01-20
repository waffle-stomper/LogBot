package wafflestomper.logbot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import wafflestomper.logbot.util.Printer;

public class RecordMobSpawner implements Record{
	
	public static final String createTableStatement =  	"CREATE TABLE IF NOT EXISTS MOBSPAWNERS " +
														"(ID			INTEGER		PRIMARY KEY ASC, " +
														"TIMESTAMP		DATETIME 	DEFAULT CURRENT_TIMESTAMP, " +	
														"WORLDNAME		TEXT    	NOT NULL, " + 
														"WORLDID 		INTEGER     NOT_NULL, " +
														"BLOCKTYPE		TEXT     	NOT NULL, " +
														"X				INTEGER     NOT NULL, " + 
														"Y				INTEGER     NOT NULL, " + 
														"Z				INTEGER		NOT NULL, " + 
														"NOTES			TEXT)"; 
	public String serverName;
	public String worldName;
	public int worldID;
	public String timestamp;
	public String blockType;
	public int x;
	public int y;
	public int z;
	public String notes;
	private boolean isRequest;
	private int requestLimit = 1; // How many records should be returned
	private long createdTime = System.currentTimeMillis();
	
	
	/**
	 * Constructor for inserting blocks into the database
	 */
	public RecordMobSpawner(String _serverName, String _worldName, int _worldId, String _blockType, int _x, int _y, int _z, String _notes){
		this.timestamp = DBThread.getUTCTimestamp();
		this.serverName = _serverName;
		this.worldName = _worldName;
		this.worldID = _worldId;
		this.blockType = _blockType;
		this.x = _x;
		this.y = _y;
		this.z = _z;
		this.notes = _notes;
		this.isRequest = false;
	}
	

	@Override
	public void insertRecord(Connection c){
		PreparedStatement prep = null;
		try {
			prep = c.prepareStatement("INSERT INTO MOBSPAWNERS (TIMESTAMP, WORLDNAME, WORLDID, BLOCKTYPE, X, Y, Z, NOTES) " +
					 	    		  "VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
			prep.setString(1, this.timestamp);
			prep.setString(2, this.worldName);
			prep.setInt(3, this.worldID);
			prep.setString(4, this.blockType);
			prep.setInt(5, this.x);
			prep.setInt(6, this.y);
			prep.setInt(7, this.z);
			prep.setString(8, this.notes);
			prep.execute();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
			Printer.gamePrint("\u00A7cDatabase spawner insert failed!");
		}
	}


	@Override
	public String getServerName() {
		return(this.serverName);
	}


	@Override
	public boolean isRequest() {
		return(this.isRequest);
	}


	@Override
	public void requestRecord(Connection c) {
		//TODO: Write this!
		System.out.println("THIS HAS NOT BEEN IMPLEMENTED YET. BAD THINGS MIGHT HAPPEN");
	}
	
	
	@Override
	public long getTimeExisted(){
		return(System.currentTimeMillis()-this.createdTime);
	}
}
