package openeye.notes;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import openeye.logic.ModState;
import openeye.logic.StateHolder;
import openeye.logic.Storages;
import openeye.net.GenericSender.EncryptionState;
import openeye.notes.entries.MsgNoteEntry;
import openeye.notes.entries.NoteEntry;
import openeye.notes.entries.RemoveFileEntry;
import openeye.notes.entries.ReportedCrashEntry;
import openeye.notes.entries.ResolvedCrashEntry;
import openeye.notes.entries.SystemNoteEntry;
import openeye.responses.ResponseKnownCrashAction;
import openeye.responses.ResponseModMsgAction;
import openeye.responses.ResponseRemoveFileAction;

public class NoteCollector {

	private static final Comparator<NoteEntry> NOTE_COMPARATOR = new Comparator<NoteEntry>() {
		@Override
		public int compare(NoteEntry o1, NoteEntry o2) {
			int result = o2.level - o1.level;
			if (result != 0) return result;

			return o1.file.compareTo(o2.file);
		}
	};

	private final ScreenNotificationHolder menuLine = new ScreenNotificationHolder();

	private boolean important;

	public static final NoteCollector INSTANCE = new NoteCollector();

	private final List<NoteEntry> notes = Lists.newArrayList();

	private NoteCategory maxCategory = NoteCategory.INFO;

	private NoteCollector() {}

	public synchronized void addNote(NoteEntry entry) {
		notes.add(entry);
		maxCategory = Ordering.natural().max(maxCategory, entry.category);
		important |= entry.category.important;
	}

	public void addNote(File file, ResponseModMsgAction note) {
		NoteEntry entry = new MsgNoteEntry(file, note);
		addNote(entry);
	}

	public void addNote(File file, ResponseRemoveFileAction note) {
		addNote(new RemoveFileEntry(file, note));
		menuLine.signalDangerousFile();
	}

	public void addNote(ResponseKnownCrashAction note) {
		if (Strings.isNullOrEmpty(note.note)) {
			addNote(new ReportedCrashEntry(note));
			menuLine.signalCrashReported();
		} else {
			addNote(new ResolvedCrashEntry(note));
			menuLine.signalKnownCrash();
		}
	}

	public void addNote(EncryptionState encryptionState) {
		switch (encryptionState) {
			case NO_ROOT_CERTIFICATE:
				addNote(new SystemNoteEntry(NoteCategory.WARNING, 11,
						WrappedChatComponent.createTranslation("openeye.note.title.old_java"),
						WrappedChatComponent.createTranslation("openeye.note.content.old_java_recoverable"),
						"http://lmgtfy.com/?q=download+java"));
				break;
			case NOT_SUPPORTED:
				addNote(new SystemNoteEntry(NoteCategory.ALERT, 22,
						WrappedChatComponent.createTranslation("openeye.note.title.old_java"),
						WrappedChatComponent.createTranslation("openeye.note.content.old_java_total_failure"),
						"http://lmgtfy.com/?q=download+java"));
				break;
			default:
				break;
		}

	}

	public boolean isEmpty() {
		return notes.isEmpty();
	}

	public NoteCategory getMaxLevel() {
		return maxCategory;
	}

	public WrappedChatComponent getScreenMsg() {
		return menuLine.getSelectedLine();
	}

	public boolean hasImportantNotes() {
		return important;
	}

	public List<NoteEntry> getNotes() {
		Collections.sort(notes, NOTE_COMPARATOR);
		return ImmutableList.copyOf(notes);
	}

	private void addIntroNote(int id, String url) {
		String title = "openeye.note.title.intro" + id;
		String content = "openeye.note.content.intro" + id;
		addNote(new SystemNoteEntry(NoteLevels.SYSTEM_NOTIFICATION_LEVEL + 16 - id,
				WrappedChatComponent.createTranslation(title),
				WrappedChatComponent.createTranslation(content),
				url));
	}

	public void finishNoteCollection() {
		ModState state = StateHolder.state();

		if (!state.infoNotesDisplayed) {
			addIntroNote(1, "http://openeye.openmods.info");
			addIntroNote(2, "http://openeye.openmods.info");
			addIntroNote(3, "http://openeye.openmods.info/storage-policy");

			Storages storages = Storages.instance();
			if (storages != null) addIntroNote(4, storages.reportsDir.toURI().toString());

			addIntroNote(5, "https://github.com/OpenMods/OpenData");
			addIntroNote(6, "http://openeye.openmods.info/configuration");
			state.infoNotesDisplayed = true;
		}

		if (!state.mainMenuInfoDisplayed) {
			menuLine.signalIntroStuff();
			state.mainMenuInfoDisplayed = true;
		}
	}
}
