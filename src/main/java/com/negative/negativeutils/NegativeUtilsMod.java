package com.negative.negativeutils;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NegativeUtilsMod.MOD_ID)
public final class NegativeUtilsMod {
    public static final String MOD_ID = "negativeutils";

    public NegativeUtilsMod(FMLJavaModLoadingContext context) {
        var modEventBus = context.getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlocks.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        TrailNetwork.register();
        VoicechatMuteStateNetwork.register();
        TimeDelayNetwork.register();
        WaypointNetwork.register();
        CameraNetwork.register();
        CommandSequenceNetwork.register();
        CommunicatorNetwork.register();
        DiscordEmoteNetwork.register();
    }
}