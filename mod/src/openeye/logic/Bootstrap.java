package openeye.logic;

import java.io.File;
import java.util.List;
import java.util.Map;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import com.google.common.base.Preconditions;

public class Bootstrap {

	private Bootstrap() {}

	public static final Bootstrap instance = new Bootstrap();

	private boolean runtimeDeobfuscationEnabled;

	private List<ITweaker> tweakers;

	private LaunchClassLoader loader;

	private File mcLocation;

	private File selfLocation;

	@SuppressWarnings("unchecked")
	public void populateFromInject(Map<String, Object> data) {
		runtimeDeobfuscationEnabled = (Boolean)data.get("runtimeDeobfuscationEnabled");
		tweakers = (List<ITweaker>)data.get("coremodList");
	}

	public void populateFromSetupClass(Map<String, Object> data) {
		loader = (LaunchClassLoader)data.get("classLoader");
		mcLocation = (File)data.get("mcLocation");
		selfLocation = (File)data.get("coremodLocation");
	}

	public void startup() {
		Preconditions.checkNotNull(mcLocation, "Failed to start OpenEye, no minecraft folder available");

		Config.load(mcLocation);

		Storages storages = Storages.init(mcLocation);
		StateHolder.init(storages);

		Sanitizers.addMinecraftPath(mcLocation);
		ThrowableLogger.init();
	}

	public boolean isRuntimeDeobfuscationEnabled() {
		return runtimeDeobfuscationEnabled;
	}

	public List<ITweaker> getTweakers() {
		return tweakers;
	}

	public LaunchClassLoader getLoader() {
		return loader;
	}

	public File getMcLocation() {
		return mcLocation;
	}

	public File getSelfLocation() {
		return selfLocation;
	}
}
