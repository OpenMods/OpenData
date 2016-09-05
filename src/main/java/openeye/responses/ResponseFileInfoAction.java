package openeye.responses;

import openeye.logic.IContext;
import openeye.protocol.responses.ResponseFileInfo;

public class ResponseFileInfoAction extends ResponseFileInfo implements IExecutableResponse {

	@Override
	public void execute(IContext context) {
		context.queueFileReport(signature);
	}

}
