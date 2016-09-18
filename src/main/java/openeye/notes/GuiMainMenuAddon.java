package openeye.notes;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import openeye.logic.Config;

public class GuiMainMenuAddon extends GuiMainMenu {

	private static final int BUTTON_NOTES_ID = 666;
	private static WrappedChatComponent notification;

	@Override
	public void initGui() {
		super.initGui();
		onGuiInit(this, buttonList);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void onGuiInit(GuiScreen screen, List buttonList) {
		final NoteCollector noteCollector = NoteCollector.INSTANCE;
		noteCollector.finishNoteCollection();

		notification = noteCollector.getScreenMsg();

		if (!noteCollector.isEmpty()) {
			NoteCategory type = noteCollector.getMaxLevel();

			NoteIcons icon = type.icon;
			boolean blinking = noteCollector.hasImportantNotes();
			GuiButtonNotes button = getOrCreateInfoButton(screen, buttonList);
			button.setBlinking(blinking);
			button.setIcon(icon);
		}
	}

	protected static GuiButtonNotes getOrCreateInfoButton(GuiScreen screen, List<GuiButton> buttonList) {
		for (GuiButton button : buttonList)
			if (button instanceof GuiButtonNotes) return (GuiButtonNotes)button;

		GuiButtonNotes buttonNotes = new GuiButtonNotes(BUTTON_NOTES_ID, screen.width / 2 + 104, screen.height / 4 + 48 + 24 * 2);
		buttonList.add(buttonNotes);
		return buttonNotes;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicktime) {
		super.drawScreen(mouseX, mouseY, partialTicktime);
		onScreenDraw(this);
	}

	public static void onScreenDraw(GuiScreen screen) {
		if (Config.mainScreenExtraLine && notification != null) screen.drawCenteredString(Minecraft.getMinecraft().fontRenderer, notification.getFormatted(), screen.width / 2, screen.height / 4 + 48 + 24 * 3, 0xFFFFFF);
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == BUTTON_NOTES_ID) onActionPerformed(mc, this);
		else super.actionPerformed(button);
	}

	public static void onActionPerformed(Minecraft mc, GuiScreen screen, GuiButton button) {
		if (button.id == BUTTON_NOTES_ID) onActionPerformed(mc, screen);
	}

	private static void onActionPerformed(Minecraft mc, GuiScreen screen) {
		mc.displayGuiScreen(new GuiNotes(screen, NoteCollector.INSTANCE.getNotes()));
	}

}
