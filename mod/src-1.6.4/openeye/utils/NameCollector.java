package openeye.utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetLoginHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import openeye.logic.Sanitizers;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.Player;

public class NameCollector {

	private static void tryAddPlayer(Object obj) {
		if (obj instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)obj;
			Sanitizers.mainSanitizer.addPre(Sanitizers.PRIORITY_PLAYER_NAME, Sanitizers.replaceNoDuplicates(player.username, "[player]"));
		}
	}

	public static class ForgeHooks {
		@ForgeSubscribe(priority = EventPriority.HIGHEST)
		public void onWorldLoad(WorldEvent.Load evt) {
			Sanitizers.addWorldNames(evt.world);
		}

		@ForgeSubscribe(priority = EventPriority.HIGHEST)
		public void onEntityJoin(EntityJoinWorldEvent evt) {
			tryAddPlayer(evt.entity);
		}
	}

	public static class ConnectionHandler implements IConnectionHandler {

		@Override
		public void playerLoggedIn(Player player, NetHandler netHandler, INetworkManager manager) {
			tryAddPlayer(player);
		}

		@Override
		public String connectionReceived(NetLoginHandler netHandler, INetworkManager manager) {
			return null;
		}

		@Override
		public void connectionOpened(NetHandler netClientHandler, String server, int port, INetworkManager manager) {}

		@Override
		public void connectionOpened(NetHandler netClientHandler, MinecraftServer server, INetworkManager manager) {}

		@Override
		public void connectionClosed(INetworkManager manager) {}

		@Override
		public void clientLoggedIn(NetHandler clientHandler, INetworkManager manager, Packet1Login login) {}
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new ForgeHooks());
		NetworkRegistry.instance().registerConnectionHandler(new ConnectionHandler());
	}
}
