package com.negative.negativeutils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class NegativeUtilsTarget {
    private static final double REACH = 128.0D;

    private NegativeUtilsTarget() {}

    public static Vec3 getLookedBlockPosition(ServerPlayer player) {
        HitResult hit = player.pick(REACH, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            return new Vec3(pos.getX(), pos.getY(), pos.getZ());
        }
        return new Vec3(Math.floor(player.getX()), Math.floor(player.getY()), Math.floor(player.getZ()));
    }
}
