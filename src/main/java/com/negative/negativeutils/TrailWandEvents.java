package com.negative.negativeutils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = "negativeutils",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class TrailWandEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();

        if (!player.getMainHandItem().is(ModItems.TRAIL_WAND.get())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!player.hasPermissions(2) || event.getLevel().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        TrailSavedData data = TrailSavedData.get(level.getServer());
        var pos = event.getPos();

        data.addPoint(level, pos.getX(), pos.getY(), pos.getZ());

        TrailNetwork.sendToAll(data.getPoints());

        player.displayClientMessage(
                Component.literal("Punto añadido al camino (" + data.getPoints().size() + ")"),
                true
        );
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        var player = event.getEntity();

        if (!player.getMainHandItem().is(ModItems.TRAIL_WAND.get())) {
            return;
        }

        event.setCanceled(true);

        if (!player.hasPermissions(2) || event.getLevel().isClientSide()) {
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        TrailSavedData data = TrailSavedData.get(level.getServer());
        var pos = event.getPos();

        boolean removed = data.removeNearestPoint(
                level,
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );

        if (removed) {
            TrailNetwork.sendToAll(data.getPoints());
        }

        player.displayClientMessage(
                Component.literal(
                        removed
                                ? "Punto eliminado del camino"
                                : "No hay un punto del camino cerca"
                ),
                true
        );
    }
}