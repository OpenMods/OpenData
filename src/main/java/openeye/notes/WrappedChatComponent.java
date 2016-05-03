package openeye.notes;

import net.minecraft.util.ChatMessageComponent;

public class WrappedChatComponent {

	private final ChatMessageComponent wrapped;

	public WrappedChatComponent(ChatMessageComponent wrapped) {
		this.wrapped = wrapped;
	}

	public String getFormatted() {
		return wrapped.toStringWithFormatting(true);
	}

	public String getUnformatted() {
		return wrapped.toString();
	}

	public ChatMessageComponent unwrap() {
		return wrapped;
	}

	public static WrappedChatComponent createText(String text) {
		return new WrappedChatComponent(ChatMessageComponent.createFromText(text));
	}

	public static WrappedChatComponent createTranslation(String format) {
		return new WrappedChatComponent(ChatMessageComponent.createFromTranslationKey(format));
	}

	public static WrappedChatComponent createTranslation(String format, Object... args) {
		return new WrappedChatComponent(ChatMessageComponent.createFromTranslationWithSubstitutions(format, args));
	}
}
