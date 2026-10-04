package com.negative.negativeutils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = EnciclopediaMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class ModKeyMappings {
    public static final KeyMapping OPEN_ENCYCLOPEDIA =
            new KeyMapping(
                    "key.negativeutils.open_encyclopedia",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    "key.categories.negativeutils"
            );

    public static final KeyMapping OPEN_GUILDS =
            new KeyMapping(
                    "key.negativeutils.open_guilds",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    "key.categories.negativeutils"
            );

    @SubscribeEvent
    public static void registerKeyMappings(
            RegisterKeyMappingsEvent event
    ) {
        event.register(OPEN_ENCYCLOPEDIA);
        event.register(OPEN_GUILDS);
    }
}