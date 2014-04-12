package openeye.reports;

import java.util.Collection;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ReportCrash implements IReport {

	public static class StackTrace {
		@SerializedName("class")
		public String className;

		@SerializedName("method")
		public String methodName;

		@SerializedName("file")
		public String fileName;

		@SerializedName("line")
		public int lineNumber;

		@SerializedName("signatures")
		public Collection<String> signatures;
	}

	public static class ModState {
		@SerializedName("modId")
		public String modId;

		@SerializedName("state")
		public String state;
	}

	public static class FileState {
		@SerializedName("signature")
		public String signature;

		@SerializedName("mods")
		public List<ModState> mods;
	}

	@SerializedName("states")
	public List<FileState> states;

	@SerializedName("stack")
	public List<StackTrace> stackTrace;

	@SerializedName("exception")
	public String exceptionCls;

	@SerializedName("message")
	public String message;

	@SerializedName("timestamp")
	public long timestamp;
}
