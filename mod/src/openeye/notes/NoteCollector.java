package openeye.notes;

import java.io.File;
import java.util.*;

import net.minecraft.util.StatCollector;
import openeye.notes.entries.*;
import openeye.responses.ResponseDangerousFile;
import openeye.responses.ResponseKnownCrash;
import openeye.responses.ResponseModMsg;

import com.google.common.collect.*;

public class NoteCollector {

	private static final Comparator<NoteEntry> NOTE_COMPARATOR = new Comparator<NoteEntry>() {
		@Override
		public int compare(NoteEntry o1, NoteEntry o2) {
			int result = o1.type.compareTo(o2.type);
			if (result != 0) return result;

			return o1.file.compareTo(o2.file);
		}
	};

	public static class ScreenNotification implements Comparable<ScreenNotification> {
		public final IconType type;
		public final int color;
		public final String prefix;
		public final String line;
		public final Object[] params;

		private ScreenNotification(IconType type, int color, String prefix, String line, Object... params) {
			this.type = type;
			this.color = color;
			this.prefix = prefix;
			this.line = line;
			this.params = params;
		}

		public String getDisplay() {
			return prefix + StatCollector.translateToLocalFormatted(line, params);
		}

		@Override
		public int compareTo(ScreenNotification o) {
			return type.compareTo(o.type);
		}
	}

	private boolean important;

	public static final NoteCollector INSTANCE = new NoteCollector();

	private final List<NoteEntry> notes = Lists.newArrayList();

	private final SortedSet<ScreenNotification> lines = Sets.newTreeSet();

	private IconType maxType = IconType.INFO;

	private NoteCollector() {}

	protected void addNote(NoteEntry entry) {
		notes.add(entry);
		maxType = Ordering.natural().max(maxType, entry.type);
	}

	public void addNote(File file, ResponseModMsg note) {
		NoteEntry entry = new MsgNoteEntry(file, note);
		addNote(entry);
		important |= entry.type.important;
	}

	public void addNote(File file, ResponseDangerousFile note) {
		addNote(new DangerousFileEntry(file, note));
		lines.add(new ScreenNotification(IconType.CRITICAL, 0xFF0000, "\u00a7L", "openeye.main_screen.dangerous_file"));
		important = true;
	}

	public void addNote(ResponseKnownCrash note) {
		addNote(new KnownCrashEntry(note));
		lines.add(new ScreenNotification(IconType.INFO, 0x00FF00, "", "openeye.main_screen.known_crash"));
		important = true;
	}

	public boolean isEmpty() {
		return notes.isEmpty();
	}

	public IconType getMaxLevel() {
		return maxType;
	}

	public ScreenNotification getScreenMsg() {
		return lines.isEmpty()? null : lines.first();
	}

	public boolean hasImportantNotes() {
		return important;
	}

	public List<NoteEntry> getNotes() {
		Collections.sort(notes, NOTE_COMPARATOR);
		return ImmutableList.copyOf(notes);
	}
}
