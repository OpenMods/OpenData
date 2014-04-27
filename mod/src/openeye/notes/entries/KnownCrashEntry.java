package openeye.notes.entries;

import java.io.File;

import net.minecraft.util.StatCollector;
import openeye.logic.MainWorker;
import openeye.notes.IconType;
import openeye.responses.ResponseKnownCrash;

import com.google.common.base.Strings;

public class KnownCrashEntry extends NoteEntry {
	private final String crashUrl;
	private final String note;

	public KnownCrashEntry(ResponseKnownCrash msg) {
		super(new File("dummy"), IconType.INFO);
		this.crashUrl = msg.crashUrl;
		this.note = msg.note;
	}

	@Override
	public String title() {
		return StatCollector.translateToLocal("openeye.notes.title.known_crash");
	}

	@Override
	public String description() {
		return Strings.nullToEmpty(note);
	}

	@Override
	public String url() {
		return MainWorker.getOpenEyeUrl(crashUrl);
	}
}