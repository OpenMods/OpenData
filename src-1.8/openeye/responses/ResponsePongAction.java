package openeye.responses;

import openeye.Log;
import openeye.logic.IContext;
import openeye.protocol.responses.ResponsePong;

public class ResponsePongAction extends ResponsePong implements IExecutableResponse {

	@Override
	public void execute(IContext context) {
		Log.info("Ping-pong: %s", payload);
	}
}
