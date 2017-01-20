package wafflestomper.logbot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.minecraft.util.text.TextFormatting;
import wafflestomper.logbot.util.Printer;

public class RecordNote implements Record{
	public static final String createTableStatement =  	"CREATE TABLE IF NOT EXISTS NOTES " +
			"(ID			INTEGER		PRIMARY KEY ASC, " +
			"TIMESTAMP		DATETIME 	DEFAULT CURRENT_TIMESTAMP, " +	
			"WORLDNAME		TEXT    	NOT NULL, " + 
			"X				REAL		NOT NULL, " + 
			"Y				REAL		NOT NULL, " + 
			"Z				REAL		NOT NULL, " + 
			"NOTE			TEXT)"; 
	public String serverName;
	public String worldName;
	public String timestamp;
	public double x;
	public double y;
	public double z;
	public String note;
	private boolean isRequest;
	private int requestLimit = 1; // How many records should be returned
	private long createdTime = System.currentTimeMillis();
	
	
	/**
	 * Constructor for inserting notes into the database
	 */
	public RecordNote(String _serverName, String _worldName, double _x, double _y, double _z, String _note){
		this.timestamp = DBThread.getUTCTimestamp();
		this.serverName = _serverName;
		this.worldName = _worldName;
		this.x = _x;
		this.y = _y;
		this.z = _z;
		this.note = _note;
		this.isRequest = false;
	}	
	
	
	@Override
	public void insertRecord(Connection c){
		PreparedStatement prep = null;
		try {
			prep = c.prepareStatement("INSERT INTO NOTES (TIMESTAMP, WORLDNAME, X, Y, Z, NOTE) " +
					 	    		  "VALUES (?, ?, ?, ?, ?, ?);");
			prep.setString(1, this.timestamp);
			prep.setString(2, this.worldName);
			prep.setDouble(3, this.x);
			prep.setDouble(4, this.y);
			prep.setDouble(5, this.z);
			prep.setString(6, this.note);
			prep.execute();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
			Printer.gamePrint(TextFormatting.RED + "Database note insert failed!");
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
