package openeye.responses;

import openeye.Log;
import openeye.logic.IContext;

public class ResponsePong implements IResponse {
	public String payload;

	@Override
	public void execute(IContext context) {
		Log.info("Ping-pong: %s", payload);
	}
}
