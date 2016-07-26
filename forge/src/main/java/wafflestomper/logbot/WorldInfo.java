package wafflestomper.logbot;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WorldInfo{
	
	private static final int MIN_DELAY_MS = 1000;
	private static long lastRequest;
	private static long lastResponse;
	private SimpleNetworkWrapper channel;
	private static String worldID;
	private static String serverIP;
	private static Minecraft mc;

	
	public void preInit(FMLPreInitializationEvent event) {
		mc = Minecraft.getMinecraft();
		channel = NetworkRegistry.INSTANCE.newSimpleChannel("world_info");
		channel.registerMessage(WorldListener.class, WorldIDPacket.class, 0, Side.CLIENT);
		MinecraftForge.EVENT_BUS.register(this);
		FMLCommonHandler.instance().bus().register(this);
	}
	
	
	public String getWorldName(){
		if (worldID == null){
			return("NO_WORLD_NAME");
		}
		else{
			return(worldID);
		}
	}
	
	
	public String getServerIP(){
		if (serverIP == null){
			return("NO_SERVER_IP");
		}
		else{
			return(serverIP);
		}
	}
	
	
	/**
	 * Returns a version of the server IP/hostname without special characters or spaces
	 * It should be suitable for file/folder naming
	 */
	public String getSanitizedServerIP(){
		String serverName = this.getServerIP();
		if (serverName.contains(":")){
			serverName = serverName.substring(0, serverName.indexOf(':'));
		}
		if (serverName.contains("/")){
			serverName = serverName.substring(0, serverName.indexOf('/'));
		}
		return(serverName);
	}
	
	
	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event) {
		if(!mc.isSingleplayer() && mc.thePlayer != null && !mc.thePlayer.isDead) {
			if(mc.thePlayer.getDisplayName().equals(event.getEntity().getDisplayName())) {
				requestWorldID();
				serverIP = mc.getCurrentServerData().serverIP;
				//serverIP = .getNetHandler().getNetworkManager().getRemoteAddress().toString();
				//mc.thePlayer.addChatMessage(new TextComponentString(worldID + " " + serverIP));
			}
		}
	}
	
	
	private void requestWorldID() {
		long now = System.currentTimeMillis();
		if((lastRequest + MIN_DELAY_MS < now) && (lastResponse + MIN_DELAY_MS < now)) {
			//System.out.println("Sending request..");
			channel.sendToServer(new WorldIDPacket());
			lastRequest = System.currentTimeMillis();
		}
	}
	
	
	public static class WorldListener implements IMessageHandler<WorldIDPacket, IMessage> {
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(WorldIDPacket message, MessageContext ctx) {
			lastResponse = System.currentTimeMillis();
			worldID = message.getWorldID();
			//mc.thePlayer.addChatMessage(new TextComponentString(worldID + "@" + serverIP));
			return null;
		}
	}
	
	
	public static class WorldIDPacket implements IMessage {
		
		public static final String CHANNEL_NAME = "world_info";
		private String worldID;
		
		
		public WorldIDPacket() {}
		
		
		public WorldIDPacket(String worldID) {
			this.worldID = worldID;
		}
		
		
		public String getWorldID() {
			return worldID;
		}

		
		@Override
		public void fromBytes(ByteBuf buf) {
			worldID = ByteBufUtils.readUTF8String(buf);
		}
		
		
		@Override
		public void toBytes(ByteBuf buf) {
			if(worldID != null) {
				ByteBufUtils.writeUTF8String(buf, worldID);
			}
			else{
				System.out.println("World ID Is null");
			}
		}
	}
}