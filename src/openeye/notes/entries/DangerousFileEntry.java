package openeye.notes.entries;

import java.io.File;

import openeye.notes.NoteCategory;
import openeye.notes.NoteLevels;
import openeye.notes.WrappedChatComponent;
import openeye.responses.ResponseDangerousFile;

import com.google.gson.JsonObject;

public class DangerousFileEntry extends NoteEntry {

	private final String signature;

	public DangerousFileEntry(File file, ResponseDangerousFile msg) {
		super(file, NoteCategory.DANGEROUS_FILE, NoteLevels.DANGEROUS_FILE_LEVEL);
		this.signature = msg.signature;
	}

	@Override
	public WrappedChatComponent title() {
		return WrappedChatComponent.createTranslation("openeye.notes.title.dangerous_file", file.getName());
	}

	@Override
	public WrappedChatComponent content() {
		return WrappedChatComponent.createTranslation("openeye.notes.content.dangerous_file", file.getName());
	}

	@Override
	public String url() {
		return null;
	}

	@Override
	public JsonObject toJson() {
		JsonObject result = super.toJson();
		result.addProperty("signature", signature);
		return result;
	}
}