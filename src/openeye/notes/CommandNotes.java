package openeye.notes;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.minecraft.command.*;
import net.minecraft.util.ChatMessageComponent;
import openeye.Log;
import openeye.logic.GsonUtils;
import openeye.notes.entries.NoteEntry;
import openeye.storage.*;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class CommandNotes implements ICommand {

	private static final String COMMAND_NAME = "eye_notes";

	private interface INoteSink {
		public void dump(Collection<NoteEntry> notes, ICommandSender sender);
	}

	private final Map<String, INoteSink> sinks = Maps.newHashMap();

	public CommandNotes(File minecraftDir) {
		File reportDir = Storages.getReportDir(minecraftDir);
		final IAppendableStorage<Object> notesDump = new GsonSimpleStorage<Object>(reportDir, "notes", "json", Object.class, GsonUtils.PRETTY_GSON);

		sinks.put("console", new INoteSink() {
			@Override
			public void dump(Collection<NoteEntry> notes, ICommandSender sender) {
				int count = 0;
				for (NoteEntry note : notes) {
					ChatMessageComponent level = ChatMessageComponent.createFromTranslationKey(note.type.translated).setColor(note.type.color);
					sender.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions("openeye.chat.note", count++, level));
					sender.sendChatToPlayer(note.title().setBold(true));
					sender.sendChatToPlayer(note.description());

					String url = note.url();
					if (!Strings.isNullOrEmpty(url)) sender.sendChatToPlayer(ChatMessageComponent.createFromText(note.url()));
				}
			}
		});

		sinks.put("json", new INoteSink() {
			@Override
			public void dump(Collection<NoteEntry> notes, ICommandSender sender) {
				JsonArray result = new JsonArray();

				for (NoteEntry note : notes) {
					JsonObject object = note.toJson();
					result.add(object);
				}

				try {
					IDataSource<Object> target = notesDump.createNew();
					target.store(result);
					sender.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions("openeye.chat.dumped", target.getId()));
				} catch (Throwable t) {
					Log.warn(t, "Failed to store notes");
					throw new CommandException("openeye.chat.store_failed");
				}
			}
		});
	}

	@Override
	public int compareTo(Object o) {
		return COMMAND_NAME.compareTo(((ICommand)o).getCommandName());
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
	@SuppressWarnings("rawtypes")
	public List getCommandAliases() {
		return null;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] command) {
		if (command.length != 1) throw new SyntaxErrorException();
		String sinkType = command[0];
		INoteSink sink = sinks.get(sinkType);
		if (sink == null) throw new SyntaxErrorException();

		sink.dump(NoteCollector.INSTANCE.getNotes(), sender);
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return sender.canCommandSenderUseCommand(4, COMMAND_NAME);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public List addTabCompletionOptions(ICommandSender sender, String[] command) {
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