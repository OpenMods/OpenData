package openeye.storage;

import java.io.File;

import argo.jdom.JsonRootNode;

public class Storages {
	public final IQueryableStorage<JsonRootNode> permanent;
	public final IWorkingStorage<JsonRootNode> pendingCrashes;
	public final IAppendableStorage<JsonRootNode> sentReports;

	public Storages(File mcDir) {
		File eyeDir = new File(mcDir, "reports");
		eyeDir.mkdir();

		permanent = new JsonPredefinedStorage(eyeDir, "installed-mods");
		pendingCrashes = new JsonWorkingStorage(eyeDir, "pending-crash");
		sentReports = new JsonArchiveStorage(eyeDir, "report");
	}
}
