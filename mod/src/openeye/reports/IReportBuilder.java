package openeye.reports;

import java.util.Map;

import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;

public interface IReportBuilder {
	public String getType();

	public void createReport(JsonRootNode input, Map<String, JsonNode> result);
}
