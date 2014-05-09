package openeye.reports;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

public class ReportCrash implements IReport {
	public static final String TYPE = "crashlog";

	@Override
	public String getType() {
		return TYPE;
	}

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

	public static class ExceptionInfo {

		@SerializedName("stack")
		public List<StackTrace> stackTrace;

		@SerializedName("exception")
		public String exceptionCls;

		@SerializedName("message")
		public String message;

		@SerializedName("cause")
		public ExceptionInfo cause;
	}

	@SerializedName("states")
	public List<FileState> states;

	@SerializedName("timestamp")
	public long timestamp;

	@SerializedName("location")
	public String location;

	@SerializedName("exception")
	public ExceptionInfo exception;

	@SerializedName("tags")
	public Set<String> tags;

	@SerializedName("javaVersion")
	public String javaVersion;

	@SerializedName("random")
	public int random;

	@SerializedName("side")
	public String side;

	@SerializedName("resolved")
	public boolean resolved;
}
