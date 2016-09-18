package openeye.notes.entries;

import com.google.common.base.Strings;
import net.minecraft.util.ChatMessageComponent;
import openeye.notes.NoteCategory;
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
	public ChatMessageComponent title() {
		return ChatMessageComponent.createFromTranslationKey("openeye.notes.title.resolved_crash");
	}

	@Override
	public ChatMessageComponent content() {
		return ChatMessageComponent.createFromText(Strings.nullToEmpty(note));
	}

	@Override
	public String url() {
		return url;
	}
}
