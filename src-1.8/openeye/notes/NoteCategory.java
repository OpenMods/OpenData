package openeye.notes;

import net.minecraft.util.EnumChatFormatting;

public enum NoteCategory {
	INFO(NoteIcons.INFO, false, "openeye.category.info", EnumChatFormatting.BLUE),
	REPORTED_CRASH(NoteIcons.OK, false, "openeye.level.reported_crash", EnumChatFormatting.GREEN),
	WARNING(NoteIcons.WARNING, false, "openeye.category.warning", EnumChatFormatting.YELLOW),
	RESOLVED_CRASH(NoteIcons.WARNING, true, "openeye.level.resolved_crash", EnumChatFormatting.GREEN),
	ALERT(NoteIcons.ERROR, false, "openeye.level.category", EnumChatFormatting.RED),
	CRITICAL(NoteIcons.CRITICAL, true, "openeye.category.critical", EnumChatFormatting.RED),
	REMOVE_FILE(NoteIcons.CRITICAL, true, "openeye.category.remove_file", EnumChatFormatting.RED),
	SYSTEM_INFO(NoteIcons.EYE, true, "openeye.category.system_info", EnumChatFormatting.AQUA);

	public final NoteIcons icon;
	public final boolean important;
	public final String translated;
	public final EnumChatFormatting color;

	private NoteCategory(NoteIcons icon, boolean important, String translated, EnumChatFormatting color) {
		this.icon = icon;
		this.important = important;
		this.translated = translated;
		this.color = color;
	}

	public static final NoteCategory[] VALUES = values();

}
