package openeye.notes.entries;

import java.io.File;

import net.minecraft.util.StatCollector;
import openeye.notes.IconType;

public class DangerousFileEntry extends NoteEntry {
	@SuppressWarnings("unused")
	private final String signature;

	public DangerousFileEntry(File file, String signature) {
		super(file, IconType.DANGER);
		this.signature = signature;
	}

	@Override
	public String title() {
		return StatCollector.translateToLocalFormatted("openeye.notes.title.dangerous_file", file.getName());
	}

	@Override
	public String description() {
		return StatCollector.translateToLocalFormatted("openeye.notes.dangerous_file", file.getName());
	}

	@Override
	public String url() {
		return null; // MainWorker.getOpenEyeUrl("achtung/" + signature);
	}
}