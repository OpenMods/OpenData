package openeye.notes.entries;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import openeye.notes.NoteCategory;
import openeye.notes.NoteLevels;
import openeye.responses.ResponseKnownCrashAction;

public class ReportedCrashEntry extends NoteEntry {
	private final String url;

	public ReportedCrashEntry(ResponseKnownCrashAction msg) {
		super(NoteCategory.REPORTED_CRASH, NoteLevels.REPORTED_CRASH_LEVEL);
		this.url = msg.url;
	}

	@Override
	public ITextComponent title() {
		return new TextComponentTranslation("openeye.notes.title.reported_crash");
	}

	@Override
	public ITextComponent content() {
		return new TextComponentString("");
	}

	@Override
	public String url() {
		return url;
	}
}