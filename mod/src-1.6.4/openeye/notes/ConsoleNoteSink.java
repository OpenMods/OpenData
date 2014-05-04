package openeye.notes;

import java.util.Collection;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatMessageComponent;
import openeye.notes.CommandNotes.INoteSink;
import openeye.notes.entries.NoteEntry;

import com.google.common.base.Strings;

final class ConsoleNoteSink implements INoteSink {
	@Override
	public void dump(Collection<NoteEntry> notes, ICommandSender sender) {
		int count = 0;
		for (NoteEntry note : notes) {
			ChatMessageComponent level = ChatMessageComponent.createFromTranslationKey(note.category.translated).setColor(note.category.color);
			sender.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions("openeye.chat.note", count++, level));
			sender.sendChatToPlayer(note.title().unwrap().setBold(true));
			sender.sendChatToPlayer(note.content().unwrap());

			String url = note.url();
			if (!Strings.isNullOrEmpty(url)) sender.sendChatToPlayer(ChatMessageComponent.createFromText(note.url()));
		}
	}
}