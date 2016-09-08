package openeye.notes.entries;

import openeye.notes.NoteCategory;
import openeye.notes.WrappedChatComponent;

public class SystemNoteEntry extends NoteEntry {

	private final WrappedChatComponent title;

	private final WrappedChatComponent contents;

	private final String url;

	public SystemNoteEntry(int level, WrappedChatComponent title, WrappedChatComponent contents, String url) {
		this(NoteCategory.SYSTEM_INFO, level, title, contents, url);
	}

	public SystemNoteEntry(NoteCategory category, int level, WrappedChatComponent title, WrappedChatComponent contents, String url) {
		super(category, level);
		this.title = title;
		this.contents = contents;
		this.url = url;
	}

	@Override
	public WrappedChatComponent title() {
		return title;
	}

	@Override
	public WrappedChatComponent content() {
		return contents;
	}

	@Override
	public String url() {
		return url;
	}

}
