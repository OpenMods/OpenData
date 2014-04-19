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
import openeye.reports.ReportCrash.StackTrace;
import openeye.reports.ReportFileContents.ArchiveDirEntry;
import openeye.reports.ReportFileContents.ArchiveEntry;
import openeye.reports.ReportFileContents.ArchiveFileEntry;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;

public class ReportBuilders {

	public static ReportKnownFiles buildKnownFilesReport(ModMetaCollector data) {
		ReportKnownFiles result = new ReportKnownFiles();

		result.signatures = data.getAllSignatures();

		return result;
	}

	public static ReportAnalytics buildAnalyticsReport(ModMetaCollector data) {
		ReportAnalytics analytics = new ReportAnalytics();

		analytics.branding = FMLCommonHandler.instance().getBrandings();

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

		analytics.signatures = data.getAllSignatures();

		Set<String> tags = Sets.newHashSet();

		Set<String> configTags = Config.tags;
		if (configTags != null) tags.addAll(configTags);

		Set<String> envTags = TagsCollector.collectSystemTags();
		tags.addAll(envTags);

		if (!tags.isEmpty()) analytics.tags = tags;

		return analytics;
	}

	public static ReportCrash buildCrashReport(Throwable throwable, ModMetaCollector collector) {
		ReportCrash crash = new ReportCrash();

		crash.timestamp = new Date().getTime() / 1000; // bleh

		crash.exceptionCls = throwable.getClass().getName();

		crash.message = throwable.getMessage();

		List<StackTrace> trace = Lists.newArrayList();
		for (StackTraceElement e : throwable.getStackTrace()) {
			StackTrace el = new StackTrace();
			final String clsName = e.getClassName();
			el.className = clsName;
			el.fileName = e.getFileName();
			el.methodName = e.getMethodName();
			el.lineNumber = e.getLineNumber();

			if (collector != null) el.signatures = collector.identifyClassSource(clsName);
			trace.add(el);
		}

		crash.stackTrace = trace;

		if (collector != null) crash.states = collector.collectStates();

		return crash;
	}

	public static void fillFileContents(File container, ReportFileContents report) {
		try {
			ZipFile jarFile = new ZipFile(container);

			report.comment = jarFile.getComment();

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

		while (dis.read(buffer) != -1);

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
