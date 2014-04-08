package openeye.storage;

import java.io.File;

import net.minecraft.crash.CrashReport;
import openeye.logic.*;
import openeye.logic.TypedCollections.ReportsList;
import openeye.logic.TypedCollections.RequestsList;

public class Storages {
	public final IQueryableStorage<PermanentStorage> permanent;
	public final IWorkingStorage<CrashReport> pendingCrashes;
	public final IAppendableStorage<ReportsList> sentReports;
	public final IAppendableStorage<RequestsList> receivedRequests;

	public Storages(File mcDir) {
		File eyeDir = new File(mcDir, "reports");
		eyeDir.mkdir();

		permanent = new GsonPredefinedStorage<PermanentStorage>(eyeDir, PermanentStorage.class, GsonUtils.PRETTY_GSON, "installed-mods");
		pendingCrashes = new GsonWorkingStorage<CrashReport>(eyeDir, "pending-crash", CrashReport.class, GsonUtils.PRETTY_GSON);
		sentReports = new GsonArchiveStorage<ReportsList>(eyeDir, "report", "report.json", ReportsList.class, GsonUtils.PRETTY_GSON);
		receivedRequests = new GsonArchiveStorage<RequestsList>(eyeDir, "request", "request.json", RequestsList.class, GsonUtils.PRETTY_GSON);
	}
}
