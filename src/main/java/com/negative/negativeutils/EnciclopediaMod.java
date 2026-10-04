package com.negative.negativeutils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EnciclopediaMod.MOD_ID)
public class EnciclopediaMod {
    public static final String MOD_ID = "negativeutils";

    public EnciclopediaMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlocks.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        TrailNetwork.register();
        EncyclopediaNetwork.register();
        TimeDelayNetwork.register();
        GuildNetwork.register();
        GuildActionsNetwork.register();
        FeatureToggleNetwork.register();
        WaypointNetwork.register();
        CommandSequenceNetwork.register();
        DiscordEmoteNetwork.register();
    }
}