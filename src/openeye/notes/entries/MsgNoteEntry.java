package openeye.notes.entries;

import java.io.File;

import net.minecraft.util.ChatMessageComponent;
import openeye.responses.ResponseModMsg;

import com.google.gson.JsonObject;

public class MsgNoteEntry extends NoteEntry {
	private final String description;
	private final String signature;
	private final int level;
	private final String payload;

	public MsgNoteEntry(File file, ResponseModMsg msg) {
		super(file, calculateIconType(msg.level));
		this.signature = msg.signature;
		this.description = msg.description;
		this.level = msg.level;
		this.payload = msg.payload;
	}

	@Override
	public String url() {
		return null; // TODO
	}

	@Override
	public ChatMessageComponent title() {
		return ChatMessageComponent.createFromTranslationWithSubstitutions("openeye.notes.title.note", file.getName());
	}

	@Override
	public ChatMessageComponent description() {
		return ChatMessageComponent.createFromText(description);
	}

	@Override
	public JsonObject toJson() {
		JsonObject result = super.toJson();
		result.addProperty("signature", signature);
		result.addProperty("level", level);
		result.addProperty("payload", payload);
		return result;
	}

}