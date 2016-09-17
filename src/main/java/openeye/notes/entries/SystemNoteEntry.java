package openeye.notes.entries;

import net.minecraft.util.IChatComponent;
import openeye.notes.NoteCategory;

public class SystemNoteEntry extends NoteEntry {

	private final IChatComponent title;

	private final IChatComponent contents;

	private final String url;

	public SystemNoteEntry(int level, IChatComponent title, IChatComponent contents, String url) {
		this(NoteCategory.SYSTEM_INFO, level, title, contents, url);
	}

	public SystemNoteEntry(NoteCategory category, int level, IChatComponent title, IChatComponent contents, String url) {
		super(category, level);
		this.title = title;
		this.contents = contents;
		this.url = url;
	}

	@Override
	public IChatComponent title() {
		return title;
	}

	@Override
	public IChatComponent content() {
		return contents;
	}

	@Override
	public String url() {
		return url;
	}

}
