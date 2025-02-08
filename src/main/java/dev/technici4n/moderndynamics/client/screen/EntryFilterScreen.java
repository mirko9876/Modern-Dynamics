package dev.technici4n.moderndynamics.client.screen;

import org.checkerframework.checker.units.qual.m;

import dev.technici4n.moderndynamics.gui.menu.EntryFilterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Mth;
import dev.technici4n.moderndynamics.util.MdId;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.gui.components.EditBox;

public class EntryFilterScreen extends AbstractContainerScreen<EntryFilterMenu> {
	private static final ResourceLocation BACKGROUND = ResourceLocation.parse("moderndynamics:textures/gui/entry_filter.png");
	private final Level world;
	private final int x, y, z;
	private final Player entity;

	private static final int ENTRY_WIDTH = 84;
	private static final int ENTRY_HEIGHT = 24;
	private static final int ENTRIES_PER_PAGE = 4;
	private static final int MAX_ENTRIES = 40;
	private static final int ENTRY_Y_OFFSET = 20; 

	private static final ResourceLocation ENTRY_TEXTURE = ResourceLocation.parse("moderndynamics:textures/gui/entry.png");

	// Aggiungi queste costanti per la scrollbar
	protected static final int SCROLLBAR_X = 175;
	protected static final int SCROLLBAR_Y = 30;
	protected static final int SCROLLBAR_WIDTH = 8;
	protected static final int SCROLLBAR_HEIGHT = 34;
	protected static final int HANDLE_HEIGHT = 8;
	protected static final int BUTTON_WIDTH = 8;
	protected static final int BUTTON_HEIGHT = 8;
	protected static final int BUTTON_GAP = 2;
	protected static final int MAX_SCROLL = MAX_ENTRIES - 4;


	protected static final int SCROLLBAR_UV_X = 0;
	protected static final int SCROLLBAR_UV_Y = 0;
	protected static final int HANDLE_UV_X = 8;
	protected static final int HANDLE_UV_Y = 0;
	protected static final int BUTTON_UP_UV_X = 16;
	protected static final int BUTTON_UP_UV_Y = 0;
	protected static final int BUTTON_DOWN_UV_X = 24;
	protected static final int BUTTON_DOWN_UV_Y = 0;

	// UV coordinates for the highlighted textures (below the normal ones)
	protected static final int HIGHLIGHTED_SCROLLBAR_UV_Y = 8;
	protected static final int HIGHLIGHTED_HANDLE_UV_Y = 8;
	protected static final int HIGHLIGHTED_BUTTON_UP_UV_Y = 8;
	protected static final int HIGHLIGHTED_BUTTON_DOWN_UV_Y = 8;

	protected static final ResourceLocation SCROLLBAR_TEXTURE = MdId.of("textures/gui/scrollbar.png");
	protected boolean isScrolling = false;

	private EditBox textBox;

	public EntryFilterScreen(EntryFilterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.world = menu.world;
		this.x = menu.x;
		this.y = menu.y;
		this.z = menu.z;
		this.entity = menu.entity;
		this.imageWidth = 200;
		this.imageHeight = 240;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void init() {
		super.init();

		// Add the text box, centered horizontally
		int textBoxWidth = 100;
		int textBoxX = this.leftPos + (this.imageWidth - textBoxWidth) / 2;
		int textBoxY = this.topPos + 130;
		this.textBox = new EditBox(this.font, textBoxX, textBoxY, textBoxWidth, 10, Component.translatable("gui.moderndynamics.entry_filter.text_box"));
		this.textBox.setMaxLength(100);
		this.addRenderableWidget(this.textBox);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		// Render the background with hardcoded values
		guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, 200, 240, 200, 240);
		guiGraphics.pose().pushPose();

		// Render the entries
		int startIndex = this.menu.getScrollOffset();
		for (int i = 0; i < 4; i++) {
			int entryIndex = startIndex + i;
			if (entryIndex < 40) {
				int entryX = this.leftPos + (this.imageWidth - 104) / 2;
				int entryY = this.topPos + 20 + i * 24;

				// Render the entry texture
				guiGraphics.blit(ENTRY_TEXTURE, entryX, entryY, 0, 0, 104, 24, 104, 24);

				// Render temporary text
				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(entryX + 5, entryY + 5, 0);
				guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
				guiGraphics.drawString(this.font, "Entry iteration: " + entryIndex, 0, 0, 0xFFFFFF, false);
				guiGraphics.pose().popPose();

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(entryX + 5, entryY + 15, 0);
				guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
				guiGraphics.drawString(this.font, "Entry info", 0, 0, 0xFFFFFF, false);
				guiGraphics.pose().popPose();
			}
		}

		// Render scrollbar
		renderScrollbar(guiGraphics, mouseX, mouseY);
	}

