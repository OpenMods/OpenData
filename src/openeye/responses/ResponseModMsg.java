package openeye.responses;

import java.io.File;

import net.minecraft.nbt.NBTTagCompound;
import openeye.logic.IContext;
import openeye.notes.NoteCollector;

import com.google.gson.annotations.SerializedName;

import cpw.mods.fml.common.event.FMLInterModComms;

public class ResponseModMsg implements IResponse {
	public static final String TYPE = "note";

	@Override
	public String getType() {
		return TYPE;
	}

	@SerializedName("signature")
	public String signature;

	@SerializedName("level")
	public int level;

	@SerializedName("payload")
	public String payload;

	@SerializedName("description")
	public String description;

	@Override
	public void execute(IContext context) {
		for (String modId : context.getModsForSignature(signature)) {
			NBTTagCompound msg = new NBTTagCompound();
			msg.setInteger("level", level);
			msg.setString("payload", payload);
			msg.setString("description", description);
			FMLInterModComms.sendMessage(modId, "EyeNotification", msg);
		}

		File file = context.getFileForSignature(signature);
		NoteCollector.INSTANCE.addNote(file, this);
	}
}
