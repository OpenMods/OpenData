package openeye;

import java.util.Collection;

import net.minecraft.client.Minecraft;
import openeye.logic.DangerousFileClientException;
import openeye.logic.DangerousFileServerException;
import openeye.reports.FileSignature;
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
	}

	public abstract boolean isSnooperEnabled();

	public abstract Throwable signalDangerousFiles(Collection<FileSignature> dangerousFiles);

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
