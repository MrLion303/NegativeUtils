package com.negative.negativeutils.client;

import com.negative.negativeutils.ModEntities;
import com.negative.negativeutils.NegativeUtilsMod;
import com.negative.negativeutils.client.model.RiftModel;
import com.negative.negativeutils.client.renderer.RiftRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = NegativeUtilsMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class RiftClientEvents {
    private RiftClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RiftModel.LAYER_LOCATION, RiftModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RIFT.get(), RiftRenderer::new);
    }
}
