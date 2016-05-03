package openeye.asm;

import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import openeye.Log;
import openeye.logic.ThrowableLogger;

public class CallHack {

	/*
	 * I'm to lazy to do proper signatures in transformer. Obfuscation should make sure this code always uses proper type
	 */
	public static void callFromCrashHandler(Object o) {
		try {
			CrashReport report = (CrashReport)o;
			ThrowableLogger.processThrowable(report.getCrashCause(), "crash_handler");
		} catch (Throwable t) {
			Log.warn(t, "Failed to store crash report %s", o);
		}
	}

	public static void callForSilentException(Throwable throwable, String location) {
		try {
			if (throwable instanceof ReportedException) throwable = tryExtractCause((ReportedException)throwable);
			ThrowableLogger.processThrowable(throwable, location);
		} catch (Throwable t) {
			Log.warn(t, "Failed to store exception %s from %s", throwable, location);
		}
	}

	protected static Throwable tryExtractCause(ReportedException report) {
		try {
			return report.getCrashReport().getCrashCause();
		} catch (Throwable t) {
			Log.warn(t, "Failed to extract report");
			ThrowableLogger.processThrowable(t, "openeye_internal");
			return report;
		}
	}
}
