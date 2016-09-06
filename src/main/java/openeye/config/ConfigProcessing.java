package openeye.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import java.io.File;
import java.util.Collection;
import java.util.List;
import openeye.Log;

public class ConfigProcessing {

	public static final IConfigProcessingEngine GSON = new GsonConfigProcessingEngine();

	private static Table<String, String, IConfigPropertyHolder> categorizeProperties(Collection<IConfigPropertyHolder> properties) {
		Table<String, String, IConfigPropertyHolder> result = TreeBasedTable.create();

		for (IConfigPropertyHolder property : properties) {
			IConfigPropertyHolder prev = result.put(property.category(), property.name(), property);
			Preconditions.checkState(prev == null, "Duplicated property %s:%s", property.category(), property.name());
		}

		return result;
	}

	private static void loadAndDump(File configFile, IConfigProcessingEngine engine, final List<IConfigPropertyHolder> holders) {
		final Table<String, String, IConfigPropertyHolder> properties = categorizeProperties(holders);
		final boolean modified = engine.loadConfig(configFile, properties);

		if (modified) {
			Log.info("Detected missing/malformed fields in file %s, updating", configFile);
			engine.dumpConfig(configFile, properties);
		}
	}

	public static void processConfig(File configFile, Class<?> cls, IConfigProcessingEngine engine) {
		final List<IConfigPropertyHolder> holders = ConfigPropertyCollector.collectFromClass(cls);
		loadAndDump(configFile, engine, holders);
	}

	public static void processConfig(File configFile, Object target, boolean excludeStatic, IConfigProcessingEngine engine) {
		Preconditions.checkNotNull(target);
		final List<IConfigPropertyHolder> holders = ConfigPropertyCollector.collectFromInstance(target, excludeStatic);
		loadAndDump(configFile, engine, holders);
	}
}
