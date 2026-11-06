package com.negative.negativeutils;

import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"negativeutils");
    public static final RegistryObject<Item> TRAIL_WAND=ITEMS.register("trail_wand",()->new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> RESPAWN_WAND=ITEMS.register("respawn_wand",RespawnWandItem::new);
    public static final RegistryObject<Item> RIFT_SPAWN_EGG = ITEMS.register(
            "rift_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.RIFT, 0x10101E, 0x62DDF4, new Item.Properties())
    );
    private ModItems(){}
}