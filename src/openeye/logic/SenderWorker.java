package openeye.logic;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import openeye.Log;
import openeye.net.GenericSender.FailedToReceive;
import openeye.net.GenericSender.FailedToSend;
import openeye.net.ReportSender;
import openeye.reports.FileSignature;
import openeye.reports.ReportCrash;
import openeye.reports.ReportPing;
import openeye.responses.IResponse;
import openeye.storage.IDataSource;
import openeye.storage.IWorkingStorage;
import openeye.struct.ITypedStruct;
import openeye.struct.TypedCollections.ReportsList;
import openeye.struct.TypedCollections.ResponseList;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SenderWorker implements Runnable {

	private static final String URL = "http://openeye.openmods.info/api/v1/data";

	private final Future<ModMetaCollector> collector;

	private final ModState state;

	private final CountDownLatch firstMessageReceived = new CountDownLatch(1);

	private final Set<String> dangerousSignatures = Sets.newHashSet();

	public SenderWorker(Future<ModMetaCollector> collector, ModState state) {
		this.collector = collector;
		this.state = state;
	}

	private static void logException(Throwable throwable, String msg, Object... args) {
		ThrowableLogger.processThrowable(throwable, "openeye");
		Log.warn(throwable, msg, args);
	}

	private static void store(Object report, String name) {
		try {
			IDataSource<Object> list = Storages.instance().sessionArchive.createNew(name);
			list.store(report);
		} catch (Exception e) {
			Log.warn(e, "Failed to store " + name);
		}
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

	private ReportsList executeResponses(ModMetaCollector collector, ResponseList requests) {
		Preconditions.checkState(!requests.isEmpty());

		final ReportContext context = new ReportContext(collector);

		for (IResponse request : requests)
			request.execute(context);

		dangerousSignatures.addAll(context.dangerousSignatures());
		return context.reports();
	}

	private static Collection<ReportCrash> listPendingCrashes() {
		final IWorkingStorage<ReportCrash> pendingCrashes = Storages.instance().pendingCrashes;

		Map<CrashId, ReportCrash> crashes = Maps.newHashMap();

		for (IDataSource<ReportCrash> crash : pendingCrashes.listAll()) {
			try {
				ReportCrash report = crash.retrieve();
				if (report != null) {
					ReportCrash prev = crashes.put(new CrashId(report.timestamp, report.random), report);
					if (prev != null) Log.warn("Found duplicated crash report %s", crash.getId());
				}
			} catch (Exception e) {
				// no point of sending those to server
				Log.warn(e, "Failed to read crash %s, removing", crash.getId());
				crash.delete();
			}
		}
		return crashes.values();
	}

	private static void removePendingCrashes() {
		final IWorkingStorage<ReportCrash> pendingCrashes = Storages.instance().pendingCrashes;

		for (IDataSource<ReportCrash> crash : pendingCrashes.listAll())
			crash.delete();
	}

	protected ReportsList createInitialReport(ModMetaCollector collector) {
		final ReportsList result = new ReportsList();

		try {
			createAnalyticsReport(collector, result);
			result.addAll(listPendingCrashes());
		} catch (Exception e) {
			logException(e, "Failed to create initial report");
		}

		return result;
	}

	protected void createAnalyticsReport(ModMetaCollector collector, final ReportsList result) {
		try {
			if (Config.scanOnly) {
				result.add(ReportBuilders.buildKnownFilesReport(collector));
			} else {
				result.add(ReportBuilders.buildAnalyticsReport(collector, state.installedMods));
			}

			if (Config.pingOnInitialReport) result.add(new ReportPing());
		} catch (Exception e) {
			logException(e, "Failed to create analytics report");
		}
	}

	private void sendReports(ModMetaCollector collector) {
		ReportsList currentReports = createInitialReport(collector);

		try {
			ReportSender sender = new ReportSender(URL);

			while (!currentReports.isEmpty()) {
				filterStructs(currentReports, Config.reportsBlacklist);
				store(currentReports, "request");

				ResponseList response = Config.dontSend? null : sender.sendAndReceive(currentReports);
				if (response == null || response.isEmpty()) break;

				filterStructs(response, Config.responseBlacklist);
				store(response, "response");

				currentReports.clear();
				try {
					currentReports = executeResponses(collector, response);
				} catch (Exception e) {
					logException(e, "Failed to execute responses");
					break;
				}

				firstMessageReceived.countDown(); // early release - notes send in next packets are ignored
			}

			removePendingCrashes();
		} catch (FailedToSend e) {
			Log.warn(e, "Failed to send report to %s", URL);
		} catch (FailedToReceive e) {
			Log.warn(e, "Failed to receive response from %s", URL);
		} catch (Exception e) {
			Log.warn(e, "Failed to send report to %s", URL);
		}
	}

	@Override
	public void run() {
		try {
			ModMetaCollector collector = this.collector.get();
			sendReports(collector);
		} catch (Throwable t) {
			logException(t, "Failed to send data to server OpenEye");
		} finally {
			firstMessageReceived.countDown(); // can't do much more, releasing lock
		}
	}

	public void start() {
		Thread senderThread = new Thread(this);
		senderThread.setName("OpenEye sender thread");

		senderThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logException(e, "Uncaught exception in data collector thread, report will not be send");
				firstMessageReceived.countDown(); // oh well, better luck next time
			}
		});

		senderThread.start();
	}

	public void waitForFirstMsg() {
		try {
			if (!firstMessageReceived.await(30, TimeUnit.SECONDS)) Log.warn("OpenEye timeouted while waiting for worker thread, data will be incomplete");
		} catch (InterruptedException e) {
			Log.warn("Thread interrupted while waiting for msg");
		}
	}

	public Collection<FileSignature> listDangerousFiles() {
		List<FileSignature> result = Lists.newArrayList();

		try {
			ModMetaCollector collector = this.collector.get();

			for (String signature : dangerousSignatures) {
				FileSignature file = collector.getFileForSignature(signature);
				if (signature != null) result.add(file);
			}
		} catch (Throwable t) {
			Log.warn(t, "Failed to list dangerous files");
		}

		return result;
	}

}
