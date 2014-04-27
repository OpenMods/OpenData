package openeye.notes.entries;

import java.io.File;

import net.minecraft.util.ChatMessageComponent;
import openeye.notes.IconType;
import openeye.responses.ResponseDangerousFile;

import com.google.gson.JsonObject;

public class DangerousFileEntry extends NoteEntry {

	private final String signature;

	public DangerousFileEntry(File file, ResponseDangerousFile msg) {
		super(file, IconType.CRITICAL);
		this.signature = msg.signature;
	}

	@Override
	public ChatMessageComponent title() {
		return ChatMessageComponent.createFromTranslationWithSubstitutions("openeye.notes.title.dangerous_file", file.getName());
	}

	@Override
	public ChatMessageComponent description() {
		return ChatMessageComponent.createFromTranslationWithSubstitutions("openeye.notes.dangerous_file", file.getName());
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