package openeye.notes;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.ForgeSubscribe;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class GuiReplacer {

	@ForgeSubscribe
	@SideOnly(Side.CLIENT)
	public void onGuiOpen(GuiOpenEvent evt) {
		// Only exact class match, to prevent interfering with other mods
		if (evt.gui != null && evt.gui.getClass() == GuiMainMenu.class) evt.gui = new GuiMainMenuAddon();
	}

}
