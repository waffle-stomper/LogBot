package wafflestomper.logbot;

import java.util.List;

import net.minecraftforge.fml.common.eventhandler.Event;
import wafflestomper.logbot.database.Record;

public class LogBotRecordsRetrievedEvent extends Event{
	
	private List<Record> records;
	private long requestID;
	
	public LogBotRecordsRetrievedEvent(List<Record> _records, long _requestID){
		this.records = _records;
		this.requestID = _requestID;
	}
	
	public List<Record> getRecords(){
		return(this.records);
	}
	
	public long getRequestID(){
		return(this.requestID);
	}
}
