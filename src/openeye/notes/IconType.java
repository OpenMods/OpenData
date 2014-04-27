package openeye.notes;

public enum IconType {
	// Keep thresholds in inverse order
	DANGER(60, 64, true),
	ALERT(40, 32, true),
	WARNING(20, 16, false),
	INFO(0, 0, false);

	public final int textureX;
	public final int threshold;
	public final boolean important;

	private IconType(int textureX, int threshold, boolean important) {
		this.textureX = textureX;
		this.threshold = threshold;
		this.important = important;
	}

	public static final IconType[] VALUES = values();
}