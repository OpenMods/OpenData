package openeye.notes.entries;

import net.minecraft.util.ChatMessageComponent;
import openeye.notes.NoteCategory;

public class SystemNoteEntry extends NoteEntry {

	private final ChatMessageComponent title;

	private final ChatMessageComponent contents;

	private final String url;

	public SystemNoteEntry(int level, ChatMessageComponent title, ChatMessageComponent contents, String url) {
		super(NoteCategory.SYSTEM_INFO, level);
		this.title = title;
		this.contents = contents;
		this.url = url;
	}

	@Override
	public ChatMessageComponent title() {
		return title;
	}

	@Override
	public ChatMessageComponent content() {
		return contents;
	}

	@Override
	public String url() {
		return url;
	}

}
