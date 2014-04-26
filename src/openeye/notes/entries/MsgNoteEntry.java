package openeye.notes.entries;

import java.io.File;

import net.minecraft.util.StatCollector;
import openeye.responses.ResponseModMsg;

public class MsgNoteEntry extends NoteEntry {
	public MsgNoteEntry(File file, ResponseModMsg msg) {
		super(file, calculateIconType(msg.level));
		this.msg = msg;
	}

	public final ResponseModMsg msg;

	@Override
	public String url() {
		return null; // TODO
	}

	@Override
	public String title() {
		return StatCollector.translateToLocalFormatted("openeye.notes.title.note", file.getName());
	}

	@Override
	public String description() {
		return msg.description;
	}
}