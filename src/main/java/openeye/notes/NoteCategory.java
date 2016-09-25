package openeye.notes;

import net.minecraft.util.text.TextFormatting;

public enum NoteCategory {
	INFO(NoteIcons.INFO, false, "openeye.category.info", TextFormatting.BLUE),
	REPORTED_CRASH(NoteIcons.OK, false, "openeye.level.reported_crash", TextFormatting.GREEN),
	WARNING(NoteIcons.WARNING, false, "openeye.category.warning", TextFormatting.YELLOW),
	RESOLVED_CRASH(NoteIcons.WARNING, true, "openeye.level.resolved_crash", TextFormatting.GREEN),
	ALERT(NoteIcons.ERROR, false, "openeye.level.category", TextFormatting.RED),
	CRITICAL(NoteIcons.CRITICAL, true, "openeye.category.critical", TextFormatting.RED),
	REMOVE_FILE(NoteIcons.CRITICAL, true, "openeye.category.remove_file", TextFormatting.RED),
	SYSTEM_INFO(NoteIcons.EYE, true, "openeye.category.system_info", TextFormatting.AQUA);

	public final NoteIcons icon;
	public final boolean important;
	public final String translated;
	public final TextFormatting color;

	private NoteCategory(NoteIcons icon, boolean important, String translated, TextFormatting color) {
		this.icon = icon;
		this.important = important;
		this.translated = translated;
		this.color = color;
	}

	public static final NoteCategory[] VALUES = values();

}
