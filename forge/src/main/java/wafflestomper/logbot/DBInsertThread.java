package wafflestomper.logbot;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

public class DBInsertThread implements Runnable{

	protected static boolean sqlDriverNotFound_databaseDisabled = false;
	private String serverName;
	private String tableCreateStatement = "";
	private String insertStatement = "";
	private Object[] insertValues;
	private Thread t;
	private String threadName = "";
	
	
	public DBInsertThread(String _threadName){
		this.threadName = _threadName;
	}
	
	
	public static boolean isSQLDriverMissing(){
		return(sqlDriverNotFound_databaseDisabled);
	}
	
	
	private Connection connect() throws SQLException{
		// Create folders
		String path = Minecraft.getMinecraft().mcDataDir.getAbsolutePath();
		if (path.endsWith(".")){
			path = path.substring(0, path.length()-2);
		}
		File serverPath = new File(path, "mods" + File.separator + "LogBot" + File.separator + this.serverName);
		if (!serverPath.exists()) serverPath.mkdirs();
		
		// Connect
		String logSubPath = "mods" + File.separator + "LogBot" + File.separator + this.serverName + File.separator + "log.sqlite";
		Connection dbConnection = null;
		dbConnection = DriverManager.getConnection("jdbc:sqlite:" + logSubPath);
        System.out.println("Database opened succesfully!");
        return(dbConnection);
    }

	
	public void insertBlock(String _serverName, String worldName, String blockType, int x, int y, int z, boolean mined, String notes) throws SQLException{
		if (sqlDriverNotFound_databaseDisabled){
			return;
		}
		this.serverName = _serverName;
		this.tableCreateStatement = "CREATE TABLE IF NOT EXISTS BLOCKS " +
                					"(ID			INTEGER		PRIMARY KEY ASC, " +
					                "TIMESTAMP		DATETIME 	DEFAULT CURRENT_TIMESTAMP, " +	
					                "WORLDNAME     	TEXT    	NOT NULL, " + 
					                "BLOCKTYPE     	TEXT     	NOT NULL, " +
					                "X            	INTEGER     NOT NULL, " + 
					                "Y             	INTEGER     NOT NULL, " + 
					                "Z             	INTEGER		NOT NULL, " + 
					                "MINED		   	INTEGER		NOT NULL, " + 
					                "NOTES         	TEXT)"; 
		
	    this.insertStatement = "INSERT INTO BLOCKS (WORLDNAME, BLOCKTYPE, X, Y, Z, MINED, NOTES) "+
	    			           "VALUES (?, ?, ?, ?, ?, ?, ?);";
	    this.insertValues = new Object[7];
	    this.insertValues[0] = worldName;
	    this.insertValues[1] = blockType;
	    this.insertValues[2] = x;
	    this.insertValues[3] = y;
	    this.insertValues[4] = z;
	    this.insertValues[5] = mined;
	    this.insertValues[6] = notes;
	}
	
	
	public void insertChest(String _serverName, String worldName, int x, int y, int z, String contents) throws SQLException{
		if (sqlDriverNotFound_databaseDisabled){
			return;
		}
		this.serverName = _serverName;
		this.tableCreateStatement = "CREATE TABLE IF NOT EXISTS CHESTS " +
                					"(ID			INTEGER		PRIMARY KEY ASC, " +
					                "TIMESTAMP		DATETIME 	DEFAULT CURRENT_TIMESTAMP, " +	
					                "WORLDNAME      TEXT    	NOT NULL, " +
					                "X              INTEGER     NOT NULL, " + 
					                "Y              INTEGER     NOT NULL, " + 
					                "Z              INTEGER     NOT NULL, " + 
					                "CONTENTS	   	TEXT)"; 
		this.insertStatement = "INSERT INTO CHESTS (WORLDNAME, X, Y, Z, CONTENTS) " + 
					            "VALUES (?, ?, ?, ?, ?);";
		this.insertValues = new Object[6];
		this.insertValues[0] = worldName;
	    this.insertValues[1] = x;
	    this.insertValues[2] = y;
	    this.insertValues[3] = z;
	    this.insertValues[4] = contents;
	}


	@Override
	public void run() {
		System.out.println("Running insert thread!!");
		long startTime = System.currentTimeMillis();
		if (!this.serverName.isEmpty() && !this.tableCreateStatement.isEmpty() && !this.insertStatement.isEmpty() && 
				this.insertValues != null && !sqlDriverNotFound_databaseDisabled){
			try {
				Connection c = this.connect();
				c.setAutoCommit(true);
				
				// Make sure the table exists
				Statement stmt = c.createStatement();
			    stmt.executeUpdate(this.tableCreateStatement);
			    stmt.close();
			    
			    // Insert the new record
			    PreparedStatement prep = null;
			    prep = c.prepareStatement(this.insertStatement);
			    for(int i=0; i<this.insertValues.length; i++){
			    	Object o = this.insertValues[i];
			    	if (o instanceof String){
			    		prep.setString(i+1, (String)o);
			    	}
			    	else if(o instanceof Integer){
			    		prep.setInt(i+1, (Integer)o);
			    	}
			    	else if(o instanceof Boolean){
			    		prep.setInt(i+1, (Boolean)o?1:0);
			    	}
			    }
			    prep.execute();
			    prep.close();
			    c.close();
			    
			} catch (SQLException e) {
				System.out.println("Database insert failed!");
				e.printStackTrace();
			}
		}
		System.out.println("Threaded DB insert took " + (System.currentTimeMillis()-startTime) + "ms");
	}
	
	
	public void start (){
		if (t == null){
			t = new Thread (this, threadName);
			t.start ();
	    }
	}
}
