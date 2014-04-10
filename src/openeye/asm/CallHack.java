package openeye.asm;

import net.minecraft.crash.CrashReport;
import openeye.logic.MainWorker;

public class CallHack {

	/*
	 * I'm to lazy to do proper signatures in transformer. Obfuscation should make sure this call always has proper type
	 */
	public static void callFromCrashHandler(Object o) {
		CrashReport report = (CrashReport)o;
		MainWorker.storeThrowableForReport(report.getCrashCause());
	}
}
