package openeye.notes;

import com.google.common.base.Strings;
import java.util.Collection;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import openeye.notes.CommandNotes.INoteSink;
import openeye.notes.entries.NoteEntry;

final class ConsoleNoteSink implements INoteSink {
	@Override
	public void dump(Collection<NoteEntry> notes, ICommandSender sender) {
		int count = 0;
		for (NoteEntry note : notes) {
			ITextComponent level = new TextComponentTranslation(note.category.translated);
			level.getStyle().setColor(note.category.color);
			sender.addChatMessage(new TextComponentTranslation("openeye.chat.note", count++, level));
			ITextComponent title = note.title();
			title.getStyle().setBold(true);
			sender.addChatMessage(title);
			sender.addChatMessage(note.content());

			String url = note.url();
			if (!Strings.isNullOrEmpty(url)) sender.addChatMessage(new TextComponentString(note.url()));
		}
	}
}