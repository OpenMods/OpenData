package openeye.storage;

import java.io.File;

import openeye.logic.*;
import openeye.logic.TypedCollections.ReportsList;
import openeye.logic.TypedCollections.ResponseList;
import openeye.reports.ReportCrash;

public class Storages {
	public static final String CONFIG_ID = "config";
	public static final String INSTALLED_MODS_ID = "installed-mods";

	public final IQueryableStorage<LogList> installedMods;
	public final IQueryableStorage<Config> config;
	public final IWorkingStorage<ReportCrash> pendingCrashes;
	public final IAppendableStorage<ReportsList> sentReports;
	public final IAppendableStorage<ResponseList> receivedRequests;

	public Storages(File mcDir) {
		File eyeDir = new File(mcDir, "reports");
		eyeDir.mkdir();

		installedMods = new GsonPredefinedStorage<LogList>(eyeDir, LogList.class, GsonUtils.PRETTY_GSON, INSTALLED_MODS_ID);
		config = new GsonPredefinedStorage<Config>(eyeDir, Config.class, GsonUtils.PRETTY_GSON, CONFIG_ID);
		pendingCrashes = new GsonWorkingStorage<ReportCrash>(eyeDir, "pending-crash", ReportCrash.class, GsonUtils.PRETTY_GSON);
		sentReports = new GsonArchiveStorage<ReportsList>(eyeDir, "report", "report.json", ReportsList.class, GsonUtils.PRETTY_GSON);
		receivedRequests = new GsonArchiveStorage<ResponseList>(eyeDir, "request", "request.json", ResponseList.class, GsonUtils.PRETTY_GSON);
	}
}
