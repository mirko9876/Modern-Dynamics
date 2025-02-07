package dev.technici4n.moderndynamics.gui.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class EntryFilterMenu extends AbstractContainerMenu {
    public EntryFilterMenu(int containerId, Inventory playerInventory) {
        super(MdMenus.ENTRY_FILTER_MENU, containerId);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
} 