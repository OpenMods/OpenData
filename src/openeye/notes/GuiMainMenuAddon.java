package openeye.notes;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import openeye.notes.NoteCollector.ScreenNotification;

public class GuiMainMenuAddon extends GuiMainMenu {

	private static final int BUTTON_NOTES_ID = 666;
	private ScreenNotification notification;

	@Override
	@SuppressWarnings("unchecked")
	public void initGui() {
		super.initGui();

		if (!mc.isDemo()) {
			final NoteCollector noteCollector = NoteCollector.INSTANCE;

			notification = noteCollector.getScreenMsg();

			if (!noteCollector.isEmpty()) {
				IconType type = noteCollector.getMaxLevel();

				GuiButton buttonNotes = new GuiButtonNotes(BUTTON_NOTES_ID, width / 2 + 104, height / 4 + 48 + 24 * 2, type, noteCollector.hasImportantNotes());
				this.buttonList.add(buttonNotes);
			}
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicktime) {
		super.drawScreen(mouseX, mouseY, partialTicktime);
		if (notification != null) drawCenteredString(fontRenderer, notification.getDisplay(), width / 2, height / 4 + 48 + 24 * 3, notification.color);
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == BUTTON_NOTES_ID) mc.displayGuiScreen(new GuiNotes(this, NoteCollector.INSTANCE.getNotes()));
		else super.actionPerformed(button);
	}

}
