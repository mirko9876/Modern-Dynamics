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
package dev.technici4n.moderndynamics.attachment.attached;

import dev.technici4n.moderndynamics.attachment.settings.FilterDamageMode;
import dev.technici4n.moderndynamics.attachment.settings.FilterInversionMode;
import dev.technici4n.moderndynamics.attachment.settings.FilterModMode;
import dev.technici4n.moderndynamics.attachment.settings.FilterNbtMode;
import dev.technici4n.moderndynamics.item.EntryFilterDefinitionItem;
import dev.technici4n.moderndynamics.util.FluidVariant;
import dev.technici4n.moderndynamics.util.ItemVariant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import dev.technici4n.moderndynamics.ModernDynamics;

public final class ItemCachedFilter {
    private final Set<ItemVariant> listedVariants;
    private final Set<Item> listedItems;
    private final FilterInversionMode filterInversion;
    private final FilterDamageMode filterDamage;
    private final FilterNbtMode filterNbt;
    private final FilterModMode filterMod;

    /**
     * Lists mod IDs in case mod-id based filtering is enabled.
     * This supersedes/replaces filtering by explicit items or fluids.
     * It is always null otherwise.
     */
    @Nullable
    private Set<String> listedMods;

    private final Map<ItemVariant, Integer> sentQuantities = new HashMap<>();
    private final Map<ItemVariant, Integer> receivedQuantities = new HashMap<>();
    private final Map<ItemVariant, Boolean> map_reset = new HashMap<>();
    private final Map<ItemVariant, Long> ttlMap = new HashMap<>();
    private long ttlDuration = 15000; // TTL duration in milliseconds, default 15 seconds

    public ItemCachedFilter(List<ItemVariant> filterConfig,
            FilterInversionMode filterInversion,
            FilterDamageMode filterDamage,
            FilterNbtMode filterNbt,
            FilterModMode filterMod) {
        this.filterInversion = filterInversion;
        this.filterDamage = filterDamage;
        this.filterNbt = filterNbt;
        this.filterMod = filterMod;

        // Dedupe and drop blanks
        this.listedVariants = new HashSet<>(filterConfig.size());
        this.listedItems = Collections.newSetFromMap(new IdentityHashMap<>());
        for (var variant : filterConfig) {
            if (!variant.isBlank()) {
                this.listedVariants.add(variant);
                this.listedItems.add(variant.getItem());
            }
        }
    }

    public boolean matchesItem(ItemVariant variant, IItemHandler chest, int maxExtracted, String attachmentType) {
        
        confirmReceipt(variant, chest);

        // Use pure variant for filtering
        boolean isListed = isItemListed(variant, chest, maxExtracted, attachmentType);

        // Apply inversion mode (whitelist/blacklist)
        boolean result = isListed == (filterInversion == FilterInversionMode.WHITELIST);

        // Confirm receipt if the item is listed and matches
        return result;
    }

