package wafflestomper.logbot.util;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import wafflestomper.logbot.LogBot;

public class KeyBindings {
	
	private KeyBinding chestSearchKey;
	
	public KeyBindings(){
		this.chestSearchKey = new KeyBinding("Chest Search", Keyboard.KEY_L, "LogBot");
		ClientRegistry.registerKeyBinding(this.chestSearchKey);
    	FMLCommonHandler.instance().bus().register(this);
	}
	
	@SubscribeEvent
	public void inputEvent(KeyInputEvent event)
	{
		if (event.isCanceled()) return;
		if (this.chestSearchKey.isPressed()){ 
			LogBot.INSTANCE.chestSearchKeyPressed();
		}
	}
}
