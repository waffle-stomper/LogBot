package wafflestomper.logbot;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

public class DBThread implements Runnable{

	private static boolean sqlDriverNotFound_databaseDisabled = false;
	private Thread t;
	private static boolean terminate = false;
	private static final String threadName = "InsertThread";
	public static ArrayBlockingQueue toDBqueue = new ArrayBlockingQueue(512);
	private static ArrayList<String> createTableStatements = new ArrayList<String>(); 
	private static String currentServer = "NO_SERVER_NAME";
	
	public static final DBThread INSTANCE;
	static{
		try{
			INSTANCE = new DBThread();
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
	
	
	public synchronized static void setServerName(String newServerName){
		currentServer = newServerName;
	}
	
	
	/**
	 * Adds a table creation statement to the list
	 */
	public synchronized static void registerCreateTable(String tableStatement){
		createTableStatements.add(tableStatement);
	}
	
	
	private static void addChatError(String error){
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.theWorld != null){
			mc.thePlayer.addChatComponentMessage(new TextComponentString("\u00A7c" + error));
		}
	}
	
	
	private synchronized static Connection connect() throws SQLException{
		// Create folders
		String path = Minecraft.getMinecraft().mcDataDir.getAbsolutePath();
		if (path.endsWith(".")){
			path = path.substring(0, path.length()-2);
		}
		File serverPath = new File(path, "mods" + File.separator + "LogBot" + File.separator + currentServer);
		if (!serverPath.exists()) serverPath.mkdirs();
		
		// Connect
		String logSubPath = "mods" + File.separator + "LogBot" + File.separator + currentServer + File.separator + "log.sqlite";
		Connection dbConnection = null;
		dbConnection = DriverManager.getConnection("jdbc:sqlite:" + logSubPath);
        //System.out.println("Database opened succesfully!");
        return(dbConnection);
    }
		
	
	@Override
	public void run() {
		System.out.println("Starting DB insert thread!");
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			setSQLDriverMissing();
			return;
		}
		if (!isSQLDriverMissing()){
			while(!terminate){
				// Process the queue coming in
				if(toDBqueue.isEmpty()){
					try {
						t.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}
				else{
					long startTime = System.currentTimeMillis();
					
					try {
						// Someone set us up the connection!
						Connection c = connect();
						c.setAutoCommit(true);
						
						// Make sure both tables exist
						Statement stmt = c.createStatement();
						for (String createTableStat : createTableStatements){
							stmt.executeUpdate(createTableStat);
						}
						stmt.close();
						
						// Add each record from the queue to the database until the queue is empty
						while(!toDBqueue.isEmpty()){
						    // Insert the new record
							Object nextRecord;
							try {
								nextRecord = toDBqueue.take();
							} catch (InterruptedException e) {
								e.printStackTrace();
								break;
							}
							
							if (nextRecord instanceof Record){
								Record currRec = (Record)nextRecord;
								if (currRec.isRequest()){
									// Process request
									currRec.requestRecord(c);
								}
								else{
									// Insert record into the database
									currRec.insertRecord(c);
								}
							}
							
							
						}
						
					    c.close();
					    
					} catch (SQLException e) {
						e.printStackTrace();
						addChatError("Database insert failed!");
					}
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
