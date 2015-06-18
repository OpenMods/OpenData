package openeye.utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import openeye.logic.Sanitizers;

import com.mojang.authlib.GameProfile;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public abstract class NameCollector {

	private static void tryAddPlayer(EntityPlayer player) {
		GameProfile profile = player.getGameProfile();
		if (profile != null) {
			Sanitizers.mainSanitizer.addPre(Sanitizers.PRIORITY_PLAYER_ID, Sanitizers.replaceNoDuplicates(profile.getId(), "[player id]"));
			Sanitizers.mainSanitizer.addPre(Sanitizers.PRIORITY_PLAYER_NAME, Sanitizers.replaceNoDuplicates(profile.getName(), "[player name]"));
		}
	}

	public static class ForgeHooks {
		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public void onWorldLoad(WorldEvent.Load evt) {
			Sanitizers.addWorldNames(evt.world);
		}

		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public void onEntityJoin(EntityJoinWorldEvent evt) {
			if (evt.entity instanceof EntityPlayer) tryAddPlayer((EntityPlayer)evt.entity);
		}
	}

	public static class FmlHooks {
		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent evt) {
			tryAddPlayer(evt.player);
		}
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new ForgeHooks());
		FMLCommonHandler.instance().bus().register(new FmlHooks());
	}
}
