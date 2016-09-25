package openeye.notes.entries;

import net.minecraft.util.text.ITextComponent;
import openeye.notes.NoteCategory;

public class SystemNoteEntry extends NoteEntry {

	private final ITextComponent title;

	private final ITextComponent contents;

	private final String url;

	public SystemNoteEntry(int level, ITextComponent title, ITextComponent contents, String url) {
		this(NoteCategory.SYSTEM_INFO, level, title, contents, url);
	}

	public SystemNoteEntry(NoteCategory category, int level, ITextComponent title, ITextComponent contents, String url) {
		super(category, level);
		this.title = title;
		this.contents = contents;
		this.url = url;
	}

	@Override
	public ITextComponent title() {
		return title;
	}

	@Override
	public ITextComponent content() {
		return contents;
	}

	@Override
	public String url() {
		return url;
	}

}
