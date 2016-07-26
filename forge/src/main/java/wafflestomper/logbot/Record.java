package wafflestomper.logbot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Record {
	
	public String serverName;
	public String worldName;
	public String timestamp;
	
	
	public Record(){
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:22");
		df.setTimeZone(tz);
		this.timestamp = df.format(new Date());
	}
	
	
	public String getServerName(){
		return(this.serverName);
	}
}
