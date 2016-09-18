package openeye.notes.entries;

import com.google.common.base.Strings;
import openeye.notes.NoteCategory;
import openeye.notes.WrappedChatComponent;
import openeye.responses.ResponseKnownCrashAction;

public class ResolvedCrashEntry extends NoteEntry {
	private final String url;
	private final String note;

	public ResolvedCrashEntry(ResponseKnownCrashAction msg) {
		super(NoteCategory.RESOLVED_CRASH, 64);
		this.url = msg.url;
		this.note = msg.note;
	}

	@Override
	public WrappedChatComponent title() {
		return WrappedChatComponent.createTranslation("openeye.notes.title.resolved_crash");
	}

	@Override
	public WrappedChatComponent content() {
		return WrappedChatComponent.createText(Strings.nullToEmpty(note));
	}

	@Override
	public String url() {
		return url;
	}
}
