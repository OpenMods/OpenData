package openeye.notes;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

public class WrappedChatComponent {

	private final IChatComponent wrapped;

	public WrappedChatComponent(IChatComponent wrapped) {
		this.wrapped = wrapped;
	}

	public String getFormatted() {
		return wrapped.getFormattedText();
	}

	public String getUnformatted() {
		return wrapped.getUnformattedText();
	}

	public IChatComponent unwrap() {
		return wrapped;
	}

	public static WrappedChatComponent createText(String text) {
		return new WrappedChatComponent(new ChatComponentText(text));
	}

	public static WrappedChatComponent createTranslation(String format, Object... args) {
		return new WrappedChatComponent(new ChatComponentTranslation(format, args));
	}
}
