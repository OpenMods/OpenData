package openeye;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import net.minecraft.client.Minecraft;
import openeye.logic.Config;
import openeye.logic.Sanitizers;
import openeye.notes.NotesButtonInjector;

import com.google.common.base.Strings;

import net.minecraftforge.fml.common.FMLCommonHandler;

public abstract class Proxy {

	private static class Client extends Proxy {
		@Override
		public boolean isSnooperEnabled() {
			try {
				return Minecraft.getMinecraft().gameSettings.snooperEnabled;
			} catch (Exception e) {
				Log.warn(e, "Can't read client snooper settings, won't send any data");
				return false;
			}
		}

		@Override
		public String getLanguage() {
			return Minecraft.getMinecraft().gameSettings.language;
		}

		@Override
		public void first() {
			try {
				String username = Minecraft.getMinecraft().getSession().getUsername();
				if (!Strings.isNullOrEmpty(username)) Sanitizers.addPlayerName(username);
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
			try {
				File settings = new File("server.properties");
				Properties props = new Properties();
				InputStream input = new FileInputStream(settings);
				try {
					props.load(input);
				} finally {
					input.close();
				}
				String flag = props.getProperty("snooper-enabled");
				// default value for vanilla is also true
				return flag != null? flag.equalsIgnoreCase("true") : true;
			} catch (Exception e) {
				Log.warn(e, "Can't read server snooper settings, won't send any data");
				return false;
			}
		}

		@Override
		public String getLanguage() {
			return "n/a";
		}

		@Override
		public void first() {}

		@Override
		public void init() {}
	}

	public abstract boolean isSnooperEnabled();

	public abstract String getLanguage();

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
