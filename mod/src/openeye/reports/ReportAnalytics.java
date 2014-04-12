package openeye.reports;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;

public class ReportAnalytics implements IReport {
	public interface RuntimeMeta {}

	public static class SerializableSignature {
		@SerializedName("signature")
		public String signature;

		@SerializedName("filename")
		public String filename;
	}

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

	@SerializedName("language")
	public String language;

	@SerializedName("locale")
	public String locale;

	@SerializedName("timezone")
	public String timezone;

	@SerializedName("signatures")
	public List<SerializableSignature> signatures = ImmutableList.of();

	@SerializedName("workTime")
	public float workTime;

	@SerializedName("tags")
	public Set<String> tags;
}
