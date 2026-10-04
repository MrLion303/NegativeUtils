package com.negative.negativeutils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RespawnWandEvents {
    private RespawnWandEvents() {
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(ModItems.RESPAWN_WAND.get())
                || !(event.getTarget() instanceof ServerPlayer target)
                || !(event.getEntity() instanceof ServerPlayer user)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        var server = target.getServer();
        if (server == null) {
            return;
        }

        ServerLevel destinationLevel = target.getRespawnPosition() == null
                ? null
                : server.getLevel(target.getRespawnDimension());
        Vec3 destination = null;
        if (destinationLevel != null) {
            BlockPos respawnPos = target.getRespawnPosition();
            destination = Player.findRespawnPositionAndUseSpawnBlock(
                    destinationLevel,
                    respawnPos,
                    target.getRespawnAngle(),
                    target.isRespawnForced(),
                    false
            ).orElse(null);
        }

        if (destination == null) {
            destinationLevel = server.overworld();
            BlockPos spawn = destinationLevel.getSharedSpawnPos();
            destination = Vec3.atBottomCenterOf(spawn);
        }

        ServerLevel originLevel = target.serverLevel();
        spawnBlackSmoke(originLevel, target.position());
        target.teleportTo(
                destinationLevel,
                destination.x,
                destination.y,
                destination.z,
                target.getRespawnAngle(),
                target.getXRot()
        );
        spawnBlackSmoke(destinationLevel, destination);
        target.displayClientMessage(
                Component.literal("Te han enviado a tu punto de reaparición."),
                true
        );
        user.displayClientMessage(
                Component.literal(
                        "Has enviado a " + target.getName().getString()
                                + " a su punto de reaparición."
                ),
                true
        );
    }

    private static void spawnBlackSmoke(ServerLevel level, Vec3 position) {
        level.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                position.x,
                position.y + 0.8,
                position.z,
                32,
                0.45,
                0.7,
                0.45,
                0.015
        );
    }
}
