package openeye.reports;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;

public class ReportKnownFiles implements IReport {
	@SerializedName("signatures")
	public List<SerializableSignature> signatures = ImmutableList.of();
}
