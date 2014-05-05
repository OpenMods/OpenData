package openeye.logic;

import java.io.File;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraftforge.common.ForgeVersion;
import openeye.Log;
import openeye.reports.*;
import openeye.reports.ReportAnalytics.FmlForgeRuntime;
import openeye.reports.ReportCrash.ExceptionInfo;
import openeye.reports.ReportCrash.StackTrace;
import openeye.reports.ReportFileContents.ArchiveDirEntry;
import openeye.reports.ReportFileContents.ArchiveEntry;
import openeye.reports.ReportFileContents.ArchiveFileEntry;
import openeye.utils.CompatiblityAdapter;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;

public class ReportBuilders {

	private static final TagsCollector tagsCollector = new TagsCollector();

	public static ReportKnownFiles buildKnownFilesReport(ModMetaCollector data) {
		ReportKnownFiles result = new ReportKnownFiles();

		result.signatures = data.getAllFiles();

		return result;
	}

	public static String getJavaVersion() {
		String vendor = Strings.nullToEmpty(System.getProperty("java.vendor"));
		String version = Strings.nullToEmpty(System.getProperty("java.version"));
		return vendor + " " + version;
	}

	public static ReportAnalytics buildAnalyticsReport(ModMetaCollector data, Set<String> prevSignatures) {
		ReportAnalytics analytics = new ReportAnalytics();

		analytics.branding = CompatiblityAdapter.getBrandings();

		analytics.language = FMLCommonHandler.instance().getCurrentLanguage();

		analytics.locale = Locale.getDefault().toString();

		TimeZone tz = Calendar.getInstance().getTimeZone();
		analytics.timezone = tz.getID();

		analytics.workTime = data.getCollectingDuration() / 1000.0f;

		FmlForgeRuntime runtime = new FmlForgeRuntime();
		runtime.mcpVersion = Loader.instance().getMCPVersionString();
		runtime.fmlVersion = Loader.instance().getFMLVersionString();
		runtime.forgeVersion = ForgeVersion.getVersion();

		analytics.runtime = runtime;

		analytics.minecraft = Loader.instance().getMCVersionString();

		analytics.javaVersion = getJavaVersion();

		analytics.signatures = data.getAllFiles();

		Set<String> tags = tagsCollector.getTags();

		if (!tags.isEmpty()) analytics.tags = tags;

		Set<String> currentSignatures = data.getAllSignatures();

		analytics.addedSignatures = Sets.difference(currentSignatures, prevSignatures);

		analytics.removedSignatures = Sets.difference(prevSignatures, currentSignatures);

		return analytics;
	}

	private static StackTrace createStackTraceElement(StackTraceElement e, ModMetaCollector collector) {
		StackTrace el = new StackTrace();
		final String clsName = e.getClassName();
		el.className = clsName;
		el.fileName = e.getFileName();
		el.methodName = e.getMethodName();
		el.lineNumber = e.getLineNumber();

		if (collector != null) el.signatures = collector.identifyClassSource(clsName);
		return el;
	}

	private static ExceptionInfo createStackTrace(Throwable throwable, StackTraceElement[] prevStacktrace, Set<Throwable> alreadySerialized, ModMetaCollector collector) {
		if (alreadySerialized.contains(throwable)) return null; // cyclical reference

		ExceptionInfo info = new ExceptionInfo();

		info.exceptionCls = throwable.getClass().getName();
		info.message = Sanitizer.sanitize(throwable.getMessage());

		alreadySerialized.add(throwable);

		info.stackTrace = Lists.newArrayList();

		StackTraceElement[] stackTrace = throwable.getStackTrace();

		int m = stackTrace.length - 1;
		int n = prevStacktrace.length - 1;
		while (m >= 0 && n >= 0 && stackTrace[m].equals(prevStacktrace[n])) {
			m--;
			n--;
		}

		for (int i = 0; i <= m; i++)
			info.stackTrace.add(createStackTraceElement(stackTrace[i], collector));

		Throwable cause = throwable.getCause();
		if (cause != null) info.cause = createStackTrace(cause, stackTrace, alreadySerialized, collector);

		return info;
	}

	public static ReportCrash buildCrashReport(Throwable throwable, String location, ModMetaCollector collector) {
		ReportCrash crash = new ReportCrash();

		crash.timestamp = new Date().getTime();

		crash.location = location;

		Set<Throwable> blacklist = Sets.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
		crash.exception = createStackTrace(throwable, new StackTraceElement[0], blacklist, collector);

		if (collector != null) crash.states = collector.collectStates();

		crash.tags = tagsCollector.getTags();

		crash.javaVersion = getJavaVersion();

		return crash;
	}

	public static void fillFileContents(File container, ReportFileContents report) {
		try {
			ZipFile jarFile = new ZipFile(container);

			List<ArchiveDirEntry> dirs = Lists.newArrayList();
			List<ArchiveFileEntry> files = Lists.newArrayList();

			try {
				Enumeration<? extends ZipEntry> entries = jarFile.entries();

				while (entries.hasMoreElements()) {
					ZipEntry zipEntry = entries.nextElement();

					if (zipEntry.isDirectory()) {
						ArchiveDirEntry resultEntry = new ArchiveDirEntry();
						fillCommonFields(zipEntry, resultEntry);
						dirs.add(resultEntry);
					} else {
						ArchiveFileEntry resultEntry = new ArchiveFileEntry();
						fillCommonFields(zipEntry, resultEntry);
						resultEntry.size = zipEntry.getSize();
						resultEntry.crc = Long.toHexString(zipEntry.getCrc());
						resultEntry.signature = createSignature(jarFile.getInputStream(zipEntry));
						files.add(resultEntry);
					}
				}
			} finally {
				jarFile.close();
			}

			report.dirs = dirs;
			report.files = files;
		} catch (Exception e) {
			Log.warn(e, "Failed to get contents of file %s", container);
		}
	}

	private static void fillCommonFields(ZipEntry zipEntry, ArchiveEntry resultEntry) {
		resultEntry.filename = zipEntry.getName();
		resultEntry.timestamp = zipEntry.getTime();
		resultEntry.comment = zipEntry.getComment();
	}

	private static String createSignature(InputStream is) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		DigestInputStream dis = new DigestInputStream(is, md);

		byte[] buffer = new byte[1024];

		while (dis.read(buffer) != -1) {}

		dis.close();

		byte[] digest = md.digest();
		return "sha256:" + bytesToHex(digest);
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(2 * bytes.length);
		for (byte b : bytes)
			sb.append(HEX[(b >> 4) & 0xf]).append(HEX[b & 0xf]);
		return sb.toString();
	}

	private static final char[] HEX = "0123456789abcdef".toCharArray();
}
