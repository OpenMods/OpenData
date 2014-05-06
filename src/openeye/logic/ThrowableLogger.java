package openeye.logic;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import openeye.Log;
import openeye.reports.ReportCrash;
import openeye.storage.IDataSource;

import com.google.common.collect.Queues;

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

	private static void tryStoreCrash(Throwable throwable, String location) {
		try {
			ModMetaCollector collector = resolver != null? resolver.get(5, TimeUnit.SECONDS) : null;
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

		if (resolver != null) tryStoreCrash(throwable, location);
		else delayedThrowables.add(new ThrowableEntry(throwable, location));
	}

	public static void enableResolving(Future<ModMetaCollector> collector) {
		resolver = collector;
		storeAllPending();
	}
}
