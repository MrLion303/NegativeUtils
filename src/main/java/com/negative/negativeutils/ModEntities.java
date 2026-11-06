package com.negative.negativeutils;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = NegativeUtilsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, NegativeUtilsMod.MOD_ID);

    public static final RegistryObject<EntityType<RiftEntity>> RIFT = ENTITY_TYPES.register(
            "rift",
            () -> EntityType.Builder.<RiftEntity>of(RiftEntity::new, MobCategory.MISC)
                    .sized(1.35F, 1.95F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("rift")
    );

    private ModEntities() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RIFT.get(), RiftEntity.createAttributes().build());
    }
}
