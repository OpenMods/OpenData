package openeye.logic;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;

import openeye.Log;
import openeye.storage.Storages;
import argo.format.JsonFormatter;
import argo.format.PrettyJsonFormatter;
import argo.jdom.JsonRootNode;
import cpw.mods.fml.common.discovery.ASMDataTable;

public final class ReportSender {
	private static final JsonFormatter JSON_SERIALIZER = new PrettyJsonFormatter();

	private ModMetaCollector collector;

	private Storages storages;

	private static Throwable lethalException;

	public static void storeThrowableForReport(Throwable throwable) {
		lethalException = throwable;
	}

	private void initStorage(InjectedDataStore dataStore) {
		storages = new Storages(dataStore.getMcLocation());
	}

	private void collectData(InjectedDataStore dataStore, ASMDataTable table) {
		collector = new ModMetaCollector(dataStore, table);

		JsonRootNode node = collector.dumpAllFiles();
		try {
			File file = new File("out.txt");
			OutputStream out = new FileOutputStream(file);
			Writer writer = new OutputStreamWriter(out);
			JSON_SERIALIZER.format(node, writer);
			writer.close();
		} catch (Exception e) {}
	}

	private void sendInitialReport() {

	}

	private void storeCrash(Throwable throwable) {

	}

	private void startDataCollection(final InjectedDataStore dataStore, final ASMDataTable table) {
		Thread senderThread = new Thread() {
			@Override
			public void run() {
				initStorage(dataStore);
				collectData(dataStore, table);
				sendInitialReport();
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
