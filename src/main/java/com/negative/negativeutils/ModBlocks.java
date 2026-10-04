package com.negative.negativeutils;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "negativeutils");

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "negativeutils");

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "negativeutils");

    public static final RegistryObject<Block> TIME_DELAY_BLOCK =
            BLOCKS.register(
                    "time_delay",
                    () -> new TimeDelayBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(2.0F)
                                    .sound(SoundType.METAL)
                                    .isRedstoneConductor(
                                            (state, level, pos) -> false
                                    )
                    )
            );

    public static final RegistryObject<Block> COMMAND_SEQUENCE_BLOCK =
            BLOCKS.register(
                    "command_sequence",
                    () -> new CommandSequenceBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(3.5F)
                                    .sound(SoundType.METAL)
                                    .isRedstoneConductor(
                                            (state, level, pos) -> false
                                    )
                    )
            );

    public static final RegistryObject<Item> TIME_DELAY_BLOCK_ITEM =
            ITEMS.register(
                    "time_delay",
                    () -> new BlockItem(
                            TIME_DELAY_BLOCK.get(),
                            new Item.Properties()
                    )
            );

    public static final RegistryObject<Item> COMMAND_SEQUENCE_BLOCK_ITEM =
            ITEMS.register(
                    "command_sequence",
                    () -> new BlockItem(
                            COMMAND_SEQUENCE_BLOCK.get(),
                            new Item.Properties()
                    )
            );

    public static final RegistryObject<BlockEntityType<TimeDelayBlockEntity>>
            TIME_DELAY_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "time_delay",
                    () -> BlockEntityType.Builder.of(
                            TimeDelayBlockEntity::new,
                            TIME_DELAY_BLOCK.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<CommandSequenceBlockEntity>>
            COMMAND_SEQUENCE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "command_sequence",
                    () -> BlockEntityType.Builder.of(
                            CommandSequenceBlockEntity::new,
                            COMMAND_SEQUENCE_BLOCK.get()
                    ).build(null)
            );

    private ModBlocks() {
    }
}