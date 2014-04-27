package openeye.notes;

import net.minecraft.util.EnumChatFormatting;

public enum IconType {
	// Keep thresholds in inverse order
	CRITICAL(60, 64, true, "openeye.level.critical", EnumChatFormatting.RED),
	ALERT(40, 32, true, "openeye.level.alert", EnumChatFormatting.RED),
	WARNING(20, 16, false, "openeye.level.warning", EnumChatFormatting.YELLOW),
	INFO(0, 0, false, "openeye.level.info", EnumChatFormatting.BLUE);

	public final int textureX;
	public final int threshold;
	public final boolean important;
	public final String translated;
	public final EnumChatFormatting color;

	private IconType(int textureX, int threshold, boolean important, String translated, EnumChatFormatting color) {
		this.textureX = textureX;
		this.threshold = threshold;
		this.important = important;
		this.translated = translated;
		this.color = color;
	}

	public static final IconType[] VALUES = values();
}