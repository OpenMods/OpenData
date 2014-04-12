package openeye.responses;

import openeye.logic.IContext;

public class ResponseFileInfo implements IResponse {

	public String signature;

	@Override
	public void execute(IContext context) {
		context.queueFileReport(signature);
	}

}
