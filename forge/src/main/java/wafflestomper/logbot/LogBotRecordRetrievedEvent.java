package wafflestomper.logbot;

import net.minecraftforge.fml.common.eventhandler.Event;

public class LogBotRecordRetrievedEvent extends Event{
	
	private Record record;
	
	public LogBotRecordRetrievedEvent(Record _record){
		this.record = _record;
	}
	
	public Record getRecord(){
		return(this.record);
	}
}
