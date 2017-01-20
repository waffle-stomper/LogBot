package wafflestomper.logbot.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import wafflestomper.logbot.database.DBThread;
import wafflestomper.logbot.database.RecordNote;
import wafflestomper.logbot.util.Printer;
import wafflestomper.wafflecore.WaffleCore;

public class CommandAddNote implements ICommand{
	private static final String[] ALIASES = {"note", "an"};
	
	@Override
	public int compareTo(ICommand arg0) {
		return this.getName().compareTo(arg0.getName());
	}

	@Override
	public String getName() {
		return "addnote";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "Add a note to the database";
	}

	@Override
	public List<String> getAliases() {
		return(Arrays.asList(ALIASES));
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		System.out.println("Executing test command");
		// For now, just assemble all of the 'arguments' into a string and add that to the notes table
		String note = "";
		for (String s : args){
			note += s + " ";
		}
		if (note.isEmpty()){
			Printer.gamePrint(Printer.RED + "You need to enter at least one character for the note");
			return;
		}
		Printer.gamePrint(TextFormatting.GREEN + "Adding note to database...");
		WaffleCore wafflecore = WaffleCore.INSTANCE;
		Minecraft mc = Minecraft.getMinecraft();
    	String serverName = wafflecore.worldInfo.getNiceServerIP();
		String worldName = wafflecore.worldInfo.getWorldName();
		double x = mc.player.posX;
		double y = mc.player.posY;
		double z = mc.player.posZ;
		RecordNote newNote = new RecordNote(serverName, worldName, x, y, z, note);
		try {
			DBThread.INSTANCE.toDBqueue.put(newNote);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
		return Collections.<String>emptyList();
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
        return false;
	}
}
