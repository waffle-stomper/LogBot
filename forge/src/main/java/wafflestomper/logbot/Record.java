package wafflestomper.logbot;

import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

public interface Record {
	
	public abstract String getServerName();
	
	public abstract void insertRecord(Connection c);
	
}
