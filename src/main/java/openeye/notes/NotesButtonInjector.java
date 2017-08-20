package openeye.notes;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openeye.logic.Config;

public class NotesButtonInjector {

	private static final int BUTTON_NOTES_ID = 666;
	private static ITextComponent notification;

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

	public static void onScreenDraw(GuiScreen screen) {
		if (Config.mainScreenExtraLine && notification != null)
			screen.drawCenteredString(screen.mc.fontRenderer,
					notification.getFormattedText(),
					getX(screen, Config.isExtraLinePosAbsolute, Config.extraLinePosX),
					getY(screen, Config.isExtraLinePosAbsolute, Config.extraLinePosY),
					0xFFFFFF);
	}

	public static void onActionPerformed(Minecraft mc, GuiScreen screen, GuiButton button) {
		if (button.id == BUTTON_NOTES_ID) onActionPerformed(mc, screen);
	}

	private static void onActionPerformed(Minecraft mc, GuiScreen screen) {
		mc.displayGuiScreen(new GuiNotes(screen, NoteCollector.INSTANCE.getNotes()));
	}

	@SubscribeEvent
	public void onGuiInit(InitGuiEvent evt) {
		if (evt.getGui() instanceof GuiMainMenu) onGuiInit(evt.getGui(), evt.getButtonList());
	}

	@SubscribeEvent
	public void onGuiInit(DrawScreenEvent.Post evt) {
		if (evt.getGui() instanceof GuiMainMenu) onScreenDraw(evt.getGui());
	}

	@SubscribeEvent
	public void onActionPerformed(ActionPerformedEvent evt) {
		if (evt.getGui() instanceof GuiMainMenu) onActionPerformed(evt.getGui().mc, evt.getGui(), evt.getButton());
	}

	public static void registerInjector() {
		MinecraftForge.EVENT_BUS.register(new NotesButtonInjector());
	}
}
