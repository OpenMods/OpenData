package openeye;

import net.minecraft.client.Minecraft;
import openeye.logic.Config;
import openeye.logic.Sanitizer;
import openeye.notes.NotesButtonInjector;

import com.google.common.base.Strings;

import cpw.mods.fml.common.FMLCommonHandler;

public abstract class Proxy {

	private static class Client extends Proxy {
		@Override
		public boolean isSnooperEnabled() {
			return Minecraft.getMinecraft().gameSettings.snooperEnabled;
		}

		@Override
		public void first() {
			try {
				String username = Minecraft.getMinecraft().getSession().getUsername();
				if (!Strings.isNullOrEmpty(username)) Sanitizer.addFirst(Sanitizer.replace(username, "[player]"));
			} catch (Throwable t) {
				Log.warn(t, "Failed to get player username");
			}
		}

		@Override
		public void init() {
			if (Config.mainScreenNotes) NotesButtonInjector.registerInjector();
		}
	}

	private static class Server extends Proxy {
		@Override
		public boolean isSnooperEnabled() {
			return true;
		}

		@Override
		public void first() {}

		@Override
		public void init() {}
	}

	public abstract boolean isSnooperEnabled();

	public abstract void first();

	public abstract void init();

	private static Proxy instance;

	public static Proxy instance() {
		if (instance == null) instance = createProxy();
		return instance;
	}

	private static Proxy createProxy() {
		switch (FMLCommonHandler.instance().getSide()) {
			case CLIENT:
				return new Client();
			case SERVER:
				return new Server();
			default:
				throw new IllegalStateException("Impossibru!");
		}
	}

}
