package openeye.responses;

import net.minecraft.nbt.NBTTagCompound;
import openeye.logic.IContext;

import com.google.gson.annotations.SerializedName;

import cpw.mods.fml.common.event.FMLInterModComms;

public class ResponseModMsg implements IResponse {

	@SerializedName("signature")
	public String signature;

	@SerializedName("level")
	public String level;

	@SerializedName("payload")
	public String payload;

	@Override
	public void execute(IContext context) {
		for (String modId : context.getModsForSignature(signature)) {
			NBTTagCompound msg = new NBTTagCompound("IMC");
			msg.setString("level", level);
			msg.setString("payload", payload);
			FMLInterModComms.sendMessage(modId, "EyeNotification", msg);
		}
	}
}
