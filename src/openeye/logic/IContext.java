package openeye.logic;

import openeye.reports.ReportFileInfo;

public interface IContext {
	public ReportFileInfo generateFileReport(String signature);
}
