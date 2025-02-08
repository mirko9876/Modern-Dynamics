/*
 * Modern Dynamics
 * Copyright (C) 2021 shartte & Technici4n
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package dev.technici4n.moderndynamics.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import io.netty.buffer.Unpooled;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.BlockPos;

import dev.technici4n.moderndynamics.gui.menu.EntryFilterMenu;



public class EntryFilterDefinitionItem extends Item {
    public EntryFilterDefinitionItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockPos pos = player.blockPosition();
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("gui.moderndynamics.entry_filter.title");
                }

                @Override
                public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                    return false;
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                    return new EntryFilterMenu(containerId, playerInventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
                }
            }, pos);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
