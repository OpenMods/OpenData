package openeye.logic;

import java.io.File;
import java.util.List;
import java.util.Map;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class InjectedDataStore {

	private InjectedDataStore() {}

	public static final InjectedDataStore instance = new InjectedDataStore();

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
