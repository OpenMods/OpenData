package openeye.notes;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import openeye.Log;
import openeye.notes.entries.NoteEntry;

import com.google.common.base.Strings;

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

	GuiNotes(GuiScreen prevGui, List<NoteEntry> notes) {
		this.prevGui = prevGui;
		this.notes = notes;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initGui() {
		super.initGui();

		buttonList.add(new GuiButton(BUTTON_FINISHED_ID, width / 2, height - 30, 150, 20, StatCollector.translateToLocal("gui.done")));
		gotoButton = new GuiButton(BUTTON_GOTO_ID, width / 2 - 150, height - 30, 150, 20, StatCollector.translateToLocal("openeye.notes.goto_page"));
		gotoButton.enabled = false;
		buttonList.add(gotoButton);
		noteList = new GuiNotesList(this, mc, width, height, 10, height - 40, notes);
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
		else if (button.id == BUTTON_GOTO_ID) {
			displayConfirmation(gotoUrl, ACTION_GOTO_URL);
		}
	}

	protected void displayConfirmation(String url, int actionId) {
		mc.displayGuiScreen(new GuiConfirmOpenLink(this, url, actionId, false));
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
