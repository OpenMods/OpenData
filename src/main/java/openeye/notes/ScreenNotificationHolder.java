package openeye.notes;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

public class ScreenNotificationHolder {
	private static class Entry {
		final int level;
		public final ITextComponent msg;

		private Entry(int level, ITextComponent msg) {
			this.level = level;
			this.msg = msg;
		}
	}

	private final Style REMOVE_FILE_STYLE = new Style().setBold(true).setColor(TextFormatting.RED);

	private final Style KNOWN_CRASH_STYLE = new Style().setColor(TextFormatting.GREEN);

	private final Style INTRO_STYLE = new Style().setColor(TextFormatting.GOLD);

	private Entry selectedLine;

	public void addLine(int level, ITextComponent msg) {
		if (selectedLine == null || level > selectedLine.level) selectedLine = new Entry(level, msg);
	}

	public void signalDangerousFile() {
		addLine(64, new TextComponentTranslation("openeye.main_screen.remove_file").setStyle(REMOVE_FILE_STYLE));
	}

	public void signalCrashReported() {
		addLine(8, new TextComponentTranslation("openeye.main_screen.crash_reported"));
	}

	public void signalKnownCrash() {
		addLine(32, new TextComponentTranslation("openeye.main_screen.known_crash").setStyle(KNOWN_CRASH_STYLE));
	}

	public void signalIntroStuff() {
		addLine(256, new TextComponentTranslation("openeye.main_screen.intro").setStyle(INTRO_STYLE));
	}

	public ITextComponent getSelectedLine() {
		return selectedLine != null? selectedLine.msg : null;
	}
}