package openeye.notes.entries;

import java.io.File;

import openeye.notes.NoteCategory;
import openeye.notes.WrappedChatComponent;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;

public abstract class NoteEntry {
	private static final File DUMMY_FILE = new File("invalid");

	protected NoteEntry(File file, NoteCategory category, int level) {
		this.file = Objects.firstNonNull(file, DUMMY_FILE);
		this.category = category;
		this.level = level;
	}

	protected NoteEntry(NoteCategory category, int level) {
		this(DUMMY_FILE, category, level);
	}

	public final NoteCategory category;
	public final int level;
	public final File file;

	public abstract String url();

	public abstract WrappedChatComponent title();

	public abstract WrappedChatComponent content();

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.addProperty("filename", file.getName());
		result.addProperty("path", file.getPath());
		result.addProperty("category", category.toString());
		result.addProperty("level", level);

		result.addProperty("title", title().getUnformatted());
		result.addProperty("content", content().getUnformatted());

		String url = url();
		if (!Strings.isNullOrEmpty(url)) result.addProperty("url", url);

		return result;
	}
}