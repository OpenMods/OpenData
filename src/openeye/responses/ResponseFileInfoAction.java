package openeye.responses;

import openeye.logic.IContext;
import openeye.protocol.responses.ResponseFileInfo;

import com.google.gson.annotations.SerializedName;

public class ResponseFileInfoAction extends ResponseFileInfo implements IExecutableResponse {

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
