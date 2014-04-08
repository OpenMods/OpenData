package openeye.logic;

import java.lang.Thread.UncaughtExceptionHandler;

import openeye.Log;
import openeye.net.ReportSender;
import openeye.reports.ReportFileInfo;
import openeye.reports.ReportsList;
import openeye.requests.RequestsList;
import openeye.storage.IDataSource;
import openeye.storage.Storages;
import cpw.mods.fml.common.discovery.ASMDataTable;

public final class MainWorker {
	private final String url = "http://37.139.27.100/api/v1/data";

	private ModMetaCollector collector;

	private Storages storages;

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

	private void storeRequest(RequestsList report) {
		IDataSource<RequestsList> list = storages.receivedRequests.createNew();
		list.store(report);
	}

	private void collectData(InjectedDataStore dataStore, ASMDataTable table) {
		collector = new ModMetaCollector(dataStore, table);
	}

	private void sendReports() {
		final ReportsList initialReport = new ReportsList();

		try {
			initialReport.append(AnalyticsReportBuilder.build(collector));
		} catch (Exception e) {
			Log.warn(e, "Failed to create initial report");
			return;
		}

		final IContext context = new IContext() {
			@Override
			public ReportFileInfo generateFileReport(String signature) {
				return collector.generateFileReport(signature);
			}
		};

		try {
			ReportSender sender = new ReportSender(url);

			ReportsList currentReport = initialReport;

			while (true) {
				try {
					storeReport(currentReport);
				} catch (Exception e) {
					Log.warn(e, "Failed to store report");
				}

				RequestsList response = sender.sendAndReceive(currentReport);

				try {
					storeRequest(response);
				} catch (Exception e) {
					Log.warn(e, "Failed to store report/request");
				}

				if (response == null || response.isEmpty()) break;

				try {
					currentReport = response.generateResponse(context);
				} catch (Exception e) {
					Log.warn(e, "Failed to create response");
				}
			}

		} catch (Exception e) {
			Log.warn(e, "Failed to send report to %s", url);
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
				sendReports();
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
}
