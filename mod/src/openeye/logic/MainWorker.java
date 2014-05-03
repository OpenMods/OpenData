package openeye.logic;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import openeye.Log;
import openeye.config.ConfigProcessing;
import openeye.logic.TypedCollections.ReportsList;
import openeye.logic.TypedCollections.ResponseList;
import openeye.net.GenericSender.FailedToSend;
import openeye.net.ReportSender;
import openeye.reports.*;
import openeye.responses.IResponse;
import openeye.storage.IDataSource;
import openeye.storage.Storages;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.discovery.ASMDataTable;

public final class MainWorker {

	public static String getOpenEyeUrl(String resource) {
		return "http://openeye.openmods.info/" + resource;
	}

	private final class Context implements IContext {
		private final ReportsList result = new ReportsList();
		private final Set<String> addedFileInfos = Sets.newHashSet();
		private final Set<String> addedFileContents = Sets.newHashSet();

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
			if (!addedFileInfos.contains(signature)) {
				result.add(collector.generateFileReport(signature));
				addedFileInfos.add(signature);
			}
		}

		@Override
		public void queueFileContents(String signature) {
			if (!addedFileContents.contains(signature)) {
				result.add(collector.generateFileContentsReport(signature));
				addedFileContents.add(signature);
			}
		}

		@Override
		public void markDangerousSignature(String signature) {
			dangerousSignatures.add(signature);
		}

