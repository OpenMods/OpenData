package openeye.utils;

import net.minecraftforge.event.world.WorldEvent;
import openeye.logic.Sanitizers;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class WorldNameSanitizer {

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load evt) {
		Sanitizers.addWorldNames(evt.world);
	}
}
