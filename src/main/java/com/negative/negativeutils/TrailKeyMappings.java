package com.negative.negativeutils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(
        modid = "negativeutils",
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class TrailKeyMappings {
    public static final KeyMapping TOGGLE_TRAIL = new KeyMapping(
            "key.negativeutils.toggle_trail",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "key.categories.negativeutils"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_TRAIL);
    }
}