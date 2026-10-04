package com.negative.negativeutils;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(
                    ForgeRegistries.ITEMS,
                    "negativeutils"
            );

    public static final RegistryObject<Item> TRAIL_WAND =
            ITEMS.register(
                    "trail_wand",
                    () -> new Item(new Item.Properties().stacksTo(1))
            );

    public static final RegistryObject<Item> WAYPOINT_WAND =
            ITEMS.register(
                    "waypoint_wand",
                    () -> new Item(new Item.Properties().stacksTo(1))
            );

    public static final RegistryObject<Item> RESPAWN_WAND =
            ITEMS.register(
                    "respawn_wand",
                    () -> new Item(new Item.Properties().stacksTo(1))
            );

    private ModItems() {
    }
}