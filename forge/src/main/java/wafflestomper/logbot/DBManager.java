package wafflestomper.logbot;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

public class DBManager {
	
	private Minecraft mc;
	private boolean sqlDriverNotFound_databaseDisabled = false;
	private boolean firstBlockInsert = true;
	private boolean firstChestInsert = true;
	
	
	public DBManager(){
		this.mc = Minecraft.getMinecraft();
	    try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.out.println("Setting sqlErrorDBDisabled to true");
			this.sqlDriverNotFound_databaseDisabled = true;
		}
	}
	
	
	public boolean isSQLDriverMissing(){
		return(this.sqlDriverNotFound_databaseDisabled);
	}
	
	
	private Connection connect(String serverName) throws SQLException{
		// Create folders
		String path = Minecraft.getMinecraft().mcDataDir.getAbsolutePath();
		if (path.endsWith(".")){
			path = path.substring(0, path.length()-2);
		}
		File serverPath = new File(path, "mods" + File.separator + "LogBot" + File.separator + serverName);
		if (!serverPath.exists()) serverPath.mkdirs();
		
		// Connect
		String logSubPath = "mods" + File.separator + "LogBot" + File.separator + serverName + File.separator + "log.sqlite";
		Connection dbConnection = null;
		dbConnection = DriverManager.getConnection("jdbc:sqlite:" + logSubPath);
        System.out.println("Database opened succesfully!");
        return(dbConnection);
    }
	
	
	private void createBlocksTable(String serverName) throws SQLException{
		if (this.sqlDriverNotFound_databaseDisabled){
			return;
		}
		Connection c = this.connect(serverName);
		Statement stmt = null;
	    stmt = c.createStatement();
	    String sql = "CREATE TABLE IF NOT EXISTS BLOCKS " +
	                 "(ID			INTEGER		PRIMARY KEY ASC, " +
	                 "TIMESTAMP	  	DATETIME 	DEFAULT CURRENT_TIMESTAMP, " +	
	                 "WORLDNAME     TEXT    	NOT NULL, " + 
	                 "BLOCKTYPE     TEXT     	NOT NULL, " +
	                 "X             INTEGER     NOT NULL, " + 
	                 "Y             INTEGER     NOT NULL, " + 
	                 "Z             INTEGER		NOT NULL, " + 
	                 "MINED		   	INTEGER		NOT NULL, " + 
	                 "NOTES         TEXT)"; 
	    stmt.executeUpdate(sql);
	    stmt.close();
	    c.close();
	    System.out.println("Blocks table created successfully");
	}
	
	
	public void insertBlock(String serverName, String worldName, String blockType, int x, int y, int z, boolean mined, String notes) throws SQLException{
		if (this.sqlDriverNotFound_databaseDisabled){
			return;
		}
		
		// Make sure the table has been created if this is the first time we're accessing it this session
		if (this.firstBlockInsert){
			this.firstBlockInsert = false;
			this.createBlocksTable(serverName);
		}
		
		Connection c = this.connect(serverName);
	    Statement stmt = null;
	    c.setAutoCommit(true);
	    System.out.println("Opened database successfully");
	  
	    PreparedStatement prep = null;
	    prep = c.prepareStatement("INSERT INTO BLOCKS (WORLDNAME, BLOCKTYPE, X, Y, Z, MINED, NOTES) " +
	    						  "VALUES (?, ?, ?, ?, ?, ?, ?);");
	    prep.setString(1, worldName);
	    prep.setString(2, blockType);
	    prep.setInt(3, x);
	    prep.setInt(4, y);
	    prep.setInt(5, z);
	    prep.setInt(6, mined?1:0);
	    prep.setString(7, notes);
	    prep.execute();
	    prep.close();
	    c.close();
	    System.out.println("Block record created successfully");
	}
	
	
	private void createChestsTable(String serverName) throws SQLException{
		if (this.sqlDriverNotFound_databaseDisabled){
			return;
		}
		Connection c = this.connect(serverName);
		Statement stmt = null;
	    stmt = c.createStatement();
	    String sql = "CREATE TABLE IF NOT EXISTS CHESTS " +
	                 "(ID			INTEGER		PRIMARY KEY ASC, " +
	                 "TIMESTAMP		DATETIME 	DEFAULT CURRENT_TIMESTAMP, " +	
	                 "WORLDNAME     TEXT    	NOT NULL, " +
	                 "X             INTEGER     NOT NULL, " + 
	                 "Y             INTEGER     NOT NULL, " + 
	                 "Z             INTEGER     NOT NULL, " + 
	                 "CONTENTS	   	TEXT)"; 
	    stmt.executeUpdate(sql);
	    stmt.close();
	    c.close();
	    System.out.println("Chests table created successfully");
	}
	
	
	public void insertChest(String serverName, String worldName, int x, int y, int z, String contents) throws SQLException{
		if (this.sqlDriverNotFound_databaseDisabled){
			return;
		}
		
		// Make sure the table has been created if this is the first time we're accessing it this session
		if (this.firstChestInsert){
			this.firstChestInsert = false;
			this.createChestsTable(serverName);
		}
		
		Connection c = this.connect(serverName);
	    Statement stmt = null;

	    c.setAutoCommit(true);
	    System.out.println("Opened database successfully");
	      
	    PreparedStatement prep = null;
	    prep = c.prepareStatement("INSERT INTO CHESTS (WORLDNAME, X, Y, Z, CONTENTS) " + "VALUES (?, ?, ?, ?, ?);");
	    prep.setString(1, worldName);
	    prep.setInt(2, x);
	    prep.setInt(3, y);
	    prep.setInt(4, z);
	    prep.setString(5, contents);
	     
	    prep.execute();
	    prep.close();
	    c.close();
	    System.out.println("Chest record created successfully");
	}
}
