package openeye.logic;

import java.util.Set;

import openeye.reports.IReport;
import openeye.reports.ReportFileInfo;

public interface IContext {
	public ReportFileInfo generateFileReport(String signature);

	public Set<String> getModsForSignature(String signature);

	public void queueReport(IReport report);
}
