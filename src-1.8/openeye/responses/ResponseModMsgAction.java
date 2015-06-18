package openeye.responses;

import java.io.File;

import net.minecraft.nbt.NBTTagCompound;
import openeye.logic.IContext;
import openeye.notes.NoteCollector;
import openeye.protocol.responses.ResponseModMsg;
import net.minecraftforge.fml.common.event.FMLInterModComms;

public class ResponseModMsgAction extends ResponseModMsg implements IExecutableResponse {

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
