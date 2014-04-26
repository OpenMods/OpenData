package openeye.notes.entries;

import java.io.File;

import net.minecraft.util.StatCollector;
import openeye.logic.MainWorker;
import openeye.notes.IconType;
import openeye.responses.ResponseKnownCrash;

public class KnownCrashEntry extends NoteEntry {
	private final String crashUrl;

	public KnownCrashEntry(ResponseKnownCrash msg) {
		super(new File("dummy"), IconType.INFO);
		this.crashUrl = msg.crashUrl;
	}

	@Override
	public String title() {
		return StatCollector.translateToLocal("openeye.notes.title.known_crash");
	}

	@Override
	public String description() {
		return "";
	}

	@Override
	public String url() {
		return MainWorker.getOpenEyeUrl(crashUrl);
	}
}