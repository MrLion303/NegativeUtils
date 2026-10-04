package com.negative.negativeutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = NegativeUtilsMod.MOD_ID,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class DiscordEmoteClientEvents {
    private DiscordEmoteClientEvents() {
    }

    @SubscribeEvent
    public static void addEmoteResourcePack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }

        Path packDirectory = DiscordEmoteClientData.packDirectory();
        try {
            Files.createDirectories(packDirectory);
            Files.writeString(
                    packDirectory.resolve("pack.mcmeta"),
                    "{\"pack\":{\"pack_format\":15,\"description\":\"Discord bot emotes synchronized by NegativeUtils\"}}"
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not initialize the Discord emote resource pack.",
                    exception
            );
        }

        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    DiscordEmoteClientData.packId(),
                    Component.literal("NegativeUtils Discord Emotes"),
                    false,
                    packId -> new PathPackResources(
                            DiscordEmoteClientData.packId(),
                            packDirectory,
                            false
                    ),
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }
}
