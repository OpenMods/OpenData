package openeye.responses;

import openeye.logic.IContext;

import com.google.gson.annotations.SerializedName;

public class ResponseFileInfo implements IResponse {
	public static final String TYPE = "file_info";

	@Override
	public String getType() {
		return TYPE;
	}

	@SerializedName("signature")
	public String signature;

	@Override
	public void execute(IContext context) {
		context.queueFileReport(signature);
	}

}
