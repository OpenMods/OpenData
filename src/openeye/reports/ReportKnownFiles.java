package openeye.reports;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;

public class ReportKnownFiles implements IReport {
	public static final String TYPE = "known_files";

	@Override
	public String getType() {
		return TYPE;
	}

	@SerializedName("signatures")
	public List<FileSignature> signatures = ImmutableList.of();
}
