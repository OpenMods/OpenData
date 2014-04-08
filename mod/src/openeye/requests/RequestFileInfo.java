package openeye.requests;

import openeye.logic.IContext;

public class RequestFileInfo implements IRequest {

	public String signature;

	@Override
	public Object createReport(IContext context) {
		return context.generateFileReport(signature);
	}

}
