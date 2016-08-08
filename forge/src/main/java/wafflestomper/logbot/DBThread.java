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
	public static ArrayBlockingQueue queue = new ArrayBlockingQueue(512);
	public static ArrayList<String> createTableStatements = new ArrayList<String>(); 
	
	
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
						for (String createTableStat : createTableStatements){
							stmt.executeUpdate(createTableStat);
						}
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
							if (nextRecord instanceof Record){
								System.out.println("Inserting record..");
								((Record)nextRecord).insertRecord(c);
							}
							insertCount++;
						}
					    c.close();
					    
					} catch (SQLException e) {
						e.printStackTrace();
						addChatError("Database insert failed!");
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
