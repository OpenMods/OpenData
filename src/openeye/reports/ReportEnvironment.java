package openeye.reports;

import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

public abstract class ReportEnvironment implements IReport {

	public interface RuntimeMeta {}

	public static class FmlForgeRuntime implements RuntimeMeta {
		@SerializedName("fml")
		public String fmlVersion;

		@SerializedName("forge")
		public String forgeVersion;

		@SerializedName("mcp")
		public String mcpVersion;
	}

	@SerializedName("branding")
	public List<String> branding;

	@SerializedName("runtime")
	public RuntimeMeta runtime;

	@SerializedName("minecraft")
	public String minecraft;

	@SerializedName("javaVersion")
	public String javaVersion;

	@SerializedName("tags")
	public Set<String> tags;

	@SerializedName("side")
	public String side;

}
