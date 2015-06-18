package openeye.responses;

import openeye.Log;
import openeye.logic.IContext;
import openeye.protocol.responses.ResponseError;

public class ResponseErrorAction extends ResponseError implements IExecutableResponse {

	@Override
	public void execute(IContext context) {
		Log.warn("Server failed to parse report %d (type: %s)", reportIndex, reportType);
	}

}
