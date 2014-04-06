package openeye.reports;

import argo.jdom.JsonRootNode;

public interface IReportHandler {
	public String getType();

	public void onReportRequest(JsonRootNode input);
}
