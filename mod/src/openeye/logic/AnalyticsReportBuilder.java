package openeye.logic;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import net.minecraftforge.common.ForgeVersion;
import openeye.reports.ReportAnalytics;
import openeye.reports.ReportAnalytics.FmlForgeRuntime;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;

public class AnalyticsReportBuilder {

	public static ReportAnalytics build(ModMetaCollector data) {
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

		return analytics;
	}

}
