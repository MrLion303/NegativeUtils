package com.negative.negativeutils;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RiftEntity extends PathfinderMob {
    public static final double HEIGHT_TO_EXIT = 100.0D;
    public static final double MODEL_CENTER_OFFSET = 1.11D;

    private static final EntityDataAccessor<Boolean> EXIT_ONLY =
            SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID exitRiftUuid;

    public RiftEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        setNoAi(true);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // Rift no tiene IA ni objetivos.
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(EXIT_ONLY, false);
    }

    public boolean isExitOnly() {
        return entityData.get(EXIT_ONLY);
    }

    public void setExitOnly(boolean exitOnly) {
        entityData.set(EXIT_ONLY, exitOnly);
        setNoAi(true);
        setNoGravity(true);
        setPersistenceRequired();
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);

        if (level().isClientSide || isExitOnly() || tickCount < 2) {
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            checkPlayerContacts(serverLevel);
        }
    }

    private void checkPlayerContacts(ServerLevel serverLevel) {
        AABB hitbox = getRiftHitbox().inflate(0.025D);
        ArrayList<ServerPlayer> players = new ArrayList<>(
                serverLevel.getEntitiesOfClass(
                        ServerPlayer.class,
                        hitbox,
                        player -> !player.isSpectator() && !player.isRemoved()
                )
        );

        for (ServerPlayer player : players) {
            if (!player.getBoundingBox().intersects(hitbox)) {
                continue;
            }

            RiftEntity exitRift = getOrCreateExitRift(serverLevel);
            if (exitRift == null) {
                continue;
            }

            player.stopRiding();
            boolean teleported = player.teleportTo(
                    serverLevel,
                    exitRift.getX(),
                    exitRift.getY() + MODEL_CENTER_OFFSET,
                    exitRift.getZ(),
                    Set.of(),
                    player.getYRot(),
                    player.getXRot()
            );

            if (teleported) {
                // Caída lenta le permite descender los 100 bloques sin recibir daño por caída.
                player.addEffect(new MobEffectInstance(
                        MobEffects.SLOW_FALLING,
                        20 * 30,
                        0,
                        false,
                        true,
                        true
                ));
                player.setDeltaMovement(0.0D, -0.08D, 0.0D);
            }
        }
    }

    private RiftEntity getOrCreateExitRift(ServerLevel serverLevel) {
        if (exitRiftUuid != null) {
            Entity linked = serverLevel.getEntity(exitRiftUuid);
            if (linked instanceof RiftEntity existing
                    && !existing.isRemoved()
                    && existing.isExitOnly()) {
                return existing;
            }
        }

        RiftEntity exit = ModEntities.RIFT.get().create(serverLevel);
        if (exit == null) {
            return null;
        }

        exit.setExitOnly(true);
        exit.setPos(getX(), getY() + HEIGHT_TO_EXIT, getZ());
        exit.setYRot(getYRot());
        exit.setYBodyRot(getYRot());
        exit.setYHeadRot(getYRot());
        exit.setXRot(0.0F);

        if (!serverLevel.addFreshEntity(exit)) {
            return null;
        }

        exitRiftUuid = exit.getUUID();
        return exit;
    }

    private AABB getRiftHitbox() {
        // Caja delgada delante del plano del modelo; rota con la orientación del Rift.
        double yaw = Math.toRadians(getYRot());
        double cos = Math.abs(Math.cos(yaw));
        double sin = Math.abs(Math.sin(yaw));
        double halfWidth = 0.78D;
        double halfDepth = 0.20D;
        double halfX = cos * halfWidth + sin * halfDepth;
        double halfZ = sin * halfWidth + cos * halfDepth;

        return new AABB(
                getX() - halfX, getY() + 0.25D, getZ() - halfZ,
                getX() + halfX, getY() + 1.98D, getZ() + halfZ
        );
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("ExitOnly", isExitOnly());
        if (exitRiftUuid != null) {
            tag.putUUID("ExitRift", exitRiftUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setExitOnly(tag.getBoolean("ExitOnly"));
        if (tag.hasUUID("ExitRift")) {
            exitRiftUuid = tag.getUUID("ExitRift");
        }
    }
}
