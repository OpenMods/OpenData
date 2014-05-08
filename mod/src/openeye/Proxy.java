package openeye;

import java.util.Collection;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import openeye.logic.*;
import openeye.notes.GuiReplacer;
import openeye.reports.FileSignature;

import com.google.common.base.Strings;

import cpw.mods.fml.common.FMLCommonHandler;

public abstract class Proxy {

	private static class Client extends Proxy {
		@Override
		public boolean isSnooperEnabled() {
			return Minecraft.getMinecraft().gameSettings.snooperEnabled;
		}

		@Override
		public Throwable signalDangerousFiles(Collection<FileSignature> dangerousFiles) {
			return new DangerousFileClientException(dangerousFiles);
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
			if (Config.mainScreenNotes) MinecraftForge.EVENT_BUS.register(new GuiReplacer());
		}
	}

	private static class Server extends Proxy {
		@Override
		public boolean isSnooperEnabled() {
			return true;
		}

		@Override
		public Throwable signalDangerousFiles(Collection<FileSignature> dangerousFiles) {
			return new DangerousFileServerException(dangerousFiles);
		}

		@Override
		public void first() {}

		@Override
		public void init() {}
	}

	public abstract boolean isSnooperEnabled();

	public abstract void first();

	public abstract void init();

	public abstract Throwable signalDangerousFiles(Collection<FileSignature> dangerousFiles);

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
