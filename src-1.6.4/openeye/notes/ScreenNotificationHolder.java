package openeye.notes;

import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.EnumChatFormatting;

public class ScreenNotificationHolder {

	private static class Entry {
		final int level;
		public final WrappedChatComponent msg;

		private Entry(int level, ChatMessageComponent msg) {
			this.level = level;
			this.msg = new WrappedChatComponent(msg);
		}
	}

	private Entry selectedLine;

	public void addLine(int level, ChatMessageComponent msg) {
		if (selectedLine == null || level > selectedLine.level) selectedLine = new ScreenNotificationHolder.Entry(level, msg);
	}

	public void signalDangerousFile() {
		addLine(64, ChatMessageComponent.createFromTranslationKey("openeye.main_screen.dangerous_file").setBold(true).setColor(EnumChatFormatting.RED));
	}

	public void signalCrashReported() {
		addLine(8, ChatMessageComponent.createFromTranslationKey("openeye.main_screen.crash_reported"));
	}

	public void signalKnownCrash() {
		addLine(32, ChatMessageComponent.createFromTranslationKey("openeye.main_screen.known_crash").setColor(EnumChatFormatting.GREEN));
	}

	public void signalIntroStuff() {
		addLine(256, ChatMessageComponent.createFromTranslationKey("openeye.main_screen.intro").setColor(EnumChatFormatting.GOLD));
	}

	public WrappedChatComponent getSelectedLine() {
		return selectedLine != null? selectedLine.msg : null;
	}
}