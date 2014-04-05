package openeye.reports;

import static argo.jdom.JsonNodeFactories.string;

import java.util.Map;

import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;

public class InitialReportBuilder implements IReportBuilder {

	@Override
	public String getType() {
		return "analytics";
	}

	@Override
	public void createReport(JsonRootNode input, Map<String, JsonNode> result) {
		result.put("test", string("Helląąą!"));
	}

}
