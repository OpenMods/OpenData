package openeye.notes.entries;

import java.io.File;

import net.minecraft.util.ChatMessageComponent;
import openeye.notes.IconType;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;

public abstract class NoteEntry {
	private static final File DUMMY_FILE = new File("invalid");

	protected NoteEntry(File file, IconType type) {
		this.file = Objects.firstNonNull(file, DUMMY_FILE);
		this.type = type;
	}

	protected NoteEntry(IconType type) {
		this.file = DUMMY_FILE;
		this.type = type;
	}

	public final IconType type;
	public final File file;

	public static IconType calculateIconType(int level) {
		for (IconType type : IconType.VALUES)
			if (level >= type.threshold) return type;
		return IconType.INFO;
	}

	public abstract String url();

	public abstract ChatMessageComponent title();

	public abstract ChatMessageComponent description();

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.addProperty("filename", file.getName());
		result.addProperty("path", file.getPath());
		result.addProperty("type", type.toString());

		result.addProperty("title", title().toString());
		result.addProperty("description", description().toString());

		String url = url();
		if (!Strings.isNullOrEmpty(url)) result.addProperty("url", url);

		return result;
	}
}