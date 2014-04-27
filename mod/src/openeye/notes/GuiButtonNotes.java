package openeye.notes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class GuiButtonNotes extends GuiButton {

	public static final ResourceLocation TEXTURE = new ResourceLocation("openeye", "textures/gui/buttons.png");

	private final IconType icon;

	private boolean blink;

	private int count;

	public GuiButtonNotes(int id, int x, int y, IconType icon, boolean blink) {
		super(id, x, y, 20, 20, "");
		this.icon = icon;
		this.blink = blink;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY) {
		if (!drawButton) return;

		mc.getTextureManager().bindTexture(TEXTURE);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		final boolean mouseOverButton = isMouseOverButton(mouseX, mouseY);

		int textureY;

		if (blink && ((count++ & 0x10) != 0)) textureY = 40;
		else if (mouseOverButton) textureY = 20;
		else textureY = 0;

		drawTexturedModalRect(xPosition, yPosition, icon.textureX, textureY, width, height);
	}

	protected boolean isMouseOverButton(int mouseX, int mouseY) {
		return mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
	}
}
