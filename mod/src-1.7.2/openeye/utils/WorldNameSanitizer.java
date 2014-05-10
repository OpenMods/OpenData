package openeye.utils;

import net.minecraftforge.event.world.WorldEvent;
import openeye.logic.Sanitizer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class WorldNameSanitizer {

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load evt) {
		Sanitizer.addWorldNames(evt.world);
	}
}
