package openeye.responses;

import openeye.Log;
import openeye.logic.IContext;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class ResponseError implements IResponse {
	public static final String TYPE = "error";

	@Override
	public String getType() {
		return TYPE;
	}

	@SerializedName("reportType")
	public String reportType;

	@SerializedName("reportIndex")
	public int reportIndex;

	@SerializedName("debug")
	public JsonElement debug;

	@Override
	public void execute(IContext context) {
		Log.warn("Server failed to parse report %d (type: %s)", reportIndex, reportType);
	}

}
