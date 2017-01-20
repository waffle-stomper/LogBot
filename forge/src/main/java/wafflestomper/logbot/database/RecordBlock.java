package wafflestomper.logbot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import wafflestomper.logbot.util.Printer;

public class RecordBlock implements Record{
	
	public static final String createTableStatement =  	"CREATE TABLE IF NOT EXISTS BLOCKS " +
														"(ID			INTEGER		PRIMARY KEY ASC, " +
														"TIMESTAMP		DATETIME 	DEFAULT CURRENT_TIMESTAMP, " +	
														"WORLDNAME		TEXT    	NOT NULL, " + 
														"BLOCKTYPE		TEXT     	NOT NULL, " +
														"DROPS			TEXT     	NOT NULL, " +
														"X				INTEGER     NOT NULL, " + 
														"Y				INTEGER     NOT NULL, " + 
														"Z				INTEGER		NOT NULL, " + 
														"MINED			INTEGER		NOT NULL, " + 
														"NOTES			TEXT)"; 
	public String serverName;
	public String worldName;
	public String timestamp;
	public String blockType;
	public String drops;
	public int x;
	public int y;
	public int z;
	public boolean mined;
	public String notes;
	private boolean isRequest;
	private int requestLimit = 1; // How many records should be returned
	private long createdTime = System.currentTimeMillis();
	
	
	/**
	 * Constructor for inserting blocks into the database
	 */
	public RecordBlock(String _serverName, String _worldName, String _blockType, String _drops, int _x, int _y, int _z, boolean _mined, String _notes){
		this.timestamp = DBThread.getUTCTimestamp();
		this.serverName = _serverName;
		this.worldName = _worldName;
		this.blockType = _blockType;
		this.drops = _drops;
		this.x = _x;
		this.y = _y;
		this.z = _z;
		this.mined = _mined;
		this.notes = _notes;
		this.isRequest = false;
	}
	

	@Override
	public void insertRecord(Connection c){
		PreparedStatement prep = null;
		try {
			prep = c.prepareStatement("INSERT INTO BLOCKS (TIMESTAMP, WORLDNAME, BLOCKTYPE, DROPS, X, Y, Z, MINED, NOTES) " +
					 	    		  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);");
			prep.setString(1, this.timestamp);
			prep.setString(2, this.worldName);
			prep.setString(3, this.blockType);
			prep.setString(4, this.drops);
			prep.setInt(5, this.x);
			prep.setInt(6, this.y);
			prep.setInt(7, this.z);
			prep.setBoolean(8, this.mined);
			prep.setString(9, this.notes);
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
