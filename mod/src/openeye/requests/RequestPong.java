package openeye.requests;

import openeye.Log;
import openeye.logic.IContext;
import openeye.reports.IReport;

public class RequestPong implements IRequest {
	public String payload;

	@Override
	public IReport createReport(IContext context) {
		Log.info("Ping-pong: %s", payload);
		return null;
	}
}
