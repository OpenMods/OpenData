package openeye.logic;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Queues;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import openeye.Log;
import openeye.protocol.reports.ReportCrash;
import openeye.storage.IDataSource;

public class ThrowableLogger {

	private static class ThrowableEntry {
		public final String location;
		public final Throwable throwable;

		public ThrowableEntry(Throwable throwable, String location) {
			this.location = location;
			this.throwable = throwable;
		}
	}

	private static Future<ModMetaCollector> resolver;

	private static final Queue<ThrowableEntry> delayedThrowables = Queues.newConcurrentLinkedQueue();

	private static final Multiset<String> locationCounters = HashMultiset.create();

	private static void tryStoreCrash(Throwable throwable, String location) {
		ModMetaCollector collector = null;
		try {
			collector = resolver != null? resolver.get(10, TimeUnit.SECONDS) : null;
		} catch (Throwable t) {
			Log.warn(t, "Failed to get resolver");
		}

		try {
			ReportCrash crashReport = ReportBuilders.buildCrashReport(throwable, location, collector);

			Storages storages = Storages.instance();
			IDataSource<ReportCrash> crashStorage = storages.pendingCrashes.createNew();
			crashStorage.store(crashReport);
		} catch (Throwable t) {
			Log.warn(t, "Failed to store crash report");
		}
	}

	private static void storeAllPending() {
		ThrowableEntry e;
		while ((e = delayedThrowables.poll()) != null)
			tryStoreCrash(e.throwable, e.location);
	}

	public static void init() {
		Thread crashDumperThread = new Thread() {
			@Override
			public void run() {
				storeAllPending();
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

	public static void processThrowable(Throwable throwable, String location) {
		if (throwable instanceof INotStoredCrash) return;

		locationCounters.add(location);

		if (locationCounters.count(location) > Config.sentCrashReportsLimit) {
			Log.debug("Limit reached for location %s, skipping %s", location, throwable);
			return;
		}

		if (resolver != null) tryStoreCrash(throwable, location);
		else delayedThrowables.add(new ThrowableEntry(throwable, location));
	}

	public static void enableResolving(Future<ModMetaCollector> collector) {
		resolver = collector;
		storeAllPending();
	}
}
