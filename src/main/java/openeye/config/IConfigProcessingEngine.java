package openeye.config;

import com.google.common.collect.Table;
import java.io.File;

public interface IConfigProcessingEngine {
	public boolean loadConfig(File source, Table<String, String, IConfigPropertyHolder> properties);

	public void dumpConfig(File source, Table<String, String, IConfigPropertyHolder> properties);
}