	private void renderScrollbar(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		// Check if the mouse is over the scrollbar or buttons
		boolean isMouseOverScrollbar = isInScrollBar(mouseX, mouseY);
		boolean isMouseOverUpButton = isInScrollUpButton(mouseX, mouseY);
		boolean isMouseOverDownButton = isInScrollDownButton(mouseX, mouseY);
		boolean isMouseOverHandle = isInScrollHandle(mouseX, mouseY);

		// Render the scrollbar (use normal texture, no highlighting)
		guiGraphics.blit(SCROLLBAR_TEXTURE,
				this.leftPos + SCROLLBAR_X, this.topPos + SCROLLBAR_Y,
				SCROLLBAR_UV_X, SCROLLBAR_UV_Y,
				8, 34,
				32, 34);

		// Render the "up" button (use highlighted texture if mouse is over)
		guiGraphics.blit(SCROLLBAR_TEXTURE,
				this.leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH + BUTTON_GAP, this.topPos + SCROLLBAR_Y,
				BUTTON_UP_UV_X, isMouseOverUpButton ? HIGHLIGHTED_BUTTON_UP_UV_Y : BUTTON_UP_UV_Y,
				8, 8, 
				32, 34);

		// Render the "down" button (use highlighted texture if mouse is over)
		guiGraphics.blit(SCROLLBAR_TEXTURE,
				this.leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH + BUTTON_GAP, this.topPos + SCROLLBAR_Y + BUTTON_HEIGHT + BUTTON_GAP,
				BUTTON_DOWN_UV_X, isMouseOverDownButton ? HIGHLIGHTED_BUTTON_DOWN_UV_Y : BUTTON_DOWN_UV_Y,
				8, 8,
				32, 34);

		// Render the handle (use highlighted texture if mouse is over)
		float scrollPercent = MAX_SCROLL > 0 ? (float) this.menu.getScrollOffset() / (MAX_SCROLL) : 0;
		int handleY = this.topPos + SCROLLBAR_Y + (int) (scrollPercent * (SCROLLBAR_HEIGHT - HANDLE_HEIGHT));
		guiGraphics.blit(SCROLLBAR_TEXTURE,
				this.leftPos + SCROLLBAR_X, handleY,
				HANDLE_UV_X, isMouseOverHandle ? HIGHLIGHTED_HANDLE_UV_Y : HANDLE_UV_Y,
				8, 8, 
				32, 34);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// Render the title
		int titleWidth = this.font.width(this.title);
		int titleX = (this.imageWidth - titleWidth) / 2;
		guiGraphics.drawString(this.font, this.title, titleX, this.titleLabelY, 0x404040, false);
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isInScrollUpButton(mouseX, mouseY)) {
			this.menu.scrollUp();this.menu.scrollUp();this.menu.scrollUp();this.menu.scrollUp();this.menu.scrollUp();
			return true;
		} else if (isInScrollDownButton(mouseX, mouseY)) {
			this.menu.scrollDown();this.menu.scrollDown();this.menu.scrollDown();this.menu.scrollDown();this.menu.scrollDown();
			return true;
		} else if (isInScrollHandle(mouseX, mouseY)) {
			isScrolling = true;
			return true;
		} else if (isInScrollBar(mouseX, mouseY)) {
			float scrollPercent = Mth.clamp((float) (mouseY - (this.topPos + SCROLLBAR_Y)) / SCROLLBAR_HEIGHT, 0, 1);
			int newScrollOffset = (int) (scrollPercent * MAX_SCROLL);
			this.menu.setScrollOffset(newScrollOffset);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (isScrolling) {
			isScrolling = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (isScrolling) {
			float scrollPercent = Mth.clamp((float) (mouseY - (this.topPos + SCROLLBAR_Y)) / (SCROLLBAR_HEIGHT - HANDLE_HEIGHT), 0, 1);
			int newScrollOffset = (int) (scrollPercent * MAX_SCROLL);
			this.menu.setScrollOffset(newScrollOffset);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount != 0) {
			int currentScroll = this.menu.getScrollOffset();
			int newScroll = currentScroll - (int) Math.signum(verticalAmount);
			this.menu.setScrollOffset(newScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private boolean isInScrollBar(double mouseX, double mouseY) {
		int i = this.leftPos + SCROLLBAR_X;
		int j = this.topPos + SCROLLBAR_Y;
		return mouseX >= i && mouseY >= j && mouseX < i + SCROLLBAR_WIDTH && mouseY < j + SCROLLBAR_HEIGHT;
	}

	private boolean isInScrollUpButton(double mouseX, double mouseY) {
		int i = this.leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH + BUTTON_GAP;
		int j = this.topPos + SCROLLBAR_Y;
		return mouseX >= i && mouseY >= j && mouseX < i + BUTTON_WIDTH && mouseY < j + BUTTON_HEIGHT;
	}

	private boolean isInScrollDownButton(double mouseX, double mouseY) {
		int i = this.leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH + BUTTON_GAP;
		int j = this.topPos + SCROLLBAR_Y + BUTTON_HEIGHT + BUTTON_GAP;
		return mouseX >= i && mouseY >= j && mouseX < i + BUTTON_WIDTH && mouseY < j + BUTTON_HEIGHT;
	}

	private boolean isInScrollHandle(double mouseX, double mouseY) {
		float scrollPercent = MAX_SCROLL > 0 ? (float) this.menu.getScrollOffset() / (MAX_SCROLL) : 0;
		int handleY = this.topPos + SCROLLBAR_Y + (int) (scrollPercent * (SCROLLBAR_HEIGHT - HANDLE_HEIGHT));
		return mouseX >= this.leftPos + SCROLLBAR_X && mouseY >= handleY && mouseX < this.leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH && mouseY < handleY + HANDLE_HEIGHT;
	}
}
