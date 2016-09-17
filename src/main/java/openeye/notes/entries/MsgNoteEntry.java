package openeye.notes.entries;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import java.io.File;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import openeye.notes.NoteCategory;
import openeye.notes.NoteLevels;
import openeye.responses.ResponseModMsgAction;

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
	public IChatComponent title() {
		return new ChatComponentTranslation("openeye.notes.title.note", file.getName());
	}

	@Override
	public IChatComponent content() {
		return new ChatComponentText(Strings.nullToEmpty(description));
	}

	@Override
	public JsonObject toJson() {
		JsonObject result = super.toJson();
		result.addProperty("signature", signature);
		result.addProperty("payload", payload);
		return result;
	}

}
