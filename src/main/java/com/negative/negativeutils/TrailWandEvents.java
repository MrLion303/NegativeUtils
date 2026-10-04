package com.negative.negativeutils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "negativeutils", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrailWandEvents {
    private TrailWandEvents() {
    }

    private static void message(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GOLD));
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getItemStack().is(ModItems.TRAIL_WAND.get())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.hasPermissions(2)) {
            message(player, "Necesitas permisos de operador para editar senderos.");
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        TrailSavedData data = TrailSavedData.get(level.getServer());
        java.util.UUID selectedId = TrailSelectionState.get(player.getUUID());
        TrailSavedData.Trail trail = selectedId == null ? null : data.getById(selectedId);

        if (trail == null) {
            message(player, "Primero crea un sendero con /negativeutils senderos crear \"Nombre\".");
            return;
        }

        data.resetPoints(trail.id(), level,
                event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
        TrailNetwork.syncAll(data.getTrails());
        message(player, "Inicio del sendero '" + trail.name()
                + "' marcado. Clic derecho en los bloques siguientes para añadir huellas.");
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(ModItems.TRAIL_WAND.get())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.hasPermissions(2)) {
            message(player, "Necesitas permisos de operador para editar senderos.");
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        TrailSavedData data = TrailSavedData.get(level.getServer());
        java.util.UUID selectedId = TrailSelectionState.get(player.getUUID());
        TrailSavedData.Trail trail = selectedId == null ? null : data.getById(selectedId);

        if (trail == null) {
            message(player, "Primero crea y selecciona un sendero con /negativeutils senderos crear \"Nombre\".");
            return;
        }

        data.addPoint(trail.id(), level,
                event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
        TrailNetwork.syncAll(data.getTrails());
        message(player, "Huella añadida a '" + trail.name()
                + "' (" + trail.points().size() + " puntos).");
    }
}