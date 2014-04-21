package openeye.responses;

import openeye.logic.IContext;

public class ResponseDangerousFile implements IResponse {
	public static final String TYPE = "dangerous_file";

	@Override
	public String getType() {
		return TYPE;
	}

	public String signature;

	@Override
	public void execute(IContext context) {
		context.markDangerousSignature(signature);
	}

}