    private boolean isItemListed(ItemVariant variant, IItemHandler chest, int maxAmount, String attachmentType) {
        boolean itemIsListed = false;
        int requiredQuantity = 0;

        if(sentQuantities.getOrDefault(variant, 0) < 1)
        {updateReceivedQuantities(variant, chest);}

        for (ItemVariant filterVariant : listedVariants) {
            if (filterVariant.getItem() instanceof EntryFilterDefinitionItem) {
                String displayName = filterVariant.toStack().getHoverName().getString()
                        .trim()
                        .replace("'", "")
                        .replace("\"", "");

                // Macro filter: &enchanted
                if (displayName.equals("&enchanted")) {
                    if (variant.toStack().isEnchanted()) {
                        itemIsListed = true;
                        break;
                    }
                    continue;
                }
                // Macro filter: &damaged
                else if (displayName.equals("&damaged")) {
                    if (variant.toStack().isDamaged()) {
                        itemIsListed = true;
                        break;
                    }
                    continue;
                }
                // Macro filter: &stackable
                else if (displayName.equals("&stackable")) {
                    if (variant.toStack().isStackable()) {
                        itemIsListed = true;
                        break;
                    }
                    continue;
                }
                // Check for tag filter (#tag;number)
                else if (displayName.startsWith("#")) {
                    String[] parts = displayName.split(";");
                    String tagName = parts[0].substring(1);
                    var tagLocation = ResourceLocation.tryParse(tagName);
                    if (tagLocation != null) {
                        var tagKey = TagKey.create(Registries.ITEM, tagLocation);
                        var registry = BuiltInRegistries.ITEM;

                        for (var holder : registry.getTagOrEmpty(tagKey)) {
                            if (holder.value() == variant.getItem()) {
                                itemIsListed = true;
                                if (attachmentType.equals("ATTRACTOR") && parts.length == 2 && !parts[1].trim().isEmpty()) {
                                    requiredQuantity = Integer.parseInt(parts[1].trim());
                                    int extracted = sentQuantities.getOrDefault(variant, 0);
                                    if (extracted >= requiredQuantity) {
                                        itemIsListed = false;
                                        if (!ttlMap.containsKey(variant)) {
                                            ttlMap.put(variant, System.currentTimeMillis() + ttlDuration);
                                        }
                                    } else if (map_reset.getOrDefault(variant, false) == false) {
                                        confirmReceipt(variant, chest);
                                        sentQuantities.put(variant, extracted + maxAmount);
                                    }
                                }else {
                                    itemIsListed = true;
                                }
                                break;
                            }
                        }
                    }
                }
                // Check for mod ID filter (@modid;number)
                else if (displayName.startsWith("@")) {
                    String[] parts = displayName.split(";");
                    String modId = parts[0].substring(1);
                    if (modId.equals(getModId(variant))) {
                        itemIsListed = true;
                        if (attachmentType.equals("ATTRACTOR") && parts.length == 2 && !parts[1].trim().isEmpty()) {
                            requiredQuantity = Integer.parseInt(parts[1].trim());
                            int extracted = sentQuantities.getOrDefault(variant, 0);
                            if (extracted >= requiredQuantity) {
                                itemIsListed = false;
                                if (!ttlMap.containsKey(variant)) {
                                    ttlMap.put(variant, System.currentTimeMillis() + ttlDuration);
                                }
                            } else if (map_reset.getOrDefault(variant, false) == false) {
                                confirmReceipt(variant, chest);
                                sentQuantities.put(variant, extracted + maxAmount);
                            }
                        } else {
                            itemIsListed = true;
                        }
                    }
                }
                // Check for item filter (-item;number)
                else if (displayName.startsWith("-")) {
                    String[] parts = displayName.split(";");
                    String itemName = parts[0].substring(1);
                    if (itemName.equals(variant.getItem().toString())) {
                        itemIsListed = true;
                        if (attachmentType.equals("ATTRACTOR") && parts.length == 2 && !parts[1].trim().isEmpty()) {
                            requiredQuantity = Integer.parseInt(parts[1].trim());
                            int extracted = sentQuantities.getOrDefault(variant, 0);
                            if (extracted >= requiredQuantity) {
                                itemIsListed = false;
                                if (!ttlMap.containsKey(variant)) {
                                    ttlMap.put(variant, System.currentTimeMillis() + ttlDuration);
                                }
                            } else if (map_reset.getOrDefault(variant, false) == false) {
                                confirmReceipt(variant, chest);
                                sentQuantities.put(variant, extracted + maxAmount);
                            }
                        }else {
                            itemIsListed = true;
                        }
                    }
                }
            } else {
                // Normal item matching
                if (filterNbt == FilterNbtMode.RESPECT_NBT) {
                    itemIsListed = listedVariants.contains(variant);
                } else {
                    itemIsListed = listedItems.contains(variant.getItem());
                }
            }
        }

        // Check TTL before returning the result
        Long ttl = ttlMap.get(variant);
        if (ttl != null && System.currentTimeMillis() > ttl) {
            ttlMap.remove(variant);
            confirmReceipt(variant, chest);
            if(receivedQuantities.getOrDefault(variant, 0) < requiredQuantity && receivedQuantities.getOrDefault(variant, 0) > 0)
            {
                receivedQuantities.remove(variant, 0);
                sentQuantities.remove(variant, 0);
                itemIsListed = true;
            }
            map_reset.remove(variant);
        }

        confirmReceipt(variant, chest);
        return itemIsListed;
    }

