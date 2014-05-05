package openeye.reports;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.annotations.SerializedName;

public class ReportAnalytics implements IReport {
	public static final String TYPE = "analytics";

	@Override
	public String getType() {
		return TYPE;
	}

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

	@SerializedName("language")
	public String language;

	@SerializedName("locale")
	public String locale;

	@SerializedName("timezone")
	public String timezone;

	@SerializedName("signatures")
	public List<FileSignature> signatures = ImmutableList.of();

	@SerializedName("addedSignatures")
	public Set<String> addedSignatures = ImmutableSet.of();

	@SerializedName("removedSignatures")
	public Set<String> removedSignatures = ImmutableSet.of();

	@SerializedName("workTime")
	public float workTime;

	@SerializedName("tags")
	public Set<String> tags;
}
