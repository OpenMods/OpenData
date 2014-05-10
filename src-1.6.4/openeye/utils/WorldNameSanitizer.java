package openeye.utils;

import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent;
import openeye.logic.Sanitizer;

public class WorldNameSanitizer {

	@ForgeSubscribe
	public void onWorldLoad(WorldEvent.Load evt) {
		Sanitizer.addWorldNames(evt.world);
	}
}
