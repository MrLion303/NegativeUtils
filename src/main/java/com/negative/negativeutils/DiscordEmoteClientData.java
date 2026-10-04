package com.negative.negativeutils;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

public final class DiscordEmoteClientData {
    public static final ResourceLocation EMOTE_FONT =
            ResourceLocation.fromNamespaceAndPath(
                    EnciclopediaMod.MOD_ID,
                    "discord_emotes"
            );
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PACK_ID = "negativeutils_discord_emotes";
    private static final Path PACK_DIRECTORY =
            FMLPaths.CONFIGDIR.get().resolve("negativeutils-discord-emotes");

    private static volatile Map<String, Integer> emotes = Map.of();
    private static final AtomicBoolean reloadPending = new AtomicBoolean();

    private DiscordEmoteClientData() {
    }

    public static Map<String, Integer> getEmotes() {
        return emotes;
    }

    public static void accept(Map<String, Integer> mapping, byte[] atlas) {
        emotes = Map.copyOf(mapping);
        try {
            Files.createDirectories(
                    PACK_DIRECTORY.resolve("assets/negativeutils/textures/font")
            );
            Files.createDirectories(
                    PACK_DIRECTORY.resolve("assets/negativeutils/font")
            );
            Path atlasPath = PACK_DIRECTORY.resolve(
                    "assets/negativeutils/textures/font/discord_emotes.png"
            );
            boolean changed = !Files.exists(atlasPath)
                    || !java.util.Arrays.equals(
                            Files.readAllBytes(atlasPath),
                            atlas
                    );
            if (changed) {
                Files.write(atlasPath, atlas);
            }
            Path mappingPath = PACK_DIRECTORY.resolve("emotes-map.json");
            String serializedMapping = new com.google.gson.Gson()
                    .toJson(mapping);
            if (!Files.exists(mappingPath)
                    || !Files.readString(mappingPath).equals(serializedMapping)) {
                Files.writeString(mappingPath, serializedMapping);
                changed = true;
            }
            try (var fontDefinition = DiscordEmoteClientData.class
                    .getResourceAsStream(
                            "/assets/negativeutils/font/discord_emotes.json"
                    )) {
                if (fontDefinition == null) {
                    throw new IOException(
                            "Bundled Discord emote font definition is missing."
                    );
                }
                Files.write(
                        PACK_DIRECTORY.resolve(
                                "assets/negativeutils/font/discord_emotes.json"
                        ),
                        fontDefinition.readAllBytes()
                );
            }
            Files.writeString(
                    PACK_DIRECTORY.resolve("pack.mcmeta"),
                    "{\"pack\":{\"pack_format\":15,\"description\":\"Discord bot emotes synchronized by NegativeUtils\"}}"
            );
            reloadPending.set(reloadPending.get() || changed);
        } catch (IOException exception) {
            LOGGER.error("Could not write the synchronized Discord emote atlas.", exception);
        }
    }

    public static void ensureEmoteFontLoaded(Runnable afterReload) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            var repository = minecraft.getResourcePackRepository();
            boolean selected = repository.getSelectedIds().contains(PACK_ID);
            if (selected && !reloadPending.getAndSet(false)) {
                afterReload.run();
                return;
            }

            repository.reload();
            java.util.ArrayList<String> packs =
                    new java.util.ArrayList<>(repository.getSelectedIds());
            if (!packs.contains(PACK_ID)) {
                packs.add(PACK_ID);
            }
            repository.setSelected(packs);
            CompletableFuture<Void> reload = minecraft.reloadResourcePacks();
            reload.whenComplete((ignored, error) -> minecraft.execute(() -> {
                if (error != null) {
                    reloadPending.set(true);
                    LOGGER.error("Could not reload the Discord emote atlas.", error);
                    return;
                }
                reloadPending.set(false);
                afterReload.run();
            }));
        });
    }

    public static Path packDirectory() {
        return PACK_DIRECTORY;
    }

    public static String packId() {
        return PACK_ID;
    }
}
