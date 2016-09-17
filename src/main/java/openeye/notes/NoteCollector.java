package openeye.notes;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import net.minecraft.util.ChatMessageComponent;
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
						ChatMessageComponent.createFromTranslationKey("openeye.note.title.old_java"),
						ChatMessageComponent.createFromTranslationKey("openeye.note.content.old_java_recoverable"),
						"http://lmgtfy.com/?q=download+java"));
				break;
			case NOT_SUPPORTED:
				addNote(new SystemNoteEntry(NoteCategory.ALERT, 22,
						ChatMessageComponent.createFromTranslationKey("openeye.note.title.old_java"),
						ChatMessageComponent.createFromTranslationKey("openeye.note.content.old_java_total_failure"),
						"http://lmgtfy.com/?q=download+java"));
				break;
			default:
				break;
		}
	}

	public void addSuspendNote(long suspendUntilTimestamp, String reason) {
		final Date suspendEndDate = new Date(suspendUntilTimestamp);
		final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		final String suspendPrintable = dateFormat.format(suspendEndDate);

		addNote(new SystemNoteEntry(NoteCategory.INFO, 32,
				ChatMessageComponent.createFromTranslationKey("openeye.note.title.suspended"),
				Strings.isNullOrEmpty(reason)
						? ChatMessageComponent.createFromTranslationWithSubstitutions("openeye.note.content.suspended_no_reason", suspendPrintable, reason)
						: ChatMessageComponent.createFromTranslationWithSubstitutions("openeye.note.content.suspended", suspendPrintable, reason),
				"https://openeye.openmods.info"));
	}

	public boolean isEmpty() {
		return notes.isEmpty();
	}

	public NoteCategory getMaxLevel() {
		return maxCategory;
	}

	public ChatMessageComponent getScreenMsg() {
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
				ChatMessageComponent.createFromTranslationKey(title),
				ChatMessageComponent.createFromTranslationKey(content),
				url));
	}

	public void finishNoteCollection() {
		ModState state = StateHolder.state();

		if (!state.infoNotesDisplayed) {
			addIntroNote(1, "https://openeye.openmods.info");
			addIntroNote(2, "https://openeye.openmods.info");
			addIntroNote(3, "https://openeye.openmods.info/storage-policy");

			Storages storages = Storages.instance();
			if (storages != null) addIntroNote(4, storages.reportsDir.toURI().toString());

			addIntroNote(5, "https://github.com/OpenMods/OpenData");
			addIntroNote(6, "https://openeye.openmods.info/configuration");
			state.infoNotesDisplayed = true;
		}

		if (!state.mainMenuInfoDisplayed) {
			menuLine.signalIntroStuff();
			state.mainMenuInfoDisplayed = true;
		}
	}
}
