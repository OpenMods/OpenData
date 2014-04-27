package openeye.notes.entries;

import java.io.File;

import openeye.notes.IconType;

import com.google.common.base.Objects;

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

	public abstract String title();

	public abstract String description();
}