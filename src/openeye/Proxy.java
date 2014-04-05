package openeye;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

public interface Proxy {

	public File getFile(String path);

	public static class Client implements Proxy {
		@Override
		public File getFile(String path) {
			return new File(Minecraft.getMinecraft().mcDataDir, path);
		}
	}

	public static class Server implements Proxy {
		@Override
		public File getFile(String path) {
			return MinecraftServer.getServer().getFile(path);
		}
	}

}
