package openeye.logic;

import com.google.common.collect.Sets;
import java.io.File;
import java.util.Set;
import openeye.protocol.reports.IReport;
import openeye.struct.TypedCollections.ReportsList;

class ReportContext implements IContext {
	private final ModMetaCollector collector;
	private final ReportsList result = new ReportsList();
	private final Set<String> addedFileInfos = Sets.newHashSet();
	private final Set<String> addedFileContents = Sets.newHashSet();
	private final Set<String> unwantedSignatures = Sets.newHashSet();

	public ReportContext(ModMetaCollector collector) {
		this.collector = collector;
	}

	@Override
	public Set<String> getModsForSignature(String signature) {
		return collector.getModsForSignature(signature);
	}

	@Override
	public File getFileForSignature(String signature) {
		return collector.getContainerForSignature(signature);
	}

	@Override
	public void queueReport(IReport report) {
		result.add(report);
	}

	@Override
	public void queueFileReport(String signature) {
		if (!signature.startsWith("special:")) {
			if (!addedFileInfos.contains(signature)) {
				result.add(collector.generateFileReport(signature));
				addedFileInfos.add(signature);
			}
		}
	}

	@Override
	public void queueFileContents(String signature) {
		// special files allowed - it may be useful to get minecraft.jar contents
		if (!addedFileContents.contains(signature)) {
			result.add(collector.generateFileContentsReport(signature));
			addedFileContents.add(signature);
		}
	}

	@Override
	public void markUnwantedSignature(String signature) {
		unwantedSignatures.add(signature);
	}

	public Set<String> dangerousSignatures() {
		return unwantedSignatures;
	}

	public ReportsList reports() {
		return result;
	}
}