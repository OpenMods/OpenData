package openeye.responses;

import openeye.logic.IContext;

public class ResponseFileContents implements IResponse {
	public static final String TYPE = "file_contents";

	@Override
	public String getType() {
		return TYPE;
	}

	public String signature;

	@Override
	public void execute(IContext context) {
		context.queueFileContents(signature);
	}

}
