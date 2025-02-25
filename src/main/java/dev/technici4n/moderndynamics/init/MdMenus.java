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
package dev.technici4n.moderndynamics.init;

import dev.technici4n.moderndynamics.attachment.attached.FluidAttachedIo;
import dev.technici4n.moderndynamics.attachment.attached.ItemAttachedIo;
import dev.technici4n.moderndynamics.gui.menu.AttachmentMenuType;
import dev.technici4n.moderndynamics.gui.menu.FluidAttachedIoMenu;
import dev.technici4n.moderndynamics.gui.menu.ItemAttachedIoMenu;
import net.minecraft.world.inventory.MenuType;


public class MdMenus {
    public static final MenuType<ItemAttachedIoMenu> ITEM_IO = AttachmentMenuType.create("item_io", ItemAttachedIo::new,
            ItemAttachedIoMenu::new);
    public static final MenuType<FluidAttachedIoMenu> FLUID_IO = AttachmentMenuType.create("fluid_io",
            FluidAttachedIo::new, FluidAttachedIoMenu::new);

    public static void init() {
        // Initialize menus here
    }
}