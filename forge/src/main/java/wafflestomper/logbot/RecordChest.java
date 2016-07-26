package wafflestomper.logbot;

public class RecordChest extends Record{
	
	public int x;
	public int y;
	public int z;
	public String contents;
	
	
	public RecordChest(String _serverName, String _worldName, int _x, int _y, int _z, String _contents){
		this.serverName = _serverName;
		this.worldName = _worldName;
		this.x = _x;
		this.y = _y;
		this.z = _z;
		this.contents = _contents;
	}
}
