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

	@ConfigProperty(category = "data", comment = "Send only basic information about files instead of full analytics. Only works when 'sendModList' is set to true")
	public static boolean scanOnly = false;

	@ConfigProperty(category = "data", comment = "If false, skips sending any mod list - ether basic file information or analytics. OpenEye will respond with information about files unknown to server")
	public static boolean sendModList = true;

	@ConfigProperty(category = "data", comment = "If false, skips sending pending crash reports. Please note that pending crashe reports will not be automatically removed.")
	public static boolean sendCrashes = false;

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

	@ConfigProperty(category = "gui", comment = "X coordinate of notes button")
	public static int notesButtonPosX = 104;

	@ConfigProperty(category = "gui", comment = "Y coordinate of notes button")
	public static int notesButtonPosY = 96;

	@ConfigProperty(category = "gui", comment = "If false, X and Y coordinates of notes button will be measured from screen width/2 and height/4 (standard MC algorithm)")
	public static boolean isNotesButtonPosAbsolute = false;

	@ConfigProperty(category = "gui", comment = "X coordinate of notification line")
	public static int extraLinePosX = 0;

	@ConfigProperty(category = "gui", comment = "Y coordinate of notification line")
	public static int extraLinePosY = 120;

	@ConfigProperty(category = "gui", comment = "If false, X and Y coordinates of notification line will be measured from screen width/2 and height/4 (standard MC algorithm)")
	public static boolean isExtraLinePosAbsolute = false;

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
