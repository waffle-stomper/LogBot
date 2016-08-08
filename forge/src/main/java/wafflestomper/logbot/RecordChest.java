package wafflestomper.logbot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

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
	
	
	public RecordChest(String _serverName, String _worldName, int _x, int _y, int _z, boolean _minecartChest, String _contents, String _notes){
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:22");
		df.setTimeZone(tz);
		this.timestamp = df.format(new Date());
		this.serverName = _serverName;
		this.worldName = _worldName;
		this.x = _x;
		this.y = _y;
		this.z = _z;
		this.minecartChest = _minecartChest;
		this.contents = _contents;
		this.notes = _notes;
	}
	
	
	@Override
	public void insertRecord(Connection c){
		PreparedStatement prep = null;
		try {
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
}
