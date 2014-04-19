package openeye.logic;

import java.util.Collection;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;
import openeye.reports.FileSignature;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.client.CustomModLoadingErrorDisplayException;

public class DangerousFileClientException extends CustomModLoadingErrorDisplayException implements INotStoredCrash {

	private static final long serialVersionUID = -2964400356592376322L;

	private final List<FileSignature> dangerousFiles;

	public DangerousFileClientException(Collection<FileSignature> dangerousFiles) {
		this.dangerousFiles = ImmutableList.copyOf(dangerousFiles);
	}

	@Override
	public void initGui(GuiErrorScreen errorScreen, FontRenderer fontRenderer) {}

	@Override
	public void drawScreen(GuiErrorScreen errorScreen, FontRenderer fontRenderer, int mouseRelX, int mouseRelY, float tickTime) {
		int offset = Math.max(40 - dangerousFiles.size() * 30, 10);
		final int middle = errorScreen.width / 2;

		errorScreen.drawCenteredString(fontRenderer, "OpenEye has detected files marked as dangerous by authors", middle, offset, 0xFFFFFF);

		for (FileSignature signature : dangerousFiles) {
			offset += 15;
			errorScreen.drawCenteredString(fontRenderer, signature.filename, middle, offset, 0xEEEEEE);
			offset += 10;
			errorScreen.drawCenteredString(fontRenderer, signature.signature, middle, offset, 0xCCCCCC);
		}
		offset += 20;
		errorScreen.drawCenteredString(fontRenderer, "Replace those files with safe one as soon as possible", middle, offset, 0xFFFFFF);
		offset += 10;
		errorScreen.drawCenteredString(fontRenderer, "(or contact OpenMods team, if you think it's false-positive)", middle, offset, 0xCCCCCC);
	}

}
