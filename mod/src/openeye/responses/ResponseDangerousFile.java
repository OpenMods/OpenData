package openeye.responses;

import openeye.logic.IContext;

public class ResponseDangerousFile implements IResponse {

	public String signature;

	@Override
	public void execute(IContext context) {
		context.markDangerousSignature(signature);
	}

}
