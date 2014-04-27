package openeye.notes.entries;

import java.io.File;

import net.minecraft.util.ChatMessageComponent;
import openeye.notes.IconType;
import openeye.responses.ResponseKnownCrash;

import com.google.common.base.Strings;

public class KnownCrashEntry extends NoteEntry {
	private final String url;
	private final String note;

	public KnownCrashEntry(ResponseKnownCrash msg) {
		super(new File("dummy"), IconType.INFO);
		this.url = msg.url;
		this.note = msg.note;
	}

	@Override
	public ChatMessageComponent title() {
		return ChatMessageComponent.createFromTranslationKey("openeye.notes.title.known_crash");
	}

	@Override
	public ChatMessageComponent description() {
		return ChatMessageComponent.createFromText(Strings.nullToEmpty(note));
	}

	@Override
	public String url() {
		return url;
	}
}