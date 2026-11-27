package com.negative.negativeutils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Restaura al entrar el bloqueo de perspectiva guardado para el jugador. */
@Mod.EventBusSubscriber(
        modid = NegativeUtilsMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class F5PerspectiveLoginEvents {
    private F5PerspectiveLoginEvents() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        boolean disabled = F5PerspectiveSavedData.get(player.getServer())
                .isDisabled(player.getUUID());
        F5PerspectiveNetwork.syncToPlayer(player, disabled);
    }
}
