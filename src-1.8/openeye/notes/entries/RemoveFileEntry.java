package openeye.notes.entries;

import java.io.File;

import openeye.notes.NoteCategory;
import openeye.notes.NoteLevels;
import openeye.notes.WrappedChatComponent;
import openeye.responses.ResponseRemoveFileAction;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

public class RemoveFileEntry extends NoteEntry {

	private final String signature;
	private final String url;

	public RemoveFileEntry(File file, ResponseRemoveFileAction msg) {
		super(file, NoteCategory.REMOVE_FILE, NoteLevels.REMOVE_FILE_LEVEL);
		this.signature = msg.signature;
		this.url = msg.url;
	}

	@Override
	public WrappedChatComponent title() {
		return WrappedChatComponent.createTranslation("openeye.notes.title.remove_file", file.getName());
	}

	@Override
	public WrappedChatComponent content() {
		return WrappedChatComponent.createTranslation("openeye.notes.content.remove_file", file.getName());
	}

	@Override
	public String url() {
		return Strings.nullToEmpty(url);
	}

	@Override
	public JsonObject toJson() {
		JsonObject result = super.toJson();
		result.addProperty("signature", signature);
		return result;
	}
}