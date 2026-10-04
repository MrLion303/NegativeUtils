package com.negative.negativeutils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NegativeUtilsMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> NEGATIVEUTILS =
            CREATIVE_MODE_TABS.register(
                    "negativeutils",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.negativeutils"))
                            .icon(() -> ModItems.WAYPOINT_WAND.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.TRAIL_WAND.get());
                                output.accept(ModItems.WAYPOINT_WAND.get());
                                output.accept(ModItems.RESPAWN_WAND.get());
                                output.accept(ModBlocks.TIME_DELAY_BLOCK_ITEM.get());
                                output.accept(ModBlocks.COMMAND_SEQUENCE_BLOCK_ITEM.get());
                            })
                            .build()
            );

    private ModCreativeModeTabs() {
    }
}
