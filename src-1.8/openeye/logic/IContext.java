package openeye.logic;

import java.io.File;
import java.util.Set;

import openeye.protocol.reports.IReport;

public interface IContext {
	public Set<String> getModsForSignature(String signature);

	public void queueReport(IReport report);

	public void queueFileReport(String signature);

	public void queueFileContents(String signature);

	public void markUnwantedSignature(String signature);

	public File getFileForSignature(String signature);
}
