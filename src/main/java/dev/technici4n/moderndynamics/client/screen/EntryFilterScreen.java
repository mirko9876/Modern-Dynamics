package dev.technici4n.moderndynamics.client.screen;

import org.checkerframework.checker.units.qual.m;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;

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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

public class EntryFilterScreen extends AbstractContainerScreen<EntryFilterMenu> {
	private static final ResourceLocation BACKGROUND = ResourceLocation.parse("moderndynamics:textures/gui/entry_filter.png");
	private final Level world;
	private final int x, y, z;
	private final Player entity;

	private static final int ENTRY_WIDTH = 104;
	private static final int ENTRY_HEIGHT = 24;
	private static final int ENTRIES_PER_PAGE = 4;
	private static final int MAX_ENTRIES = 40;
	private static final int ENTRY_Y_OFFSET = 20; 

	private static final ResourceLocation ENTRY_TEXTURE = ResourceLocation.parse("moderndynamics:textures/gui/entry.png");

	protected static final int SCROLLBAR_X = 52 + ENTRY_WIDTH; // Spostata dopo le entry (104 + 10 pixel di spazio)
	protected static final int SCROLLBAR_Y = 54; // Manteniamo la stessa altezza
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

	private static final ResourceLocation BUTTONS_TEXTURE = ResourceLocation.parse("moderndynamics:textures/gui/buttons.png");

	// Coordinate UV per i pulsanti
	private static final int LEFT_ARROW_UV_X = 56; // Freccia <- spenta (f)
	private static final int LEFT_ARROW_UV_Y = 0;
	private static final int LEFT_ARROW_HIGHLIGHTED_UV_Y = 8; // Freccia <- illuminata (F)

	private static final int RIGHT_ARROW_UV_X = 48; // Freccia -> spenta (d)
	private static final int RIGHT_ARROW_UV_Y = 0;
	private static final int RIGHT_ARROW_HIGHLIGHTED_UV_Y = 8; // Freccia -> illuminata (D)

	// Coordinate UV per il pallino
	private static final int EMPTY_CIRCLE_UV_X = 8; // Pallino vuoto (p)
	private static final int EMPTY_CIRCLE_UV_Y = 0;
	private static final int FILLED_CIRCLE_UV_X = 16; // Pallino pieno (b)
	private static final int FILLED_CIRCLE_UV_Y = 0;

	// Coordinate UV per la spunta
	private static final int CHECKMARK_UV_X = 24; // Spunta spenta (s)
	private static final int CHECKMARK_UV_Y = 0;
	private static final int CHECKMARK_HIGHLIGHTED_UV_Y = 8; // Spunta illuminata (S)

	private int hoveredEntryIndex = -1; // -1 significa nessuna entry è attualmente hoverata
	private int hoveredCheckmark = -1; // -1 significa nessuna spunta è attualmente hoverata

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

