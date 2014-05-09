package openeye.reports;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.annotations.SerializedName;

public class ReportAnalytics extends ReportEnvironment {
	public static final String TYPE = "analytics";

	@Override
	public String getType() {
		return TYPE;
	}

	@SerializedName("signatures")
	public List<FileSignature> signatures = ImmutableList.of();

	@SerializedName("addedSignatures")
	public Set<String> addedSignatures = ImmutableSet.of();

	@SerializedName("removedSignatures")
	public Set<String> removedSignatures = ImmutableSet.of();

	@SerializedName("workTime")
	public float workTime;

	@SerializedName("language")
	public String language;

	@SerializedName("locale")
	public String locale;

	@SerializedName("timezone")
	public String timezone;
}
