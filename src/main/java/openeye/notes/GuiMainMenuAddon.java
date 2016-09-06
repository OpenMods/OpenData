package openeye.notes;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatMessageComponent;
import openeye.logic.Config;

public class GuiMainMenuAddon extends GuiMainMenu {

	private static final int BUTTON_NOTES_ID = 666;
	private static ChatMessageComponent notification;

	@Override
	@SuppressWarnings({ "unchecked" })
	public void initGui() {
		super.initGui();
		final NoteCollector noteCollector = NoteCollector.INSTANCE;
		noteCollector.finishNoteCollection();

		notification = noteCollector.getScreenMsg();

		if (!noteCollector.isEmpty()) {
			NoteCategory type = noteCollector.getMaxLevel();

			NoteIcons icon = type.icon;
			boolean blinking = noteCollector.hasImportantNotes();
			GuiButtonNotes button = getOrCreateInfoButton(this, buttonList);
			button.setBlinking(blinking);
			button.setIcon(icon);
		}
	}

	private static int getX(GuiScreen screen, boolean isAbsolute, int delta) {
		return (isAbsolute? 0 : screen.width / 2) + delta;
	}

	private static int getY(GuiScreen screen, boolean isAbsolute, int delta) {
		return (isAbsolute? 0 : screen.height / 4) + delta;
	}

	private static GuiButtonNotes getOrCreateInfoButton(GuiScreen screen, List<GuiButton> buttonList) {
		for (GuiButton button : buttonList)
			if (button instanceof GuiButtonNotes) return (GuiButtonNotes)button;

		GuiButtonNotes buttonNotes = new GuiButtonNotes(BUTTON_NOTES_ID,
				getX(screen, Config.isNotesButtonPosAbsolute, Config.notesButtonPosX),
				getY(screen, Config.isNotesButtonPosAbsolute, Config.notesButtonPosY));
		buttonList.add(buttonNotes);
		return buttonNotes;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicktime) {
		super.drawScreen(mouseX, mouseY, partialTicktime);
		if (Config.mainScreenExtraLine && notification != null)
			drawCenteredString(Minecraft.getMinecraft().fontRenderer,
					notification.toStringWithFormatting(true),
					getX(this, Config.isExtraLinePosAbsolute, Config.extraLinePosX),
					getY(this, Config.isExtraLinePosAbsolute, Config.extraLinePosY),
					0xFFFFFF);
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == BUTTON_NOTES_ID) {
			final GuiNotes newGuiScreen = new GuiNotes(this, NoteCollector.INSTANCE.getNotes());
			mc.displayGuiScreen(newGuiScreen);
		} else super.actionPerformed(button);
	}

}