    private int getItemQuantityInChest(ItemVariant variant, IItemHandler chest) {
        int total = 0;
        for (int i = 0; i < chest.getSlots(); i++) {
            ItemStack stack = chest.getStackInSlot(i);
            String item0 = stack.getItem().toString();
            String item1 = variant.toStack().getItem().toString();
            if (item0.equals(item1)) {
                total += stack.getCount();
            }
        }
        //ModernDynamics.LOGGER.info("Item quantity in chest for {}: {}", variant, total); // Log per debug
        return total;
    }

    private Set<String> getListedMods() {
        if (listedMods == null) {
            listedMods = new HashSet<>();
            for (var variant : listedVariants) {
                listedMods.add(getModId(variant));
            }
        }

        return listedMods;
    }

    public boolean matchesFluid(FluidVariant variant) {
        return false;
    }

    private static String getModId(ItemVariant variant) {
        // This returns "minecraft" if the item is unregistered
        return BuiltInRegistries.ITEM.getKey(variant.getItem()).getNamespace();
    }

    private static String getModId(FluidVariant variant) {
        // This returns "minecraft" if the item is unregistered
        return BuiltInRegistries.FLUID.getKey(variant.getFluid()).getNamespace();
    }

    @FunctionalInterface
    interface NbtMatcher {
        boolean matches(@Nullable CompoundTag a, @Nullable CompoundTag b);
    }

    public void confirmReceipt(ItemVariant variant, IItemHandler chest) {
        // Update received quantities (keep track of total items received)
        updateReceivedQuantities(variant, chest);
        //ModernDynamics.LOGGER.info("Updated receivedQuantities for {}: {}", variant.toStack(), receivedQuantities.getOrDefault(variant, 0));

        if(receivedQuantities.getOrDefault(variant, 0) > sentQuantities.getOrDefault(variant, 0))
        {
            sentQuantities.put(variant, receivedQuantities.getOrDefault(variant, 0));
        }

        // Check if the quantity is back to zero and receivedQuantities is not zero
        if (receivedQuantities.getOrDefault(variant, 0) == sentQuantities.getOrDefault(variant, 0) && sentQuantities.getOrDefault(variant, 0) != 0) 
        {

            map_reset.put(variant, true);
            ttlMap.remove(variant);
        }

  
        if(receivedQuantities.getOrDefault(variant, 0) == 0 && map_reset.getOrDefault(variant, false))
        {
            sentQuantities.put(variant, 0); // Reset the map if the quantity is zero and receivedQuantities is not zero
            //ModernDynamics.LOGGER.info("Reset sentQuantities for {}", variant);


            receivedQuantities.put(variant, 0); // Reset the map if the quantity is zero and receivedQuantities is not zero


            //ModernDynamics.LOGGER.info("Reset receivedQuantities for {}", variant);
            map_reset.put(variant, false);
        }
    }



    private void updateReceivedQuantities(ItemVariant variant, IItemHandler chest) {
        int receivedAmount = getItemQuantityInChest(variant, chest);
        receivedQuantities.put(variant, receivedAmount);
        //ModernDynamics.LOGGER.info("Updated receivedQuantities for {}: {}", variant, receivedAmount); // Log per debug
    }

    // Add a setter for TTL duration
    public void setTtlDuration(long ttlDuration) {
        this.ttlDuration = ttlDuration;
    }

    // Add a getter for TTL duration
    public long getTtlDuration() {
        return ttlDuration;
    }
}
