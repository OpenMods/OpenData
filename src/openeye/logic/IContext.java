package openeye.logic;

import java.util.Set;

import openeye.reports.IReport;

public interface IContext {
	public Set<String> getModsForSignature(String signature);

	public void queueReport(IReport report);

	public void queueFileReport(String signature);
}
