package openeye.requests;

import openeye.logic.IContext;
import openeye.reports.IReport;

public interface IRequest {

	public IReport createReport(IContext context);

}
