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

import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.minecraft.core.registries.Registries;
import dev.technici4n.moderndynamics.gui.menu.EntryFilterMenu;
import dev.technici4n.moderndynamics.util.MdId;



public class MdMenusDef {

    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, MdId.MOD_ID);
	public static final DeferredHolder<MenuType<?>, MenuType<EntryFilterMenu>> ENTRY_FILTER = REGISTRY.register("entry_filter", () -> IMenuTypeExtension.create(EntryFilterMenu::new));



    public static void init() {
        // Initialize menus here
    }
}