		public ReportsList reports() {
			return result;
		}
	}

	private static class ThrowableEntry {
		public final String location;
		public final Throwable throwable;

		public ThrowableEntry(Throwable throwable, String location) {
			this.location = location;
			this.throwable = throwable;
		}
	}

	private final String url = getOpenEyeUrl("api/v1/data");

	private ModMetaCollector collector;

	private Storages storages;

	private CountDownLatch canContinueLoading = new CountDownLatch(1);

	private static List<ThrowableEntry> lethalExceptions = Lists.newArrayList();

	private final boolean sendReports;

	private final Set<String> dangerousSignatures = Sets.newHashSet();

	public MainWorker(boolean sendReports) {
		this.sendReports = sendReports;
	}

	public static void storeThrowableForReport(Throwable throwable, String location) {
		if (!(throwable instanceof INotStoredCrash)) lethalExceptions.add(new ThrowableEntry(throwable, location));
	}

	private void storeReport(ReportsList report) {
		IDataSource<Object> list = storages.sessionArchive.createNew("report");
		list.store(report);
	}

	private void storeRequest(ResponseList report) {
		IDataSource<Object> list = storages.sessionArchive.createNew("request");
		list.store(report);
	}

	private static void filterStructs(Collection<? extends ITypedStruct> structs, Set<String> blacklist) {
		Iterator<? extends ITypedStruct> it = structs.iterator();

		while (it.hasNext()) {
			final ITypedStruct struct = it.next();
			final String type = struct.getType();
			if (blacklist.contains(type)) {
				Log.info("Filtered type %s(%s) from list, since it's on blacklist", type, struct);
				it.remove();
			}
		}
	}

	public ReportsList executeResponses(ResponseList requests) {
		Preconditions.checkState(!requests.isEmpty());

		final Context context = new Context();

		for (IResponse request : requests)
			request.execute(context);

		return context.reports();
	}

	private void sendReports(ModState state) {
		final ReportsList initialReports = new ReportsList();

		try {
			if (Config.scanOnly) {
				initialReports.add(ReportBuilders.buildKnownFilesReport(collector));
			} else {
				initialReports.add(ReportBuilders.buildAnalyticsReport(collector, state.installedMods));
			}

			if (Config.pingOnInitialReport) initialReports.add(new ReportPing());

		} catch (Exception e) {
			logException(e, "Failed to create initial report");
			return;
		}

		for (IDataSource<ReportCrash> crash : storages.pendingCrashes.listAll()) {
			try {
				ReportCrash report = crash.retrieve();
				if (report != null) initialReports.add(report);
			} catch (Exception e) {
				logException(e, "Failed to add crash report %s", crash.getId());
			}
		}

		try {
			ReportSender sender = new ReportSender(url);

			ReportsList currentReports = initialReports;

			while (!currentReports.isEmpty()) {
				try {
					storeReport(currentReports);
				} catch (Exception e) {
					Log.warn(e, "Failed to store report");
				}

				filterStructs(currentReports, Config.reportsBlacklist);
				ResponseList response = Config.dontSend? null : sender.sendAndReceive(currentReports);

				if (response == null || response.isEmpty()) break;
				filterStructs(response, Config.responseBlacklist);

				try {
					storeRequest(response);
				} catch (Exception e) {
					Log.warn(e, "Failed to store request");
				}

				try {
					currentReports = executeResponses(response);
				} catch (Exception e) {
					Log.warn(e, "Failed to create response");
				}

				canContinueLoading.countDown(); // early release - notes send in next packets are ignored
			}

			for (IDataSource<ReportCrash> crash : storages.pendingCrashes.listAll())
				crash.delete();

		} catch (FailedToSend e) {
			Log.warn("Failed to send report to %s, cause: %s", url, e.getMessage());
		} catch (Exception e) {
			Log.warn(e, "Failed to send report to %s", url);
		}
	}

	private static void logException(Throwable throwable, String msg, Object... args) {
		storeThrowableForReport(throwable, "openeye");
		Log.warn(throwable, msg, args);
	}

	protected static void loadConfig(InjectedDataStore dataStore) {
		try {
			File configFolder = new File(dataStore.getMcLocation(), "config");
			configFolder.mkdir();
			File configFile = new File(configFolder, "OpenEye.json");

			ConfigProcessing.processConfig(configFile, Config.class, ConfigProcessing.GSON);
		} catch (Exception e) {
			Log.warn(e, "Failed to load config");
		}
	}

	private void tryStoreCrash(ThrowableEntry entry) {
		if (storages != null) {
			try {
				storeCrash(collector, entry, storages);
			} catch (Throwable t) {
				System.err.println("[OpenEye] Failed to store crash report");
				t.printStackTrace();
			}
		} else {
			System.err.println("[OpenEye] Can't store crash report, since storage is not initialized");
		}
	}

	private static void storeCrash(ModMetaCollector collector, ThrowableEntry entry, Storages storages) {
		ReportCrash crashReport = ReportBuilders.buildCrashReport(entry.throwable, entry.location, collector);
		IDataSource<ReportCrash> crashStorage = storages.pendingCrashes.createNew();
		crashStorage.store(crashReport);
	}

	private void updateState() {
		StateHolder.state().installedMods = collector.getAllSignatures();
	}

	private void startDataCollection(final InjectedDataStore dataStore, final ASMDataTable table) {
		Thread senderThread = new Thread() {
			@Override
			public void run() {
				loadConfig(dataStore);

				storages = new Storages(dataStore.getMcLocation());
				StateHolder.init(storages);
				collector = new ModMetaCollector(dataStore, table);

				if (sendReports) sendReports(StateHolder.state());

				updateState();

				canContinueLoading.countDown();
			}
		};

		senderThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				Log.severe(e, "Uncaught exception in data collector thread, report will not be send");
				canContinueLoading.countDown(); // oh well, better luck next time
			}
		});

		senderThread.start();
	}

	private void setupCrashReportDumper() {
		// no logger in here...
		Thread crashDumperThread = new Thread() {
			@Override
			public void run() {
				for (ThrowableEntry t : lethalExceptions)
					tryStoreCrash(t);
			}
		};

		crashDumperThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				System.err.println("[OpenEye] Exception in shutdown handler, report may not be sent");
				e.printStackTrace();
			}
		});

		Runtime.getRuntime().addShutdownHook(crashDumperThread);
	}

	public void start(InjectedDataStore dataStore, ASMDataTable table) {
		startDataCollection(dataStore, table);
		setupCrashReportDumper();
	}

	public void waitForFirstMsg() {
		try {
			canContinueLoading.await();
		} catch (InterruptedException e) {
			Log.warn("Thread interrupted while waiting for msg");
		}
	}

	public Collection<FileSignature> listDangerousFiles() {
		List<FileSignature> result = Lists.newArrayList();

		for (String signature : dangerousSignatures) {
			FileSignature file = collector.getFileForSignature(signature);
			if (signature != null) result.add(file);
		}

		return result;
	}
}
