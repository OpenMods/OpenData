package openeye.notes;

import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

public class ScreenNotificationHolder {
	private static class Entry {
		final int level;
		public final IChatComponent msg;

		private Entry(int level, IChatComponent msg) {
			this.level = level;
			this.msg = msg;
		}
	}

	private final ChatStyle REMOVE_FILE_STYLE = new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED);

	private final ChatStyle KNOWN_CRASH_STYLE = new ChatStyle().setColor(EnumChatFormatting.GREEN);

	private final ChatStyle INTRO_STYLE = new ChatStyle().setColor(EnumChatFormatting.GOLD);

	private Entry selectedLine;

	public void addLine(int level, IChatComponent msg) {
		if (selectedLine == null || level > selectedLine.level) selectedLine = new Entry(level, msg);
	}

	public void signalDangerousFile() {
		addLine(64, new ChatComponentTranslation("openeye.main_screen.remove_file").setChatStyle(REMOVE_FILE_STYLE));
	}

	public void signalCrashReported() {
		addLine(8, new ChatComponentTranslation("openeye.main_screen.crash_reported"));
	}

	public void signalKnownCrash() {
		addLine(32, new ChatComponentTranslation("openeye.main_screen.known_crash").setChatStyle(KNOWN_CRASH_STYLE));
	}

	public void signalIntroStuff() {
		addLine(256, new ChatComponentTranslation("openeye.main_screen.intro").setChatStyle(INTRO_STYLE));
	}

	public IChatComponent getSelectedLine() {
		return selectedLine != null? selectedLine.msg : null;
	}
}