package openeye.notes;

import java.util.List;

import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import openeye.notes.entries.NoteEntry;

public class GuiNotes extends GuiNotesBase {

	GuiNotes(GuiScreen prevGui, List<NoteEntry> notes) {
		super(prevGui, notes);
	}

	@Override
	protected void displayConfirmation(String url, int actionId) {
		mc.displayGuiScreen(new GuiConfirmOpenLink(this, url, actionId, false));
	}
}
