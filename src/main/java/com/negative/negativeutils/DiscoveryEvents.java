package com.negative.negativeutils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class DiscoveryEvents {

    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ResourceLocation mobId =
                ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());

        discover(
                player,
                EncyclopediaData.Category.MOB,
                mobId
        );
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) {
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            checkStack(player, stack);
        }

        for (ItemStack stack : player.getInventory().armor) {
            checkStack(player, stack);
        }

        for (ItemStack stack : player.getInventory().offhand) {
            checkStack(player, stack);
        }
    }

    private static void checkStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ResourceLocation itemId =
                ForgeRegistries.ITEMS.getKey(stack.getItem());

        discover(
                player,
                EncyclopediaData.Category.ITEM,
                itemId
        );

        Block block = Block.byItem(stack.getItem());

        if (block != Blocks.AIR) {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);

            discover(
                    player,
                    EncyclopediaData.Category.BLOCK,
                    blockId
            );
        }
    }

    private static void discover(
            ServerPlayer player,
            EncyclopediaData.Category category,
            ResourceLocation targetId
    ) {
        if (targetId == null) {
            return;
        }

        EncyclopediaData catalog = EncyclopediaData.get(player.server);
        PlayerDiscoveryData discoveries =
                PlayerDiscoveryData.get(player.server);

        String id = targetId.toString();

        for (EncyclopediaData.Entry entry : catalog.getEntries()) {
            if (entry.category() == category
                    && entry.targetId().equals(id)
                    && discoveries.discover(player.getUUID(), entry)) {
                EncyclopediaNetwork.syncDiscovery(
                        player,
                        entry.notificationText()
                );
            }
        }
    }
}