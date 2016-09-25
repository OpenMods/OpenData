package openeye.notes;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import openeye.logic.GsonUtils;
import openeye.notes.entries.NoteEntry;
import openeye.storage.GsonSimpleStorage;
import openeye.storage.IAppendableStorage;

public class CommandNotes implements ICommand {

	private static final String COMMAND_NAME = "eye_notes";

	interface INoteSink {
		public void dump(Collection<NoteEntry> notes, ICommandSender sender) throws CommandException;
	}

	private final Map<String, INoteSink> sinks = Maps.newHashMap();

	public CommandNotes(File reportDir) {
		final IAppendableStorage<Object> notesDump = new GsonSimpleStorage<Object>(reportDir, "notes", "json", Object.class, GsonUtils.PRETTY_GSON);
		sinks.put("console", new ConsoleNoteSink());
		sinks.put("json", new JsonNoteSink(notesDump));
	}

	@Override
	public int compareTo(ICommand o) {
		return COMMAND_NAME.compareTo(o.getCommandName());
	}

	@Override
	public String getCommandName() {
		return COMMAND_NAME;
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		StringBuilder builder = new StringBuilder(COMMAND_NAME).append(" <");
		Joiner.on('|').appendTo(builder, sinks.keySet());
		builder.append(">");
		return builder.toString();
	}

	@Override
	public List<String> getCommandAliases() {
		return Lists.newArrayList();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] command) throws CommandException {
		if (command.length != 1) throw new SyntaxErrorException();
		String sinkType = command[0];
		INoteSink sink = sinks.get(sinkType);
		if (sink == null) throw new SyntaxErrorException();

		sink.dump(NoteCollector.INSTANCE.getNotes(), sender);
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return sender.canCommandSenderUseCommand(4, COMMAND_NAME);
	}

	@Override
	public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] command, BlockPos pos) {
		List<String> result = Lists.newArrayList();

		if (command.length == 1) {
			String prefix = command[0];
			for (String name : sinks.keySet())
				if (name.startsWith(prefix)) result.add(name);
		}

		return result;
	}

	@Override
	public boolean isUsernameIndex(String[] command, int index) {
		return false;
	}

}
