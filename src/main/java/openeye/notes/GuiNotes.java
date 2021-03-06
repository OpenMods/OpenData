package openeye.notes;

import com.google.common.base.Strings;
import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import openeye.Log;
import openeye.notes.entries.NoteEntry;

public class GuiNotes extends GuiScreen {

	private static final int ACTION_GOTO_URL = 0;
	private static final int BUTTON_FINISHED_ID = 0;
	private static final int BUTTON_GOTO_ID = 1;

	private final GuiScreen prevGui;

	private GuiNotesList noteList;

	private int selectedNote = -1;

	private final List<NoteEntry> notes;

	private GuiButton gotoButton;

	private String gotoUrl;

	public GuiNotes(GuiScreen prevGui, List<NoteEntry> notes) {
		this.prevGui = prevGui;
		this.notes = notes;
	}

	@Override
	public void initGui() {
		super.initGui();

		buttonList.add(new GuiButton(BUTTON_FINISHED_ID, width / 2, height - 30, 150, 20, I18n.format("gui.done")));
		gotoButton = new GuiButton(BUTTON_GOTO_ID, width / 2 - 150, height - 30, 150, 20, I18n.format("openeye.notes.goto_page"));
		gotoButton.enabled = false;
		buttonList.add(gotoButton);
		noteList = new GuiNotesList(this, mc, width, height, 10, height - 40, width, height, notes);
	}

	@Override
	public void drawScreen(int par1, int par2, float par3) {
		drawDefaultBackground();
		noteList.drawScreen(par1, par2, par3);
		super.drawScreen(par1, par2, par3);
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == BUTTON_FINISHED_ID) mc.displayGuiScreen(prevGui);
		else if (button.id == BUTTON_GOTO_ID) mc.displayGuiScreen(new GuiConfirmOpenLink(this, gotoUrl, ACTION_GOTO_URL, false));
	}

	private static void openURI(String uri) {
		try {
			URI parsedUri = new URI(uri);
			Desktop.getDesktop().browse(parsedUri);
		} catch (Throwable t) {
			Log.warn(t, "Failed to open URL %s", uri);
		}
	}

	@Override
	public void confirmClicked(boolean result, int action) {
		if (action == ACTION_GOTO_URL && result) openURI(gotoUrl);
		this.mc.displayGuiScreen(this);
	}

	public void selectNote(int slot) {
		selectedNote = slot;
		gotoUrl = getUrl(slot);
		gotoButton.enabled = !Strings.isNullOrEmpty(gotoUrl);
	}

	private String getUrl(int slot) {
		if (slot >= 0 && slot < notes.size()) {
			NoteEntry entry = notes.get(slot);
			return entry.url();
		}

		return null;
	}

	public boolean isNoteSelected(int slot) {
		return selectedNote == slot;
	}
}
