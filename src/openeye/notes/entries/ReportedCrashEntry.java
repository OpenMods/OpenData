package openeye.notes.entries;

import net.minecraft.util.ChatMessageComponent;
import openeye.notes.NoteCategory;
import openeye.notes.NoteLevels;
import openeye.responses.ResponseKnownCrash;

public class ReportedCrashEntry extends NoteEntry {
	private final String url;

	public ReportedCrashEntry(ResponseKnownCrash msg) {
		super(NoteCategory.REPORTED_CRASH, NoteLevels.REPORTED_CRASH_LEVEL);
		this.url = msg.url;
	}

	@Override
	public ChatMessageComponent title() {
		return ChatMessageComponent.createFromTranslationKey("openeye.notes.title.reported_crash");
	}

	@Override
	public ChatMessageComponent content() {
		return ChatMessageComponent.createFromText("");
	}

	@Override
	public String url() {
		return url;
	}
}