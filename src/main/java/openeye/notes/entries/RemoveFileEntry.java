package openeye.notes.entries;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import java.io.File;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import openeye.notes.NoteCategory;
import openeye.notes.NoteLevels;
import openeye.responses.ResponseRemoveFileAction;

public class RemoveFileEntry extends NoteEntry {

	private final String signature;
	private final String url;

	public RemoveFileEntry(File file, ResponseRemoveFileAction msg) {
		super(file, NoteCategory.REMOVE_FILE, NoteLevels.REMOVE_FILE_LEVEL);
		this.signature = msg.signature;
		this.url = msg.url;
	}

	@Override
	public IChatComponent title() {
		return new ChatComponentTranslation("openeye.notes.title.remove_file", file.getName());
	}

	@Override
	public IChatComponent content() {
		return new ChatComponentTranslation("openeye.notes.content.remove_file", file.getName());
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