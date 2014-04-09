package openeye.responses;

import openeye.logic.IContext;
import openeye.reports.IReport;

public class ResponseFileInfo implements IResponse {

	public String signature;

	@Override
	public void execute(IContext context) {
		IReport fileInfo = context.generateFileReport(signature);
		context.queueReport(fileInfo);
	}

}
