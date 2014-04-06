package openeye.logic;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import openeye.Log;
import argo.format.JsonFormatter;
import argo.format.PrettyJsonFormatter;
import argo.jdom.JdomParser;
import argo.jdom.JsonRootNode;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;

public class JsonFileOps {
	public static final String REPORT_EXT = ".json";
	public static final String LAST_REPORT_NAME = "eye-last-report" + REPORT_EXT;
	public static final String PERMANENT_STORAGE = "eye-storage" + REPORT_EXT;
	public static final String CRASH_PREFIX = "eye-crash-" + REPORT_EXT;
	public static final DateFormat CRASH_FORMAT = new SimpleDateFormat(CRASH_PREFIX + "yyyy-MM-dd_HH.mm.ss" + REPORT_EXT);

	private static final JsonFormatter JSON_SERIALIZER = new PrettyJsonFormatter();
	private static final JdomParser JDOM_PARSER = new JdomParser();

	private final File mainDir;
	private final File crashesDir;
	private final File pendingDir;

	public JsonFileOps(File mcDir) {
		mainDir = mcDir;
		crashesDir = new File(mcDir, "crash-reports");
		pendingDir = new File(crashesDir, "pending");
	}

	private static void saveJson(File dest, JsonRootNode data) {
		try {
			final Closer closer = Closer.create();
			try {
				OutputStream output = closer.register(new FileOutputStream(dest));
				Writer writer = closer.register(new OutputStreamWriter(output, StandardCharsets.UTF_8));
				JSON_SERIALIZER.format(data, writer);
			} finally {
				closer.close();
			}
		} catch (Throwable t) {
			Log.warn(t, "Failed to save JSON data to file %s", dest);
		}
	}

	private static JsonRootNode loadJson(File src) {
		try {
			final Closer closer = Closer.create();
			try {
				InputStream input = closer.register(new FileInputStream(src));
				Reader reader = closer.register(new InputStreamReader(input, StandardCharsets.UTF_8));
				return JDOM_PARSER.parse(reader);
			} finally {
				closer.close();
			}
		} catch (Throwable t) {
			Log.warn(t, "Failed to save JSON data to file %s", src);
			return null;
		}
	}

	public void addDataToPending(Date date, JsonRootNode data) {
		String fileName = CRASH_FORMAT.format(date);
		File desc = new File(pendingDir, fileName);
		Log.fine("Adding pending crash log to %s", desc);
		saveJson(desc, data);
	}

	private static final FilenameFilter JSON_FILTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			String nameLower = name.toLowerCase();
			return nameLower.startsWith(CRASH_PREFIX) && nameLower.endsWith(REPORT_EXT);
		}
	};

	public List<JsonRootNode> listPendingData() {
		ImmutableList.Builder<JsonRootNode> result = ImmutableList.builder();
		for (File jsonFile : pendingDir.listFiles(JSON_FILTER)) {
			Log.fine("Loading pending crash data from %s", jsonFile);
			JsonRootNode node = loadJson(jsonFile);
			if (node != null) result.add(node);
		}
		return result.build();
	}

	public void archivePending() {
		for (File jsonFile : pendingDir.listFiles(JSON_FILTER)) {
			File newName = new File(crashesDir, jsonFile.getName());
			Log.fine("Moving pending crash file %s to %s", jsonFile, newName);
			jsonFile.renameTo(newName);
		}
	}

	public JsonRootNode loadPermanentData() {
		File storageFile = new File(mainDir, PERMANENT_STORAGE);
		Log.fine("Reading permament storage from %s", storageFile);
		return loadJson(storageFile);
	}

	public void savePermanentStorage(JsonRootNode data) {
		File storageFile = new File(mainDir, PERMANENT_STORAGE);
		Log.fine("Writing permament storage to %s", storageFile);
		saveJson(storageFile, data);
	}
}
