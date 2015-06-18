package openeye.logic;

import java.io.File;

import openeye.protocol.reports.ReportCrash;
import openeye.storage.*;

import com.google.common.base.Preconditions;

public class Storages {
	public static final String STATE_FILE_ID = "state";

	public final IQueryableStorage<ModState> state;
	public final IWorkingStorage<ReportCrash> pendingCrashes;
	public final IAppendableStorage<Object> sessionArchive;

	public final File reportsDir;

	private static Storages instance;

	private Storages(File mcDir) {
		reportsDir = new File(mcDir, "reports");
		reportsDir.mkdir();

		state = new GsonPredefinedStorage<ModState>(reportsDir, ModState.class, GsonUtils.PRETTY_GSON, STATE_FILE_ID);
		pendingCrashes = new GsonWorkingStorage<ReportCrash>(reportsDir, "pending-crash", ReportCrash.class, GsonUtils.PRETTY_GSON);
		sessionArchive = new GsonSessionStorage<Object>(reportsDir, "json", Object.class, GsonUtils.PRETTY_GSON);
	}

	public static Storages init(File mcDir) {
		if (instance == null) instance = new Storages(mcDir);
		return instance;
	}

	public static Storages instance() {
		Preconditions.checkNotNull(instance, "Storage not initialized");
		return instance;
	}
}
