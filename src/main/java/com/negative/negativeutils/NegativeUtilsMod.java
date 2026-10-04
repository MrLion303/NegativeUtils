package com.negative.negativeutils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NegativeUtilsMod.MOD_ID)
public final class NegativeUtilsMod {
    public static final String MOD_ID = "negativeutils";

    public NegativeUtilsMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlocks.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        TrailNetwork.register();
        TimeDelayNetwork.register();
        WaypointNetwork.register();
        CommandSequenceNetwork.register();
        DiscordEmoteNetwork.register();
    }
}