		// Aggiorna l'indice dell'entry hoverata
		this.hoveredEntryIndex = getHoveredEntryIndex(mouseX, mouseY);
	}

	private int getHoveredEntryIndex(int mouseX, int mouseY) {
		int startIndex = this.menu.getScrollOffset();
		for (int i = 0; i < 4; i++) {
			int entryIndex = startIndex + i;
			if (entryIndex < 40) {
				int entryX = this.leftPos + (this.imageWidth - 104) / 2;
				int entryY = this.topPos + 20 + i * 24;
				int circleX = entryX + 5;
				int circleY = entryY + 5;

				if (mouseX >= circleX && mouseY >= circleY && mouseX < circleX + 8 && mouseY < circleY + 8) {
					return entryIndex;
				}
			}
		}
		return -1; // Nessuna entry è hoverata
	}

	@Override
	protected void init() {
		super.init();

		// Add the first text box, aligned with the entries and extended in length
		int textBoxWidth = ENTRY_WIDTH - 10; // Aumentato di 10 pixel
		int textBoxX = this.leftPos + (this.imageWidth - 104) / 2; // Aligned with entries
		int textBoxY = this.topPos + 130; // Adjust Y position if needed
		this.textBox = new EditBox(this.font, textBoxX, textBoxY, textBoxWidth, 10, Component.empty());
		this.textBox.setMaxLength(100);
		this.addRenderableWidget(this.textBox);


		// Add a checkmark button 3 pixels to the right of the text box
		int checkmarkX = textBoxX + textBoxWidth + 2; // Spostato di 10 pixel a destra
		int checkmarkY = textBoxY + 1 ; // Allineato con la EditBox
		this.addRenderableWidget(new AbstractWidget(checkmarkX, checkmarkY, 8, 8, Component.empty()) {
			@Override
			public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				boolean isHovered = isMouseOver(mouseX, mouseY);
				guiGraphics.blit(BUTTONS_TEXTURE,
						checkmarkX, checkmarkY,
						CHECKMARK_UV_X, isHovered ? CHECKMARK_HIGHLIGHTED_UV_Y : CHECKMARK_UV_Y,
						8, 8, 64, 64);
			}

			@Override
			public void onClick(double mouseX, double mouseY) {
				// Logica per gestire il clic sulla spunta
				// Esempio: conferma le modifiche o applica il filtro
			}

			@Override
			protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
				defaultButtonNarrationText(narrationElementOutput);
			}
		});

		// Add a second text box near the autonomous slot
		int slotTextBoxWidth = 38; // Extended length (18 + 20 = 38)
		int slotTextBoxX = this.leftPos + 7; // Keep X position at 7
		int slotTextBoxY = this.topPos + 80; // Y position moved up by 1 pixel (81 -> 80)
		EditBox slotTextBox = new EditBox(this.font, slotTextBoxX, slotTextBoxY, slotTextBoxWidth, 10, Component.empty());
		slotTextBox.setMaxLength(10); // Adjust max length if needed
		this.addRenderableWidget(slotTextBox);
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
				guiGraphics.blit(ENTRY_TEXTURE, entryX, entryY, 0, 0, ENTRY_WIDTH, 24, ENTRY_WIDTH, 24);

				// Ottieni la stringa dell'entry
				String entry = this.menu.getEntry(entryIndex);

				// Render the circle button (empty or filled) solo se la stringa non è vuota
				if (!entry.isEmpty()) {
					boolean isSelected = this.menu.isEntrySelected(entryIndex);
					boolean isHovered = this.hoveredEntryIndex == entryIndex;
					int circleX = entryX + 5; // Posizione X del pallino
					int circleY = entryY + 5; // Posizione Y del pallino
					guiGraphics.blit(BUTTONS_TEXTURE,
							circleX, circleY,
							isSelected ? FILLED_CIRCLE_UV_X : EMPTY_CIRCLE_UV_X,
							isHovered ? 8 : 0, // Usa la riga illuminata se hoverata
							8, 8, 64, 64);
				}




				// Render entry text
				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(entryX + 15, entryY + 5, 0);
				guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
				guiGraphics.drawString(this.font, this.menu.getEntry(entryIndex), 0, 0, 0xFFFFFF, false);
				guiGraphics.pose().popPose();

				// Render additional entry info
				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(entryX + 15, entryY + 15, 0);
				guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
				guiGraphics.drawString(this.font, this.menu.getInfo(this.menu.getEntry(entryIndex)), 0, 0, 0xFFFFFF, false);
				guiGraphics.pose().popPose();
			}
		}

		// Render scrollbar
		renderScrollbar(guiGraphics, mouseX, mouseY);

		// Render left and right buttons
		renderButtons(guiGraphics, mouseX, mouseY);
	}

	private void renderScrollbar(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		// Check if the mouse is over the scrollbar or buttons
		boolean isMouseOverScrollbar = isInScrollBar(mouseX, mouseY);
		boolean isMouseOverUpButton = isInScrollUpButton(mouseX, mouseY);
		boolean isMouseOverDownButton = isInScrollDownButton(mouseX, mouseY);
		boolean isMouseOverHandle = isInScrollHandle(mouseX, mouseY);

		// Render the scrollbar
		guiGraphics.blit(SCROLLBAR_TEXTURE,
				this.leftPos + SCROLLBAR_X, this.topPos + SCROLLBAR_Y,
				SCROLLBAR_UV_X, SCROLLBAR_UV_Y,
				SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT,
				32, 34);

		// Render the "up" button above the scrollbar
		guiGraphics.blit(SCROLLBAR_TEXTURE,
				this.leftPos + SCROLLBAR_X, this.topPos + SCROLLBAR_Y - BUTTON_HEIGHT - BUTTON_GAP,
				BUTTON_UP_UV_X, isMouseOverUpButton ? HIGHLIGHTED_BUTTON_UP_UV_Y : BUTTON_UP_UV_Y,
				BUTTON_WIDTH, BUTTON_HEIGHT,
				32, 34);

		// Render the "down" button below the scrollbar
		guiGraphics.blit(SCROLLBAR_TEXTURE,
				this.leftPos + SCROLLBAR_X, this.topPos + SCROLLBAR_Y + SCROLLBAR_HEIGHT + BUTTON_GAP,
				BUTTON_DOWN_UV_X, isMouseOverDownButton ? HIGHLIGHTED_BUTTON_DOWN_UV_Y : BUTTON_DOWN_UV_Y,
				BUTTON_WIDTH, BUTTON_HEIGHT,
				32, 34);

		// Render the handle
		float scrollPercent = MAX_SCROLL > 0 ? (float) this.menu.getScrollOffset() / (MAX_SCROLL) : 0;
		int handleY = this.topPos + SCROLLBAR_Y + (int) (scrollPercent * (SCROLLBAR_HEIGHT - HANDLE_HEIGHT));
		guiGraphics.blit(SCROLLBAR_TEXTURE,
				this.leftPos + SCROLLBAR_X, handleY,
				HANDLE_UV_X, isMouseOverHandle ? HIGHLIGHTED_HANDLE_UV_Y : HANDLE_UV_Y,
				SCROLLBAR_WIDTH, HANDLE_HEIGHT,
				32, 34);
	}

	private void renderButtons(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		// Check if the mouse is over the buttons
		boolean isMouseOverLeftButton = isInLeftButton(mouseX, mouseY);
		boolean isMouseOverRightButton = isInRightButton(mouseX, mouseY);

		// Render left button (freccia <- spenta/illuminata)
		int leftButtonX = this.leftPos + 18 - 10 - 1; // 2 pixel a sinistra della slot, spostato di 1 pixel a sinistra
		int leftButtonY = this.topPos + 61 + 18 - 9; // Allineato con il basso della slot, spostato di 1 pixel in alto (71 -> 70)
		guiGraphics.blit(BUTTONS_TEXTURE,
				leftButtonX, leftButtonY,
				LEFT_ARROW_UV_X, isMouseOverLeftButton ? LEFT_ARROW_HIGHLIGHTED_UV_Y : LEFT_ARROW_UV_Y,
				8, 8, 64, 64);

		// Render right button (freccia -> spenta/illuminata)
		int rightButtonX = this.leftPos + 18 + 18 + 2 - 1; // 2 pixel a destra della slot, spostato di 1 pixel a sinistra
		int rightButtonY = this.topPos + 61 + 18 - 9; // Allineato con il basso della slot, spostato di 1 pixel in alto (71 -> 70)
		guiGraphics.blit(BUTTONS_TEXTURE,
				rightButtonX, rightButtonY,
				RIGHT_ARROW_UV_X, isMouseOverRightButton ? RIGHT_ARROW_HIGHLIGHTED_UV_Y : RIGHT_ARROW_UV_Y,
				8, 8, 64, 64);
	}

	private boolean isInLeftButton(double mouseX, double mouseY) {
		int leftButtonX = this.leftPos + 18 - 10 - 1; // 2 pixel a sinistra della slot, spostato di 1 pixel a sinistra
		int leftButtonY = this.topPos + 61 + 18 - 9; // Allineato con il basso della slot, spostato di 1 pixel in alto (71 -> 70)
		return mouseX >= leftButtonX && mouseY >= leftButtonY && mouseX < leftButtonX + 8 && mouseY < leftButtonY + 8;
	}

	private boolean isInRightButton(double mouseX, double mouseY) {
		int rightButtonX = this.leftPos + 18 + 18 + 2 - 1; // 2 pixel a destra della slot, spostato di 1 pixel a sinistra
		int rightButtonY = this.topPos + 61 + 18 - 9; // Allineato con il basso della slot, spostato di 1 pixel in alto (71 -> 70)
		return mouseX >= rightButtonX && mouseY >= rightButtonY && mouseX < rightButtonX + 8 && mouseY < rightButtonY + 8;
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
		// Check if the user clicked on the checkmark button
		int checkmarkX = this.textBox.getX() + this.textBox.getWidth() + 2;
		int checkmarkY = this.textBox.getY() + 1;
		if (mouseX >= checkmarkX && mouseY >= checkmarkY && mouseX < checkmarkX + 8 && mouseY < checkmarkY + 8) {
			// Applica il testo della barra principale
			this.menu.applyMainBarText(this.minecraft.player);
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		}

		// Check if the user clicked on a circle button
		int startIndex = this.menu.getScrollOffset();
		for (int i = 0; i < 4; i++) {
			int entryIndex = startIndex + i;
			if (entryIndex < 40) {
				int entryX = this.leftPos + (this.imageWidth - 104) / 2;
				int entryY = this.topPos + 20 + i * 24;
				int circleX = entryX + 5;
				int circleY = entryY + 5;

				if (mouseX >= circleX && mouseY >= circleY && mouseX < circleX + 8 && mouseY < circleY + 8) {
					this.menu.selectEntry(entryIndex); // Seleziona l'entry
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					return true;
				}
			}
		}

		if (isInScrollUpButton(mouseX, mouseY)) {
			this.menu.scrollUp();this.menu.scrollUp();this.menu.scrollUp();this.menu.scrollUp();this.menu.scrollUp();
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		} else if (isInScrollDownButton(mouseX, mouseY)) {
			this.menu.scrollDown();this.menu.scrollDown();this.menu.scrollDown();this.menu.scrollDown();this.menu.scrollDown();
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		} else if (isInScrollHandle(mouseX, mouseY)) {
			isScrolling = true;
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		} else if (isInScrollBar(mouseX, mouseY)) {
			float scrollPercent = Mth.clamp((float) (mouseY - (this.topPos + SCROLLBAR_Y)) / SCROLLBAR_HEIGHT, 0, 1);
			int newScrollOffset = (int) (scrollPercent * MAX_SCROLL);
			this.menu.setScrollOffset(newScrollOffset);
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		} else if (isInLeftButton(mouseX, mouseY)) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		} else if (isInRightButton(mouseX, mouseY)) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
		int i = this.leftPos + SCROLLBAR_X;
		int j = this.topPos + SCROLLBAR_Y - BUTTON_HEIGHT - BUTTON_GAP;
		return mouseX >= i && mouseY >= j && mouseX < i + BUTTON_WIDTH && mouseY < j + BUTTON_HEIGHT;
	}

	private boolean isInScrollDownButton(double mouseX, double mouseY) {
		int i = this.leftPos + SCROLLBAR_X;
		int j = this.topPos + SCROLLBAR_Y + SCROLLBAR_HEIGHT + BUTTON_GAP;
		return mouseX >= i && mouseY >= j && mouseX < i + BUTTON_WIDTH && mouseY < j + BUTTON_HEIGHT;
	}

	private boolean isInScrollHandle(double mouseX, double mouseY) {
		float scrollPercent = MAX_SCROLL > 0 ? (float) this.menu.getScrollOffset() / (MAX_SCROLL) : 0;
		int handleY = this.topPos + SCROLLBAR_Y + (int) (scrollPercent * (SCROLLBAR_HEIGHT - HANDLE_HEIGHT));
		return mouseX >= this.leftPos + SCROLLBAR_X && mouseY >= handleY && mouseX < this.leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH && mouseY < handleY + HANDLE_HEIGHT;
	}
}
