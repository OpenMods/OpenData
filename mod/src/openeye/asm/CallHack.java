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
		CrashReport report = (CrashReport)o;
		ThrowableLogger.processThrowable(report.getCrashCause(), "crash_handler");
	}

	public static void callForSilentException(Throwable throwable, String location) {
		if (throwable instanceof ReportedException) {
			try {
				ReportedException tmp = (ReportedException)throwable;
				throwable = tmp.getCrashReport().getCrashCause();
			} catch (Throwable t) {
				Log.warn(t, "Failed to extract report");
				ThrowableLogger.processThrowable(t, "openeye_internal");
			}
		}

		ThrowableLogger.processThrowable(throwable, location);
	}
}
