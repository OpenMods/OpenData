package openeye.logic;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import openeye.Log;
import openeye.logic.TypedCollections.ReportsList;
import openeye.logic.TypedCollections.ResponseList;
import openeye.net.GenericSender.FailedToSend;
import openeye.net.ReportSender;
import openeye.reports.IReport;
import openeye.reports.ReportFileInfo;
import openeye.reports.ReportPing;
import openeye.responses.IResponse;
import openeye.storage.IDataSource;
import openeye.storage.Storages;

import com.google.common.base.Preconditions;

import cpw.mods.fml.common.discovery.ASMDataTable;

public final class MainWorker {
	private final String url = "http://37.139.27.100/api/v1/data";

	private ModMetaCollector collector;

	private Storages storages;

	private CountDownLatch initialMsgReceived = new CountDownLatch(1);

	private static Throwable lethalException;

	public static void storeThrowableForReport(Throwable throwable) {
		lethalException = throwable;
	}

	private void initStorage(InjectedDataStore dataStore) {
		storages = new Storages(dataStore.getMcLocation());
	}

	private void storeReport(ReportsList report) {
		IDataSource<ReportsList> list = storages.sentReports.createNew();
		list.store(report);
	}

	private void storeRequest(ResponseList report) {
		IDataSource<ResponseList> list = storages.receivedRequests.createNew();
		list.store(report);
	}

	private void collectData(InjectedDataStore dataStore, ASMDataTable table) {
		collector = new ModMetaCollector(dataStore, table);
	}

	public ReportsList executeResponses(ResponseList requests) {
		Preconditions.checkState(!requests.isEmpty());
		final ReportsList result = new ReportsList();

		final IContext context = new IContext() {
			@Override
			public ReportFileInfo generateFileReport(String signature) {
				return collector.generateFileReport(signature);
			}

			@Override
			public Set<String> getModsForSignature(String signature) {
				return collector.getModsForSignature(signature);
			}

			@Override
			public void queueReport(IReport report) {
				result.add(report);
			}
		};

		for (IResponse request : requests)
			request.execute(context);

		return result;
	}

	private void sendReports(Config config) {
		final ReportsList initialReport = new ReportsList();

		try {
			initialReport.add(AnalyticsReportBuilder.build(config, collector));
			initialReport.add(new ReportPing());
		} catch (Exception e) {
			Log.warn(e, "Failed to create initial report");
			return;
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
				initialMsgReceived.countDown();

				if (response == null || response.isEmpty()) break;

				try {
					storeRequest(response);
				} catch (Exception e) {
					Log.warn(e, "Failed to store report/request");
				}

				try {
					currentReport = executeResponses(response);
				} catch (Exception e) {
					Log.warn(e, "Failed to create response");
				}
			}

		} catch (FailedToSend e) {
			Log.warn("Failed to send report to %s, cause: %s", url, e.getMessage());
		} catch (Exception e) {
			Log.warn(e, "Failed to send report to %s", url);
		}
		initialMsgReceived.countDown();
	}

	protected Config loadConfig() {
		try {
			IDataSource<Config> configSource = storages.config.getById(Storages.CONFIG_ID);
			Config config = configSource.retrieve();
			return config != null? config : new Config();
		} catch (Throwable t) {
			Log.warn(t, "Failed to parse config file");
			return new Config();
		}
	}

	private void storeCrash(Throwable throwable) {

	}

	private void startDataCollection(final InjectedDataStore dataStore, final ASMDataTable table) {
		Thread senderThread = new Thread() {
			@Override
			public void run() {
				initStorage(dataStore);
				collectData(dataStore, table);

				Config config = loadConfig();
				sendReports(config);
			}
		};

		senderThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				Log.warn(e, "Uncaught exception in data collector thread, report will not be send");
			}
		});

		senderThread.run();
	}

	private void setupCrashReportDumper() {
		// no logger in here...
		Thread crashDumperThread = new Thread() {
			@Override
			public void run() {
				if (lethalException != null) {
					if (storages != null) {
						storeCrash(lethalException);
					} else {
						System.err.println("[OpenEye] Can't store crash report, since storage is not initlized");
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
			initialMsgReceived.await();
		} catch (InterruptedException e) {
			Log.warn("Thread interrupted while waiting for msg");
		}
	}
}
