package openeye.storage;

import java.io.File;

import net.minecraft.crash.CrashReport;
import openeye.logic.GsonUtils;
import openeye.logic.PermanentStorage;
import openeye.reports.ReportsList;

public class Storages {
	public final IQueryableStorage<PermanentStorage> permanent;
	public final IWorkingStorage<CrashReport> pendingCrashes;
	public final IAppendableStorage<ReportsList> sentReports;

	public Storages(File mcDir) {
		File eyeDir = new File(mcDir, "reports");
		eyeDir.mkdir();

		permanent = new GsonPredefinedStorage<PermanentStorage>(eyeDir, PermanentStorage.class, GsonUtils.PRETTY_GSON, "installed-mods");
		pendingCrashes = new GsonWorkingStorage<CrashReport>(eyeDir, "pending-crash", CrashReport.class, GsonUtils.PRETTY_GSON);
		sentReports = new GsonArchiveStorage<ReportsList>(eyeDir, "report", ReportsList.class, GsonUtils.PRETTY_GSON);
	}
}
