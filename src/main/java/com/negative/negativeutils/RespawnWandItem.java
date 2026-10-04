package com.negative.negativeutils;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RespawnWandItem extends Item {
    public RespawnWandItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
