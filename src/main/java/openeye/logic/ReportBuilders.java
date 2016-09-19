package openeye.logic;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import java.io.File;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraftforge.common.ForgeVersion;
import openeye.Log;
import openeye.Proxy;
import openeye.logic.ModMetaCollector.ClassSource;
import openeye.protocol.reports.ReportAnalytics;
import openeye.protocol.reports.ReportCrash;
import openeye.protocol.reports.ReportCrash.ExceptionInfo;
import openeye.protocol.reports.ReportCrash.StackTrace;
import openeye.protocol.reports.ReportEnvironment;
import openeye.protocol.reports.ReportFileContents;
import openeye.protocol.reports.ReportFileContents.ArchiveDirEntry;
import openeye.protocol.reports.ReportFileContents.ArchiveEntry;
import openeye.protocol.reports.ReportFileContents.ArchiveFileEntry;
import openeye.protocol.reports.ReportKnownFiles;

public class ReportBuilders {

	private static final TagsCollector TAGS_COLLECTOR = new TagsCollector();

	private static final Random RANDOM = new Random();

	private static String getSide() {
		return FMLCommonHandler.instance().getSide().toString().toLowerCase(Locale.ENGLISH);
	}

	public static ReportKnownFiles buildKnownFilesReport(ModMetaCollector data) {
		ReportKnownFiles result = new ReportKnownFiles();

		result.signatures = data.getAllFiles();

		return result;
	}

	private static String getJavaVersion() {
		String vendor = Strings.nullToEmpty(System.getProperty("java.vendor"));
		String version = Strings.nullToEmpty(System.getProperty("java.version"));
		return vendor + " " + version;
	}

	private static Map<String, String> getEnvVersions() {
		ImmutableMap.Builder<String, String> versions = ImmutableMap.builder();
		versions.put("mcp", Loader.instance().getMCPVersionString());
		versions.put("fml", Loader.instance().getFMLVersionString());
		versions.put("forge", ForgeVersion.getVersion());
		return versions.build();
	}

	private static void fillEnvInfo(ReportEnvironment report) {
		report.branding = FMLCommonHandler.instance().getBrandings(true);

		report.runtime = getEnvVersions();

		report.minecraft = Loader.instance().getMCVersionString();

		report.javaVersion = getJavaVersion();

		report.side = getSide();

		report.obfuscated = Bootstrap.instance.isRuntimeDeobfuscationEnabled();

		Set<String> tags = TAGS_COLLECTOR.getTags();

		if (!tags.isEmpty()) report.tags = tags;
	}

	public static ReportAnalytics buildAnalyticsReport(ModMetaCollector data, Set<String> prevSignatures) {
		ReportAnalytics analytics = new ReportAnalytics();

		fillEnvInfo(analytics);

		String language = Proxy.instance().getLanguage();
		analytics.language = Strings.isNullOrEmpty(language)? "invalid" : language;

		analytics.locale = Locale.getDefault().toString();

		TimeZone tz = Calendar.getInstance().getTimeZone();
		analytics.timezone = tz.getID();

		analytics.workTime = data.getCollectingDuration() / 1000.0f;

		analytics.signatures = data.getAllFiles();

		Set<String> currentSignatures = data.getAllSignatures();

		analytics.installedSignatures = Sets.difference(currentSignatures, prevSignatures);

		analytics.uninstalledSignatures = Sets.difference(prevSignatures, currentSignatures);

		return analytics;
	}

	private static StackTrace createStackTraceElement(StackTraceElement e, ModMetaCollector collector) {
		StackTrace el = new StackTrace();
		final String clsName = e.getClassName();
		el.className = clsName;
		el.fileName = e.getFileName();
		el.methodName = e.getMethodName();
		el.lineNumber = e.getLineNumber();

		if (collector != null) {
			ClassSource source = collector.identifyClassSource(clsName);
			if (source != null) {
				el.signatures = source.containingClasses;
				if (source.loadedFrom != null) {
					el.source = source.loadedFrom;
					el.signatures.add(source.loadedFrom);
				}
			}
		}
		return el;
	}

	private static ExceptionInfo createStackTrace(Throwable throwable, StackTraceElement[] prevStacktrace, Set<Throwable> alreadySerialized, ModMetaCollector collector) {
		if (alreadySerialized.contains(throwable)) return null; // cyclical reference

		ExceptionInfo info = new ExceptionInfo();

		info.exceptionCls = throwable.getClass().getName();
		info.message = Sanitizers.getSanitizerForThrowable(throwable.getClass()).sanitize(throwable.getMessage());

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

		fillEnvInfo(crash);

		crash.timestamp = new Date().getTime();

		crash.location = location;

		Set<Throwable> blacklist = Sets.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
		crash.exception = createStackTrace(throwable, new StackTraceElement[0], blacklist, collector);

		if (collector != null) crash.states = collector.collectStates();

		crash.random = RANDOM.nextInt();

		crash.resolved = collector != null;

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
