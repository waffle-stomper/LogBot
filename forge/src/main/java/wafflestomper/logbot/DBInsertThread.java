package wafflestomper.logbot;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;

import net.minecraft.client.Minecraft;

public class DBInsertThread implements Runnable{

	private static boolean sqlDriverNotFound_databaseDisabled = false;
	private Thread t;
	private static boolean terminate = false;
	private static final String threadName = "InsertThread";
	public static ArrayBlockingQueue queue = new ArrayBlockingQueue(512);
	private static final String createBlocksTable = "CREATE TABLE IF NOT EXISTS BLOCKS " +
			 	                 					"(ID			INTEGER		PRIMARY KEY ASC, " +
								 	                 "TIMESTAMP	  	DATETIME 	DEFAULT CURRENT_TIMESTAMP, " +	
								 	                 "WORLDNAME     TEXT    	NOT NULL, " + 
								 	                 "BLOCKTYPE     TEXT     	NOT NULL, " +
								 	                 "X             INTEGER     NOT NULL, " + 
								 	                 "Y             INTEGER     NOT NULL, " + 
								 	                 "Z             INTEGER		NOT NULL, " + 
								 	                 "MINED		   	INTEGER		NOT NULL, " + 
								 	                 "NOTES         TEXT)"; 
	private static final String createChestsTable = "CREATE TABLE IF NOT EXISTS CHESTS " +
								 	                "(ID			INTEGER		PRIMARY KEY ASC, " +
								 	                "TIMESTAMP		DATETIME 	DEFAULT CURRENT_TIMESTAMP, " +	
								 	                "WORLDNAME		TEXT		NOT NULL, " +
								 	                "X				INTEGER		NOT NULL, " + 
								                    "Y				INTEGER		NOT NULL, " + 
								 	                "Z				INTEGER		NOT NULL, " + 
								 	                "CONTENTS		TEXT)"; 
	private static final DBInsertThread instance;
	static{
		try{
			instance = new DBInsertThread();
		}
		catch (Exception e){
			throw new ExceptionInInitializerError(e);
		}
	}
	
	
	public synchronized static void setSQLDriverMissing(){
		sqlDriverNotFound_databaseDisabled = true;
	}
	
	
	public synchronized static boolean isSQLDriverMissing(){
		return(sqlDriverNotFound_databaseDisabled);
	}
	
	
	private static Connection connect(String serverName) throws SQLException{
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
		
	
	private static void insertBlock(RecordBlock block, Connection c){
		PreparedStatement prep = null;
		try {
			prep = c.prepareStatement("INSERT INTO BLOCKS (TIMESTAMP, WORLDNAME, BLOCKTYPE, X, Y, Z, MINED, NOTES) " +
					 	    		  "VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
			prep.setString(1, block.timestamp);
			prep.setString(2, block.worldName);
			prep.setString(3, block.blockType);
			prep.setInt(4, block.x);
			prep.setInt(5, block.y);
			prep.setInt(6, block.z);
			prep.setInt(7, block.mined?1:0);
			prep.setString(8, block.notes);
			prep.execute();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	private static void insertChest(RecordChest chest, Connection c){
		PreparedStatement prep = null;
		try {
			prep = c.prepareStatement("INSERT INTO CHESTS (TIMESTAMP, WORLDNAME, X, Y, Z, CONTENTS) " + 
									  "VALUES (?, ?, ?, ?, ?, ?);");
			prep.setString(1, chest.timestamp);
			prep.setString(2, chest.worldName);
			prep.setInt(3, chest.x);
			prep.setInt(4, chest.y);
			prep.setInt(5, chest.z);
			prep.setString(6, chest.contents);
			prep.execute();
			prep.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void run() {
		System.out.println("Starting DB insert thread!");
		if (!isSQLDriverMissing()){
			while(!terminate){
				if(queue.isEmpty()){
					try {
						t.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}
				else{
					long startTime = System.currentTimeMillis();
					int insertCount = 0;
					
					try {
						// Someone set us up the connection!
						Connection c = connect(((Record)queue.peek()).getServerName());
						c.setAutoCommit(true);
						
						// Make sure both tables exist
						Statement stmt = c.createStatement();
						stmt.executeUpdate(createBlocksTable);
						stmt.executeUpdate(createChestsTable);
						stmt.close();
						
						// Add each record from the queue to the database until the queue is empty
						while(!queue.isEmpty()){
						    // Insert the new record
							Object nextRecord;
							try {
								nextRecord = queue.take();
							} catch (InterruptedException e) {
								e.printStackTrace();
								break;
							}
							if (nextRecord instanceof RecordBlock){
								insertBlock((RecordBlock)nextRecord, c);
							}
							else if (nextRecord instanceof RecordChest){
								insertChest((RecordChest)nextRecord, c);
							}
							insertCount++;
						}
					    c.close();
					    
					} catch (SQLException e) {
						e.printStackTrace();
						System.out.println("Database insert failed!");
					}
					//System.out.println("Inserted " + insertCount + " records in " + (System.currentTimeMillis()-startTime) + "ms");
				}
			}
		}
	}

	
	public void start (){
		if (t == null){
			t = new Thread (this, threadName);
			t.start ();
	    }
	}
}
