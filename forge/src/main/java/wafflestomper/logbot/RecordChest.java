package wafflestomper.logbot;

public class RecordChest extends Record{
	
	public int x;
	public int y;
	public int z;
	public boolean minecartChest;
	public String contents;
	public String notes;
	
	
	public RecordChest(String _serverName, String _worldName, int _x, int _y, int _z, boolean _minecartChest, String _contents, String _notes){
		this.serverName = _serverName;
		this.worldName = _worldName;
		this.x = _x;
		this.y = _y;
		this.z = _z;
		this.minecartChest = _minecartChest;
		this.contents = _contents;
		this.notes = _notes;
	}
}
