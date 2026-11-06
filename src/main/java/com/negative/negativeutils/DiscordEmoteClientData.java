package com.negative.negativeutils;

import com.mojang.logging.LogUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

public final class DiscordEmoteClientData {
    public static final ResourceLocation EMOTE_FONT =
            ResourceLocation.fromNamespaceAndPath(
                    NegativeUtilsMod.MOD_ID,
                    "discord_emotes"
            );
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PACK_ID = "negativeutils_discord_emotes";
    private static final Path PACK_DIRECTORY =
            FMLPaths.CONFIGDIR.get().resolve("negativeutils-discord-emotes");

    private static final AtomicBoolean RELOAD_IN_PROGRESS = new AtomicBoolean();
    private static final AtomicBoolean RELOAD_QUEUED = new AtomicBoolean();
    private static volatile Map<String, Integer> emotes = Map.of();

    private DiscordEmoteClientData() {
    }

    public static Map<String, Integer> getEmotes() {
        return emotes;
    }

    /**
     * Creates the generated pack before Minecraft performs its initial resource load.
     * The empty atlas is replaced as soon as the server sends the current Discord emotes.
     */
    public static void initializeResourcePack() throws IOException {
        Path fontDirectory = PACK_DIRECTORY.resolve("assets/negativeutils/font");
        Path textureDirectory = PACK_DIRECTORY.resolve("assets/negativeutils/textures/font");
        Files.createDirectories(fontDirectory);
        Files.createDirectories(textureDirectory);

        Path fontPath = fontDirectory.resolve("discord_emotes.json");
        try (var fontDefinition = DiscordEmoteClientData.class.getResourceAsStream(
                "/assets/negativeutils/font/discord_emotes.json")) {
            if (fontDefinition == null) {
                throw new IOException("Bundled Discord emote font definition is missing.");
            }
            Files.write(fontPath, fontDefinition.readAllBytes());
        }

        Path atlasPath = textureDirectory.resolve("discord_emotes.png");
        if (!Files.exists(atlasPath)) {
            BufferedImage emptyAtlas = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            try {
                if (!ImageIO.write(emptyAtlas, "png", atlasPath.toFile())) {
                    throw new IOException("Could not create the initial Discord emote atlas.");
                }
            } finally {
                emptyAtlas.flush();
            }
        }

        Path mappingPath = PACK_DIRECTORY.resolve("emotes-map.json");
        if (!Files.exists(mappingPath)) {
            Files.writeString(mappingPath, "{}");
        }
        Files.writeString(
                PACK_DIRECTORY.resolve("pack.mcmeta"),
                "{\"pack\":{\"pack_format\":15,\"description\":\"Discord bot emotes synchronized by NegativeUtils\"}}"
        );
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
            String serializedMapping = new com.google.gson.Gson().toJson(mapping);
            if (!Files.exists(mappingPath)
                    || !Files.readString(mappingPath).equals(serializedMapping)) {
                Files.writeString(mappingPath, serializedMapping);
                changed = true;
            }
            try (var fontDefinition = DiscordEmoteClientData.class.getResourceAsStream(
                    "/assets/negativeutils/font/discord_emotes.json")) {
                if (fontDefinition == null) {
                    throw new IOException("Bundled Discord emote font definition is missing.");
                }
                Files.write(
                        PACK_DIRECTORY.resolve("assets/negativeutils/font/discord_emotes.json"),
                        fontDefinition.readAllBytes()
                );
            }
            Files.writeString(
                    PACK_DIRECTORY.resolve("pack.mcmeta"),
                    "{\"pack\":{\"pack_format\":15,\"description\":\"Discord bot emotes synchronized by NegativeUtils\"}}"
            );
            if (changed) {
                requestResourceReload();
            }
        } catch (IOException exception) {
            LOGGER.error("Could not write the synchronized Discord emote atlas.", exception);
        }
    }

    private static void requestResourceReload() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (!RELOAD_IN_PROGRESS.compareAndSet(false, true)) {
                RELOAD_QUEUED.set(true);
                return;
            }

            minecraft.reloadResourcePacks().whenComplete((ignored, error) ->
                    minecraft.execute(() -> {
                        RELOAD_IN_PROGRESS.set(false);
                        if (error != null) {
                            LOGGER.error("Could not reload the Discord emote atlas.", error);
                        }
                        if (RELOAD_QUEUED.getAndSet(false)) {
                            requestResourceReload();
                        }
                    })
            );
        });
    }

    public static Path packDirectory() {
        return PACK_DIRECTORY;
    }

    public static String packId() {
        return PACK_ID;
    }
}
