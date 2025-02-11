package dev.technici4n.moderndynamics.gui.menu;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import dev.technici4n.moderndynamics.init.MdMenusDef;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;

public class EntryFilterMenu extends AbstractContainerMenu {
	public final static HashMap<String, Object> guistate = new HashMap<>();
	public final Level world;
	public final Player entity;
	public int x, y, z;
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private boolean bound = false;
	private Supplier<Boolean> boundItemMatcher = null;
	private Entity boundEntity = null;
	private BlockEntity boundBlockEntity = null;
	private int scrollOffset = 0;
	private static final int MAX_SCROLL_OFFSET = 40 - 4; // 40 entries total, 4 visible per page
	private int selectedEntry = -1; // -1 significa nessuna entry selezionata

	// Add a custom ItemStackHandler for the autonomous slot
	private final ItemStackHandler customSlotHandler = new ItemStackHandler(1);

	// Variabili per memorizzare il testo delle barre
	private String mainBarText = "";
	private String slotBarText = "";

	public EntryFilterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
		super(MdMenusDef.ENTRY_FILTER.get(), containerId);
		this.entity = playerInventory.player;
		this.world = playerInventory.player.level();
		this.internal = new ItemStackHandler(0);
		BlockPos pos = null;
		final int Y = 154;

		if (extraData != null) {
			pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			access = ContainerLevelAccess.create(world, pos);
		}

		this.addSlot(new SlotItemHandler(customSlotHandler, 0, 18, 62) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				// Define what items can be placed in this slot (e.g., allow all items)
				return true;
			}
		});

		// Add player inventory slots
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 20 + col * 18, Y + row * 18));
			}
		}

		// Add hotbar slots
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 20 + col * 18, Y + 58));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.bound) {
			if (this.boundItemMatcher != null)
				return this.boundItemMatcher.get();
			else if (this.boundBlockEntity != null)
				return AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
			else if (this.boundEntity != null)
				return this.boundEntity.isAlive();
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			if (index < 36) {
				if (!this.moveItemStackTo(itemstack1, 36, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(itemstack1, 0, 36, false)) {
				return ItemStack.EMPTY;
			}

			if (itemstack1.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return itemstack;
	}

	public Map<Integer, Slot> get() {
		return customSlots;
	}

	public int getScrollOffset() {
		return scrollOffset;
	}

	public void setScrollOffset(int scrollOffset) {
		this.scrollOffset = Mth.clamp(scrollOffset, 0, MAX_SCROLL_OFFSET);
	}

	public void scrollUp() {
		setScrollOffset(scrollOffset - 1);
	}

	public void scrollDown() {
		setScrollOffset(scrollOffset + 1);
	}

	@Override
	public void removed(Player player) {
		// Handle item return when the GUI is closed
		ItemStack itemStack = customSlotHandler.getStackInSlot(0);
		if (!itemStack.isEmpty()) {
			// Try to add the item to the player's inventory
			if (!player.getInventory().add(itemStack)) {
				// If the inventory is full, drop the item on the ground
				player.drop(itemStack, false);
			}
			customSlotHandler.setStackInSlot(0, ItemStack.EMPTY); // Clear the slot
		}
		super.removed(player);
	}

	public void selectEntry(int entryIndex) {
		if (entryIndex >= 0 && entryIndex < MAX_SCROLL_OFFSET) {
			this.selectedEntry = entryIndex;
			// Notifica eventuali listener o aggiorna lo stato
		}
	}

	public boolean isEntrySelected(int entryIndex) {
		return this.selectedEntry == entryIndex;
	}

	public String getEntry(int entryIndex) {
		// Vettore di stringhe per casi particolari
		String[] specialEntries = {
			"&enchanted",
			"&damaged",
			"&stackable"
		};

		// Controlla se l'entryIndex corrisponde a un caso particolare
		if (entryIndex >= 0 && entryIndex < specialEntries.length) {
			return specialEntries[entryIndex];
		}

		// Ottieni l'oggetto dalla slot custom
		ItemStack stack = getItemInCustomSlot();
		if (stack.isEmpty()) {
			return "";
		}

		// Ottieni la lista di tag come array
		List<TagKey<Item>> tags = stack.getTags().toList();

		// Cicla la lista di tag
		int currentIndex = specialEntries.length;
		for (TagKey<Item> tagKey : tags) {
			if (currentIndex == entryIndex) {
				// Restituisci la tag con il prefisso # (es. "#c:gems")
				return "#" + tagKey.location().toString();
			}
			currentIndex++;
		}

		// Se non ci sono più tag, restituisci il nome dell'oggetto nel formato -namespace:item
		if (currentIndex == entryIndex) {
			ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
			return "-" + itemId.toString(); // Esempio: "-minecraft:diamond"
		}

		// Se non c'è altro, restituisci il modID
		if (currentIndex + 1 == entryIndex) {
			return "@" + getModId();
		}

		// Se superiamo la entryIndex, restituiamo una stringa vuota
		return "";

	}

	public ItemStack getItemInCustomSlot() {
		return this.customSlotHandler.getStackInSlot(0);
	}

	public String getModId() {
		// Ottieni l'oggetto dalla slot custom
		ItemStack stack = getItemInCustomSlot();
		if (stack.isEmpty()) {
			return "";
		}

		// Ottieni il modID dell'oggetto
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return itemId.getNamespace();
	}

	public String getInfo(String entry) {
		if (entry == null || entry.isEmpty()) {
			return "";
		}

		// Controlla il primo carattere della stringa
		switch (entry.charAt(0)) {
			case '#':
				return entry.contains("\\") ? "Specific Tag Equivalence Filter" : "Generic Tag Equivalence Filter";
			case '@':
				return "Mod Equivalence Filter";
			case '-':
				return "Item Filter (Ignore NBT)";
			case '&':
				return "Special Global Filter";
			default:
				return "Unknown Filter Type, report to the developer please.";
		}
	}

	// Metodo per ottenere il testo della barra principale
	public String getMainBarText() {
		return this.mainBarText;
	}

	// Metodo per impostare il testo della barra principale
	public void setMainBarText(String text) {
		this.mainBarText = text;
	}

	// Metodo per ottenere il testo della barra della slot
	public String getSlotBarText() {
		return this.slotBarText;
	}

	public void setSlotBarText(String text) {
		this.slotBarText = text;
	}

	public boolean isNum(String num)
	{
		try {
			Integer.parseInt(num);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	// Metodo per impostare il testo della barra della slot
	public void apply() {
		String text = this.mainBarText;
		String num = this.slotBarText;
		if (text.isEmpty() && this.selectedEntry != -1) {
			// Se la barra è vuota e c'è un'entry selezionata, usa l'entry
			text = this.getEntry(this.selectedEntry);
			if(!num.isEmpty() && isNum(num) && !text.contains("&"))
			{text = text.concat(";".concat(num));}
		}

		// Se il testo è ancora vuoto, non fare nulla
		if (text.isEmpty()) {
			return;
		}


		// Controlla la mano principale
		ItemStack mainHandItem = entity.getMainHandItem();
		ItemStack offHandItem = entity.getOffhandItem();
		if (mainHandItem.getItem().toString().equals("moderndynamics:entry_filter_definition")) {
			mainHandItem.set(DataComponents.CUSTOM_NAME, Component.literal(text));
			return;
		}
		else if (offHandItem.getItem().toString().equals("moderndynamics:entry_filter_definition")) {
			offHandItem.set(DataComponents.CUSTOM_NAME, Component.literal(text));
			return;
		}
		return;
	}
}


