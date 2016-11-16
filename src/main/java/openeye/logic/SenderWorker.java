package openeye.logic;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import openeye.Log;
import openeye.net.ReportSender;
import openeye.notes.NoteCollector;
import openeye.protocol.FileSignature;
import openeye.protocol.ITypedStruct;
import openeye.protocol.reports.ReportCrash;
import openeye.protocol.reports.ReportPing;
import openeye.responses.IExecutableResponse;
import openeye.storage.IDataSource;
import openeye.storage.IQueryableStorage;
import openeye.struct.TypedCollections.ReportsList;
import openeye.struct.TypedCollections.ResponseList;

public class SenderWorker implements Runnable {

	private static final String API_HOST = "openeye.openmods.info";

	private static final String API_PATH = "/api/v1/data";
	//private static final String API_PATH = "/dummy";

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
				Log.debug("Filtered type %s(%s) from list, since it's on blacklist", type, struct);
				it.remove();
			}
		}
	}

	private ReportsList executeResponses(ModMetaCollector collector, ResponseList requests) {
		Preconditions.checkState(!requests.isEmpty());

		final ReportContext context = new ReportContext(collector);

		for (IExecutableResponse request : requests)
			request.execute(context);

		dangerousSignatures.addAll(context.dangerousSignatures());
		return context.reports();
	}

	private static <T> SortedMap<String, T> retrieveAllSources(IQueryableStorage<T> storage) {
		final ImmutableSortedMap.Builder<String, T> result = ImmutableSortedMap.naturalOrder();

		for (IDataSource<T> source : storage.listAll()) {
			try {
				result.put(source.getId(), source.retrieve());
			} catch (Throwable t) {
				Log.warn(t, "Failed to read entry %s, removing", source.getId());
				source.delete();
			}
		}
		return result.build();
	}

	private static <T> void removeSources(IQueryableStorage<T> storage, Set<String> ids) {
		for (String id : ids) {
			final IDataSource<T> entry = storage.getById(id);
			if (entry != null) entry.delete();
		}
	}

	private static Collection<ReportCrash> removePendingCrashDuplicates(Map<String, ReportCrash> crashes) {
		final Map<CrashId, ReportCrash> result = Maps.newHashMap();

		for (Map.Entry<String, ReportCrash> e : crashes.entrySet()) {
			ReportCrash crash = e.getValue();
			if (crash != null) {
				ReportCrash prev = result.put(new CrashId(crash.timestamp, crash.random), crash);
				if (prev != null) Log.warn("Found duplicated crash report %s", e.getKey());
			}
		}
		return ImmutableList.copyOf(crashes.values());
	}

	private static SortedMap<String, ReportCrash> selectCrashes(Map<String, ReportCrash> pendingCrashes) {
		if (!Config.sendCrashes) return ImmutableSortedMap.of();

		if (Config.sentCrashReportsLimitTotal >= 0 && Config.sentCrashReportsLimitTotal < pendingCrashes.size()) {
			final ImmutableSortedMap.Builder<String, ReportCrash> result = ImmutableSortedMap.naturalOrder();
			int count = Config.sentCrashReportsLimitTotal;
			for (Map.Entry<String, ReportCrash> e : pendingCrashes.entrySet()) {
				if (--count < 0) break;
				result.put(e);
			}
			return result.build();
		}

		return ImmutableSortedMap.copyOf(pendingCrashes);
	}

	protected ReportsList createInitialReport(ModMetaCollector collector, Collection<ReportCrash> crashes) {
		final ReportsList result = new ReportsList();

		try {
			if (Config.sendModList) createAnalyticsReport(collector, result);
			if (Config.pingOnInitialReport) result.add(new ReportPing());
			result.addAll(crashes);
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
		} catch (Exception e) {
			logException(e, "Failed to create analytics report");
		}
	}

	private void sendReports(ModMetaCollector collector) {
		final SortedMap<String, ReportCrash> pendingCrashes = retrieveAllSources(Storages.instance().pendingCrashes);
		final SortedMap<String, ReportCrash> selectedPendingCrashes = selectCrashes(pendingCrashes);
		final Collection<ReportCrash> pendingUniqueCrashes = removePendingCrashDuplicates(selectedPendingCrashes);

		ReportsList currentReports = createInitialReport(collector, pendingUniqueCrashes);

		try {
			ReportSender sender = new ReportSender(API_HOST, API_PATH);

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

			removeSources(Storages.instance().pendingCrashes, selectedPendingCrashes.keySet());
			NoteCollector.INSTANCE.addNote(sender.getEncryptionState());
		} catch (Exception e) {
			Log.warn(e, "Failed to send report to " + API_HOST + API_PATH);
		}

	}

	@Override
	public void run() {
		try {
			final ModMetaCollector collector = this.collector.get();
			sendReports(collector);
			// only update state after mods were successfully sent
			StateHolder.state().installedMods = collector.getAllSignatures();
			StateHolder.save();
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
