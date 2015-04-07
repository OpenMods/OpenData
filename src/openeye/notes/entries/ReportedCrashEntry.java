package openeye.notes.entries;

import openeye.notes.NoteCategory;
import openeye.notes.NoteLevels;
import openeye.notes.WrappedChatComponent;
import openeye.responses.ResponseKnownCrashAction;

public class ReportedCrashEntry extends NoteEntry {
	private final String url;

	public ReportedCrashEntry(ResponseKnownCrashAction msg) {
		super(NoteCategory.REPORTED_CRASH, NoteLevels.REPORTED_CRASH_LEVEL);
		this.url = msg.url;
	}

	@Override
	public WrappedChatComponent title() {
		return WrappedChatComponent.createTranslation("openeye.notes.title.reported_crash");
	}

	@Override
	public WrappedChatComponent content() {
		return WrappedChatComponent.createText("");
	}

	@Override
	public String url() {
		return url;
	}
}