package dev.technici4n.moderndynamics.gui.menu;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.world.level.block.entity.BlockEntity;
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

}
