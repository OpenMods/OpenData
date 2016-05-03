package openeye.responses;

import openeye.logic.IContext;
import openeye.protocol.responses.ResponseFileContents;

public class ResponseFileContentsAction extends ResponseFileContents implements IExecutableResponse {

	@Override
	public void execute(IContext context) {
		context.queueFileContents(signature);
	}
}
