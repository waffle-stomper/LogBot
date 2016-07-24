package wafflestomper.logbot;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

public class KeyBindings{
	
	private KeyBinding loggerKey;
	private LogBot logbot;
	
	
	public KeyBindings(LogBot _logbot){
		this.logbot = _logbot;
		this.init();
	}
	
	
	public void init(){
		this.loggerKey = new KeyBinding("LogBot", Keyboard.KEY_L, "Master Logging Switch");
		ClientRegistry.registerKeyBinding(this.loggerKey);
    	FMLCommonHandler.instance().bus().register(this);
	}
	
	
	/**
	 * This is kind of a hack that I have to do because Forge hasn't fully implemented the KeyInputEvent event
	 */
	@SubscribeEvent
	public void inputEvent(KeyInputEvent event)
	{
		if (event.isCanceled()) return;
		if (this.loggerKey.isPressed()){
			this.logbot.loggerKeyPressed();
		}
	}
}