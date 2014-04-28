package openeye.notes;

public enum NoteIcons {
	// Keep thresholds in inverse order
	INFO(0, 20),
	WARNING(20, 20),
	ERROR(40, 20),
	CRITICAL(60, 20),
	EYE(80, 20),
	OK(100, 20);

	public final int textureU;
	public final int textureV;

	private NoteIcons(int textureU, int textureV) {
		this.textureU = textureU;
		this.textureV = textureV;
	}

	public static final NoteIcons[] VALUES = values();
}