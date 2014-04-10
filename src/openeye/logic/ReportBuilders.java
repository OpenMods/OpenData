package openeye.logic;

import java.util.*;

import net.minecraftforge.common.ForgeVersion;
import openeye.reports.*;
import openeye.reports.ReportAnalytics.FmlForgeRuntime;
import openeye.reports.ReportCrash.StackTrace;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;

public class ReportBuilders {

	public static ReportAnalytics buildAnalyticsReport(Config config, ModMetaCollector data) {
		ReportAnalytics analytics = new ReportAnalytics();

		analytics.branding = FMLCommonHandler.instance().getBrandings();

		analytics.language = FMLCommonHandler.instance().getCurrentLanguage();

		analytics.locale = Locale.getDefault().toString();

		TimeZone tz = Calendar.getInstance().getTimeZone();
		analytics.timezone = tz.getID();

		analytics.workTime = data.getCollectingDuration();

		FmlForgeRuntime runtime = new FmlForgeRuntime();
		runtime.mcpVersion = Loader.instance().getMCPVersionString();
		runtime.fmlVersion = Loader.instance().getFMLVersionString();
		runtime.forgeVersion = ForgeVersion.getVersion();

		analytics.runtime = runtime;

		analytics.minecraft = Loader.instance().getMCVersionString();

		analytics.signatures = data.getAllSignatures();

		Set<String> tags = Sets.newHashSet();

		Set<String> configTags = config.tags;
		if (configTags != null) tags.addAll(configTags);

		Set<String> envTags = TagsCollector.collectSystemTags();
		tags.addAll(envTags);

		if (!tags.isEmpty()) analytics.tags = tags;

		return analytics;
	}

	public static ReportCrash buildCrashReport(Throwable throwable, ModMetaCollector collector) {
		ReportCrash crash = new ReportCrash();

		crash.timestamp = new Date().getTime();

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

		return crash;
	}

}
