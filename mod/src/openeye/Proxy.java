package openeye;

import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.FMLCommonHandler;

public abstract class Proxy {

	public static class Client extends Proxy {
		@Override
		public boolean isSnooperEnabled() {
			return Minecraft.getMinecraft().gameSettings.snooperEnabled;
		}
	}

	public static class Server extends Proxy {
		@Override
		public boolean isSnooperEnabled() {
			return true;
		}
	}

	public abstract boolean isSnooperEnabled();

	private static Proxy instance;

	public static Proxy instance() {
		if (instance == null) instance = createProxy();
		return instance;
	}

	private static Proxy createProxy() {
		switch (FMLCommonHandler.instance().getEffectiveSide()) {
			case CLIENT:
				return new Client();
			case SERVER:
				return new Server();
			default:
				throw new IllegalStateException("Impossibru!");
		}
	}

}
