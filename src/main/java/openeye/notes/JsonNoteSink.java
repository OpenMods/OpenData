package openeye.notes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collection;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.TextComponentTranslation;
import openeye.Log;
import openeye.notes.CommandNotes.INoteSink;
import openeye.notes.entries.NoteEntry;
import openeye.storage.IAppendableStorage;
import openeye.storage.IDataSource;

final class JsonNoteSink implements INoteSink {
	private final IAppendableStorage<Object> notesDump;

	JsonNoteSink(IAppendableStorage<Object> notesDump) {
		this.notesDump = notesDump;
	}

	@Override
	public void dump(Collection<NoteEntry> notes, ICommandSender sender) throws CommandException {
		JsonArray result = new JsonArray();

		for (NoteEntry note : notes) {
			JsonObject object = note.toJson();
			result.add(object);
		}

		try {
			IDataSource<Object> target = notesDump.createNew();
			target.store(result);
			sender.sendMessage(new TextComponentTranslation("openeye.chat.dumped", target.getId()));
		} catch (Throwable t) {
			Log.warn(t, "Failed to store notes");
			throw new CommandException("openeye.chat.store_failed");
		}
	}
}
