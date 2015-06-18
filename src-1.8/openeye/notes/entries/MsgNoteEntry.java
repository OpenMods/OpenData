package openeye.notes.entries;

import java.io.File;

import openeye.notes.NoteCategory;
import openeye.notes.NoteLevels;
import openeye.notes.WrappedChatComponent;
import openeye.responses.ResponseModMsgAction;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

public class MsgNoteEntry extends NoteEntry {
	private final String description;
	private final String signature;
	private final String payload;

	public MsgNoteEntry(File file, ResponseModMsgAction msg) {
		super(file, calculateFromLevel(msg.level), msg.level);
		this.signature = msg.signature;
		this.description = msg.description;
		this.payload = msg.payload;
	}

	private static NoteCategory calculateFromLevel(int level) {
		if (level > NoteLevels.CRITICAL_LEVEL_THRESHOLD) return NoteCategory.CRITICAL;
		if (level > NoteLevels.ALERT_LEVEL_THRESHOLD) return NoteCategory.ALERT;
		else if (level > NoteLevels.WARNING_LEVEL_THRESHOLD) return NoteCategory.WARNING;
		else return NoteCategory.INFO;
	}

	@Override
	public String url() {
		return null; // TODO
	}

	@Override
	public WrappedChatComponent title() {
		return WrappedChatComponent.createTranslation("openeye.notes.title.note", file.getName());
	}

	@Override
	public WrappedChatComponent content() {
		return WrappedChatComponent.createText(Strings.nullToEmpty(description));
	}

	@Override
	public JsonObject toJson() {
		JsonObject result = super.toJson();
		result.addProperty("signature", signature);
		result.addProperty("payload", payload);
		return result;
	}

}
