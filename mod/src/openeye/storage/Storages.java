package openeye.storage;

import java.io.File;

import openeye.logic.GsonUtils;
import openeye.logic.ModState;
import openeye.reports.ReportCrash;

public class Storages {
	public static final String STATE_FILE_ID = "state";

	public final IQueryableStorage<ModState> state;
	public final IWorkingStorage<ReportCrash> pendingCrashes;
	public final IAppendableStorage<Object> sessionArchive;

	public Storages(File mcDir) {
		File eyeDir = new File(mcDir, "reports");
		eyeDir.mkdir();

		state = new GsonPredefinedStorage<ModState>(eyeDir, ModState.class, GsonUtils.PRETTY_GSON, STATE_FILE_ID);
		pendingCrashes = new GsonWorkingStorage<ReportCrash>(eyeDir, "pending-crash", ReportCrash.class, GsonUtils.PRETTY_GSON);
		sessionArchive = new GsonSessionStorage<Object>(eyeDir, "json", Object.class, GsonUtils.PRETTY_GSON);
	}
}
