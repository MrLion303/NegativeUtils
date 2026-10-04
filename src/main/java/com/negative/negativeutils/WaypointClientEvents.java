package com.negative.negativeutils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = NegativeUtilsMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WaypointClientEvents {
    private WaypointClientEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(ModItems.WAYPOINT_WAND.get())
                || !event.getEntity().level().isClientSide()) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Minecraft.getInstance().setScreen(
                new WaypointScreen(
                        event.getPos().immutable(),
                        event.getEntity().level().dimension().location().toString()
                )
        );
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(ModItems.WAYPOINT_WAND.get())
                || !event.getEntity().level().isClientSide()) {
            return;
        }

        event.setCanceled(true);
        deleteAimedWaypoint(event.getEntity());
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getEntity().getMainHandItem()
                        .is(ModItems.WAYPOINT_WAND.get())) {
            return;
        }

        deleteAimedWaypoint(event.getEntity());
    }

    private static void deleteAimedWaypoint(
            net.minecraft.world.entity.player.Player player
    ) {
        String dimension = player.level().dimension().location().toString();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        WaypointSavedData.Waypoint selected = null;
        double closestRayDistance = Double.MAX_VALUE;

        for (WaypointSavedData.Waypoint waypoint
                : WaypointClientData.getWaypoints()) {
            if (!waypoint.dimension().equals(dimension)) {
                continue;
            }

            Vec3 position = new Vec3(waypoint.x(), waypoint.y(), waypoint.z());
            Vec3 offset = position.subtract(eye);
            double alongRay = offset.dot(look);
            if (alongRay < 0.0 || alongRay > 512.0) {
                continue;
            }

            double rayDistanceSquared = Math.max(
                    0.0,
                    offset.lengthSqr() - alongRay * alongRay
            );
            double markerRadius = 0.8;
            if (rayDistanceSquared <= markerRadius * markerRadius
                    && rayDistanceSquared < closestRayDistance) {
                closestRayDistance = rayDistanceSquared;
                selected = waypoint;
            }
        }

        if (selected != null) {
            WaypointNetwork.delete(selected.id());
        }
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        WaypointClientData.clear();
    }
}
