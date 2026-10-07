package com.negative.negativeutils;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class CameraClientData {
    private static Active active;

    private CameraClientData() {}

    public static void show(Active camera) {
        active = camera;
    }

    public static void hide() {
        active = null;
    }

    public static Active getActive() {
        return active;
    }

    public static Vec3 resolveTarget(Active camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || camera == null) return null;

        if ("ENTITY".equals(camera.targetType())) {
            Entity entity = mc.level.getEntity(camera.entityId());
            return entity == null ? null : entity.getEyePosition();
        }
        if ("PLAYER".equals(camera.targetType())) {
            if (camera.playerUuid() != null) {
                Entity entity = mc.level.getPlayerByUUID(camera.playerUuid());
                if (entity != null) return entity.getEyePosition();
            }
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity.getName().getString().equalsIgnoreCase(camera.playerName())) {
                    return entity.getEyePosition();
                }
            }
            return null;
        }
        if (!mc.level.dimension().location().toString().equals(camera.dimension())) return null;
        return new Vec3(camera.x(), camera.y(), camera.z());
    }

    public record Active(
            UUID id, String commandId, String name, String targetType,
            String dimension, double x, double y, double z, int entityId,
            String playerName, UUID playerUuid, boolean forceLook) {
    }
}
