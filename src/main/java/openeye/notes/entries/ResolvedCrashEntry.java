package openeye.notes.entries;

import com.google.common.base.Strings;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
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
	public ITextComponent title() {
		return new TextComponentTranslation("openeye.notes.title.resolved_crash");
	}

	@Override
	public ITextComponent content() {
		return new TextComponentString(Strings.nullToEmpty(note));
	}

	@Override
	public String url() {
		return url;
	}
}
