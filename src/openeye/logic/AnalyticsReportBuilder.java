package openeye.logic;

import java.util.*;

import net.minecraftforge.common.ForgeVersion;
import openeye.reports.ReportAnalytics;
import openeye.reports.ReportAnalytics.FmlForgeRuntime;

import com.google.common.collect.Sets;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;

public class AnalyticsReportBuilder {

	public static ReportAnalytics build(Config config, ModMetaCollector data) {
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

}
