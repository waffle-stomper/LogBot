package wafflestomper.logbot;

public class RecordBlock extends Record{
	
	public String blockType;
	public String dropType;
	public int dropCount;
	public int x;
	public int y;
	public int z;
	public boolean mined;
	public String notes;
	
	
	public RecordBlock(String _serverName, String _worldName, String _blockType, String _dropType, int _dropCount, int _x, int _y, int _z, boolean _mined, String _notes){
		this.serverName = _serverName;
		this.worldName = _worldName;
		this.blockType = _blockType;
		this.dropType = _dropType;
		this.dropCount = _dropCount;
		this.x = _x;
		this.y = _y;
		this.z = _z;
		this.mined = _mined;
		this.notes = _notes;
	}
}
