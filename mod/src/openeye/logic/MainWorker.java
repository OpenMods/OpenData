package openeye.logic;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
	private final String url = "http://37.139.27.100/api/v1/data";

	private ModMetaCollector collector;

	private Storages storages;

	private CountDownLatch canContinueLoading = new CountDownLatch(1);

	private static Throwable lethalException;

	private final boolean sendReports;

	private final Set<String> dangerousSignatures = Sets.newHashSet();

	public MainWorker(boolean sendReports) {
		this.sendReports = sendReports;
	}

	public static void storeThrowableForReport(Throwable throwable) {
		if (lethalException == null) lethalException = throwable;
	}

	private void initStorage(InjectedDataStore dataStore) {
		storages = new Storages(dataStore.getMcLocation());
	}

	private void storeReport(ReportsList report) {
		IDataSource<Object> list = storages.sessionArchive.createNew("report");
		list.store(report);
	}

	private void storeRequest(ResponseList report) {
		IDataSource<Object> list = storages.sessionArchive.createNew("request");
		list.store(report);
	}

	private void collectData(InjectedDataStore dataStore, ASMDataTable table) {
		collector = new ModMetaCollector(dataStore, table);
	}

	public ReportsList executeResponses(ResponseList requests) {
		Preconditions.checkState(!requests.isEmpty());
		final ReportsList result = new ReportsList();

		final IContext context = new IContext() {
			private final Set<String> addedFileInfos = Sets.newHashSet();
			private final Set<String> addedFileContents = Sets.newHashSet();

			@Override
			public Set<String> getModsForSignature(String signature) {
				return collector.getModsForSignature(signature);
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
		};

		for (IResponse request : requests)
			request.execute(context);

		return result;
	}

	private void sendReports() {
		final ReportsList initialReport = new ReportsList();

		try {
			if (Config.scanOnly) {
				initialReport.add(ReportBuilders.buildKnownFilesReport(collector));
			} else {
				initialReport.add(ReportBuilders.buildAnalyticsReport(collector));
			}

			if (Config.pingOnInitialReport) initialReport.add(new ReportPing());

		} catch (Exception e) {
			Log.warn(e, "Failed to create initial report");
			return;
		}

		for (IDataSource<ReportCrash> crash : storages.pendingCrashes.listAll()) {
			try {
				initialReport.add(crash.retrieve());
			} catch (Exception e) {
				Log.warn(e, "Failed to add crash report %s", crash.getId());
			}
		}

		try {
			ReportSender sender = new ReportSender(url);

			ReportsList currentReport = initialReport;

			while (!currentReport.isEmpty()) {
				try {
					storeReport(currentReport);
				} catch (Exception e) {
					Log.warn(e, "Failed to store report");
				}

				ResponseList response = sender.sendAndReceive(currentReport);

				if (response == null || response.isEmpty()) break;

				try {
					storeRequest(response);
				} catch (Exception e) {
					Log.warn(e, "Failed to store request");
				}

				try {
					currentReport = executeResponses(response);
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

	private void storeCrash(Throwable throwable) {
		ReportCrash crashReport = ReportBuilders.buildCrashReport(throwable, collector);
		IDataSource<ReportCrash> crashStorage = storages.pendingCrashes.createNew();
		crashStorage.store(crashReport);
	}

	private void startDataCollection(final InjectedDataStore dataStore, final ASMDataTable table) {
		Thread senderThread = new Thread() {
			@Override
			public void run() {
				initStorage(dataStore);
				collectData(dataStore, table);

				loadConfig(dataStore);

				if (sendReports) sendReports();
				canContinueLoading.countDown();
			}
		};

		senderThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				Log.warn(e, "Uncaught exception in data collector thread, report will not be send");
			}
		});

		senderThread.start();
	}

	private void setupCrashReportDumper() {
		// no logger in here...
		Thread crashDumperThread = new Thread() {
			@Override
			public void run() {
				if (lethalException != null && !(lethalException instanceof INotStoredCrash)) {
					if (storages != null) {
						storeCrash(lethalException);
					} else {
						System.err.println("[OpenEye] Can't store crash report, since storage is not initialized");
					}
				}
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
