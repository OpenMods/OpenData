package openeye.logic;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.Set;
import openeye.Log;
import openeye.config.ConfigProcessing;
import openeye.config.ConfigProperty;

public class Config {

	@ConfigProperty(category = "debug")
	public static boolean crashOnStartup = false;

	@ConfigProperty(category = "data")
	public static Set<String> tags = ImmutableSet.of();

	@ConfigProperty(category = "data", comment = "Send only information about files and crashes - no analytics")
	public static boolean scanOnly = false;

	@ConfigProperty(category = "data", comment = "Maximum number of crash reports from single category sent per single run")
	public static int sentCrashReportsLimit = 20;

	@ConfigProperty(category = "debug")
	public static boolean pingOnInitialReport = false;

	@ConfigProperty(category = "debug")
	public static boolean dontSend = false;

	@ConfigProperty(category = "debug")
	public static boolean debugSanitizer = false;

	@ConfigProperty(category = "features")
	public static Set<String> reportsBlacklist = ImmutableSet.of();

	@ConfigProperty(category = "features")
	public static Set<String> responseBlacklist = ImmutableSet.of();

	@ConfigProperty(category = "gui", comment = "Enables OpenEye additions to main menu screen")
	public static boolean mainScreenNotes = true;

	@ConfigProperty(category = "gui", comment = "Enables extra line in main menu screen under buttons (if true, only note button will be displayed)")
	public static boolean mainScreenExtraLine = true;

	public static void load(File mcLocation) {
		try {
			File configFolder = new File(mcLocation, "config");
			configFolder.mkdir();
			File configFile = new File(configFolder, "OpenEye.json");

			ConfigProcessing.processConfig(configFile, Config.class, ConfigProcessing.GSON);
		} catch (Exception e) {
			Log.warn(e, "Failed to load config");
		}
	}
}
