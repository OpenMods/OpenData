package openeye.notes;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import openeye.Log;

public class NotesButtonInjector {

	@SideOnly(Side.CLIENT)
	public static class FallbackReplacer {
		@SubscribeEvent
		public void onGuiOpen(GuiOpenEvent evt) {
			// Only exact class match, to prevent interfering with other mods
			if (evt.gui != null && evt.gui.getClass() == GuiMainMenu.class) evt.gui = new GuiMainMenuAddon();
		}
	}

	@SideOnly(Side.CLIENT)
	public static class EventBasedReplacer {
		@SubscribeEvent
		public void onGuiInit(InitGuiEvent evt) {
			if (evt.gui instanceof GuiMainMenu) GuiMainMenuAddon.onGuiInit(evt.gui, evt.buttonList);
		}

		@SubscribeEvent
		public void onGuiInit(DrawScreenEvent.Post evt) {
			if (evt.gui instanceof GuiMainMenu) GuiMainMenuAddon.onScreenDraw(evt.gui);
		}

		@SubscribeEvent
		public void onActionPerformed(ActionPerformedEvent evt) {
			if (evt.gui instanceof GuiMainMenu) GuiMainMenuAddon.onActionPerformed(evt.gui.mc, evt.gui, evt.button);
		}
	}

	public static void registerInjector() {
		try {
			Class.forName("net.minecraftforge.client.event.GuiScreenEvent");
			MinecraftForge.EVENT_BUS.register(new EventBasedReplacer());
		} catch (ClassNotFoundException e) {
			Log.info("Old forge version, using fallback GUI modification method");
			MinecraftForge.EVENT_BUS.register(new FallbackReplacer());
		}
	}
}
