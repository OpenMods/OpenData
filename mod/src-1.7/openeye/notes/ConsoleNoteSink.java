package openeye.notes;

import java.util.Collection;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import openeye.notes.CommandNotes.INoteSink;
import openeye.notes.entries.NoteEntry;

import com.google.common.base.Strings;

final class ConsoleNoteSink implements INoteSink {
	@Override
	public void dump(Collection<NoteEntry> notes, ICommandSender sender) {
		int count = 0;
		for (NoteEntry note : notes) {
			IChatComponent level = new ChatComponentTranslation(note.category.translated);
			level.getChatStyle().setColor(note.category.color);
			sender.addChatMessage(new ChatComponentTranslation("openeye.chat.note", count++, level));
			IChatComponent title = note.title().unwrap();
			title.getChatStyle().setBold(true);
			sender.addChatMessage(title);
			sender.addChatMessage(note.content().unwrap());

			String url = note.url();
			if (!Strings.isNullOrEmpty(url)) sender.addChatMessage(new ChatComponentText(note.url()));
		}
	}
}