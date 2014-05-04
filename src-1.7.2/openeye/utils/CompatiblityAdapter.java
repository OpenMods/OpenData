package openeye.utils;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import cpw.mods.fml.common.FMLCommonHandler;

public class CompatiblityAdapter {

	public static List<String> getBrandings() {
		return FMLCommonHandler.instance().getBrandings(true);
	}

	public static FontRenderer getFontRenderer() {
		return Minecraft.getMinecraft().fontRenderer;
	}
}
