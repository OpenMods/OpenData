package openeye.config;

import java.io.File;

import com.google.common.collect.Table;

public interface IConfigProcessingEngine {
	public boolean loadConfig(File source, Table<String, String, IConfigPropertyHolder> properties);

	public void dumpConfig(File source, Table<String, String, IConfigPropertyHolder> properties);
}
